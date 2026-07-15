import {
  getPatientMedicationAdvice,
  pageAlerts,
  pageDoctorConsultSessions,
  pageMedical,
  pageMetric,
  pageReport,
} from '@/api'

const METRIC_LABELS = {
  BLOOD_PRESSURE: '血压',
  BLOOD_SUGAR: '血糖',
  HEART_RATE: '心率',
  TEMPERATURE: '体温',
  WEIGHT: '体重',
  OXYGEN: '血氧',
}

const METRIC_UNITS = {
  BLOOD_PRESSURE: 'mmHg',
  BLOOD_SUGAR: 'mmol/L',
  HEART_RATE: '次/分',
  TEMPERATURE: '℃',
  WEIGHT: 'kg',
  OXYGEN: '%',
}

const TYPE_META = {
  metric: { label: '体征记录', tone: 'blue' },
  alert: { label: '健康预警', tone: 'red' },
  report: { label: '健康报告', tone: 'cyan' },
  medical: { label: '就诊记录', tone: 'green' },
  consult: { label: '医生咨询', tone: 'violet' },
  medicine: { label: '用药建议', tone: 'orange' },
}

export function emptyHealthSituation() {
  return {
    score: 100,
    level: '状态良好',
    tone: 'excellent',
    summary: '暂无足够数据，建议先记录体征。',
    deductions: [],
    metrics: [],
    alerts: [],
    reports: [],
    medicals: [],
    sessions: [],
    medicationAdvices: [],
    events: [],
    rings: {
      metric: 0,
      alert: 0,
      consult: 0,
      medicine: 0,
    },
  }
}

export async function loadHealthSituation(options = {}) {
  const medicationLimit = options.medicationLimit ?? 8
  const [metricRes, alertRes, reportRes, medicalRes, sessionRes] = await Promise.allSettled([
    pageMetric({ pageNum: 1, pageSize: options.metricSize ?? 80 }),
    pageAlerts({ pageNum: 1, pageSize: options.alertSize ?? 50 }),
    pageReport({ pageNum: 1, pageSize: options.reportSize ?? 30 }),
    pageMedical({ pageNum: 1, pageSize: options.medicalSize ?? 30 }),
    pageDoctorConsultSessions({ pageNum: 1, pageSize: options.sessionSize ?? 30 }),
  ])

  const metrics = recordsFrom(metricRes)
  const alerts = recordsFrom(alertRes)
  const reports = recordsFrom(reportRes)
  const medicals = recordsFrom(medicalRes)
  const sessions = recordsFrom(sessionRes)
  const medicationAdvices = await loadMedicationAdvices(sessions, medicationLimit)

  return buildHealthSituation({ metrics, alerts, reports, medicals, sessions, medicationAdvices })
}

export function buildHealthSituation(data) {
  const metrics = sortByTime(data.metrics || [])
  const alerts = sortByTime(data.alerts || [])
  const reports = sortByTime(data.reports || [])
  const medicals = sortByTime(data.medicals || [])
  const sessions = sortByTime(data.sessions || [], item => sessionTime(item))
  const medicationAdvices = sortByTime(data.medicationAdvices || [])
  const deductions = []

  scoreAlerts(alerts, deductions)
  scoreMetrics(metrics, deductions)
  scoreConsults(sessions, deductions)
  scoreMedication(medicationAdvices, deductions)

  const totalDeduction = deductions.reduce((sum, item) => sum + item.points, 0)
  const score = clamp(100 - totalDeduction, 40, 100)
  const level = levelOf(score)
  const events = buildTimeline({ metrics, alerts, reports, medicals, sessions, medicationAdvices })

  return {
    score,
    level: level.label,
    tone: level.tone,
    summary: summaryOf(score, deductions),
    deductions,
    metrics,
    alerts,
    reports,
    medicals,
    sessions,
    medicationAdvices,
    events,
    rings: {
      metric: Math.min(metrics.filter(m => isMetricAbnormal(m)).length / 5, 1),
      alert: Math.min(alerts.filter(a => Number(a.readFlag) !== 1).length / 5, 1),
      consult: Math.min(sessions.filter(item => consultIssue(item)).length / 4, 1),
      medicine: Math.min(medicationAdvices.filter(a => a.status === 'PENDING_CONFIRM').length / 3, 1),
    },
  }
}

export function metricName(type) {
  return METRIC_LABELS[type] || type || '体征'
}

export function metricValue(row) {
  if (!row) return '-'
  const unit = row.unit || METRIC_UNITS[row.metricType] || ''
  if (row.metricType === 'BLOOD_PRESSURE' || row.metricValue2 != null) {
    return `${formatNumber(row.metricValue)}/${formatNumber(row.metricValue2)} ${unit}`.trim()
  }
  return `${formatNumber(row.metricValue)} ${unit}`.trim()
}

export function formatTimelineTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function typeMeta(type) {
  return TYPE_META[type] || { label: '健康事件', tone: 'blue' }
}

function recordsFrom(result) {
  if (result.status !== 'fulfilled') return []
  const data = result.value?.data
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  return []
}

async function loadMedicationAdvices(sessions, limit) {
  if (!limit || limit <= 0) return []

  const closed = sessions
    .filter(item => (item.session || item).status === 'CLOSED')
    .slice(0, limit)

  const settled = await Promise.allSettled(closed.map(item => {
    const session = item.session || item
    return getPatientMedicationAdvice(session.id)
  }))

  return settled
    .filter(item => item.status === 'fulfilled' && item.value?.data)
    .map(item => item.value.data)
}

function scoreAlerts(alerts, deductions) {
  const unread = alerts.filter(item => Number(item.readFlag) !== 1)
  const high = unread.filter(item => item.level === 'HIGH').length
  const medium = unread.filter(item => item.level === 'MEDIUM').length
  const low = unread.length - high - medium
  const points = Math.min(high * 12 + medium * 8 + low * 5, 30)
  if (points) {
    deductions.push({
      key: 'alert',
      label: '未处理健康预警',
      points,
      desc: `${unread.length} 条预警未读，其中高危 ${high} 条`,
    })
  }
}

function scoreMetrics(metrics, deductions) {
  if (!metrics.length) {
    deductions.push({ key: 'metric-empty', label: '缺少体征记录', points: 10, desc: '还没有可用于判断的体征数据' })
    return
  }

  const latest = latestMetricByType(metrics)
  const abnormal = Object.values(latest).filter(item => isMetricAbnormal(item))
  const abnormalPoints = Math.min(abnormal.reduce((sum, item) => sum + metricPenalty(item), 0), 25)
  if (abnormalPoints) {
    deductions.push({
      key: 'metric-abnormal',
      label: '最近体征存在异常',
      points: abnormalPoints,
      desc: abnormal.map(item => `${metricName(item.metricType)} ${metricValue(item)}`).join('，'),
    })
  }

  const lastTime = new Date(timeOf(metrics[0]))
  const days = Number.isNaN(lastTime.getTime()) ? 999 : Math.floor((Date.now() - lastTime.getTime()) / 86400000)
  if (days >= 7) {
    deductions.push({ key: 'metric-stale', label: '体征记录不连续', points: 10, desc: '近 7 天没有新的体征记录' })
  } else if (days >= 1) {
    deductions.push({ key: 'metric-today', label: '今日未记录体征', points: 4, desc: '建议每天至少记录一次关键指标' })
  }
}

function scoreConsults(sessions, deductions) {
  const pendingReport = sessions.filter(item => {
    const session = item.session || item
    return session.status === 'CLOSED' && !hasDoctorReport(session)
  }).length
  const pendingFeedback = sessions.filter(item => {
    const session = item.session || item
    return session.status === 'CLOSED' && hasDoctorReport(session) && !session.rating
  }).length
  const points = Math.min(pendingReport * 8 + pendingFeedback * 4, 12)
  if (points) {
    deductions.push({
      key: 'consult',
      label: '咨询闭环未完成',
      points,
      desc: `${pendingReport} 次待医生报告，${pendingFeedback} 次待患者评价`,
    })
  }
}

function scoreMedication(advices, deductions) {
  const pending = advices.filter(item => item.status === 'PENDING_CONFIRM').length
  const points = Math.min(pending * 8, 8)
  if (points) {
    deductions.push({
      key: 'medicine',
      label: '用药建议待确认',
      points,
      desc: `${pending} 份医生用药建议还没有确认`,
    })
  }
}

function buildTimeline(data) {
  const events = []
  data.metrics.forEach(row => events.push({
    id: `metric-${row.id}`,
    type: 'metric',
    time: timeOf(row),
    title: `${metricName(row.metricType)}记录`,
    desc: metricValue(row),
    status: isMetricAbnormal(row) ? '异常' : '正常',
    actionPath: '/health/metric',
  }))
  data.alerts.forEach(row => events.push({
    id: `alert-${row.id}`,
    type: 'alert',
    time: timeOf(row),
    title: row.alertType || '健康预警',
    desc: row.content || '系统生成了一条健康预警',
    status: Number(row.readFlag) === 1 ? '已读' : '未处理',
    actionPath: '/alert',
  }))
  data.reports.forEach(row => events.push({
    id: `report-${row.id}`,
    type: 'report',
    time: row.reportDate || timeOf(row),
    title: row.title || '健康报告',
    desc: row.summary || row.content || '查看周期性健康分析报告',
    status: reportTypeName(row.reportType),
    actionPath: '/health/report',
  }))
  data.medicals.forEach(row => events.push({
    id: `medical-${row.id}`,
    type: 'medical',
    time: row.visitDate || timeOf(row),
    title: row.hospital || '就诊记录',
    desc: [row.department, row.doctorName, row.diagnosis].filter(Boolean).join(' · ') || '记录了一次就诊',
    status: row.prescription ? '含用药' : '已归档',
    actionPath: '/health/medical',
  }))
  data.sessions.forEach(item => {
    const session = item.session || item
    const doctor = item.doctor || {}
    events.push({
      id: `consult-${session.id}`,
      type: 'consult',
      time: session.lastMessageTime || timeOf(session),
      title: `咨询 ${doctor.name || '医生'}`,
      desc: session.problemOverview || session.summary || session.advice || session.lastMessage || '医生咨询会话',
      status: session.status === 'CLOSED' ? (hasDoctorReport(session) ? '已出报告' : '待报告') : '进行中',
      actionPath: `/doctor-consult?sessionId=${session.id}`,
    })
  })
  data.medicationAdvices.forEach(row => events.push({
    id: `medicine-${row.id}`,
    type: 'medicine',
    time: row.patientConfirmTime || timeOf(row),
    title: '医生用药建议',
    desc: row.doctorNote || `${row.items?.length || 0} 个药品建议`,
    status: row.status === 'CONFIRMED' ? '已确认' : '待确认',
    actionPath: row.sessionId ? `/doctor-consult?sessionId=${row.sessionId}` : '/doctor-consult',
  }))

  return sortByTime(events).slice(0, 80)
}

function latestMetricByType(metrics) {
  return metrics.reduce((map, row) => {
    if (!map[row.metricType]) map[row.metricType] = row
    return map
  }, {})
}

function isMetricAbnormal(row) {
  if (Number(row.abnormal) === 1) return true
  const v1 = Number(row.metricValue)
  const v2 = Number(row.metricValue2)
  if (Number.isNaN(v1)) return false
  if (row.metricType === 'BLOOD_PRESSURE') return v1 < 90 || v1 > 140 || v2 < 60 || v2 > 90
  if (row.metricType === 'BLOOD_SUGAR') return v1 < 3.9 || v1 > 6.1
  if (row.metricType === 'HEART_RATE') return v1 < 60 || v1 > 100
  if (row.metricType === 'TEMPERATURE') return v1 < 35.5 || v1 > 37.3
  if (row.metricType === 'OXYGEN') return v1 < 95
  return false
}

function metricPenalty(row) {
  if (row.metricType === 'BLOOD_PRESSURE') return 10
  if (row.metricType === 'BLOOD_SUGAR') return 8
  if (row.metricType === 'HEART_RATE') return 6
  if (row.metricType === 'TEMPERATURE') return 5
  if (row.metricType === 'OXYGEN') return 8
  return 4
}

function consultIssue(item) {
  const session = item.session || item
  return session.status === 'CLOSED' && (!hasDoctorReport(session) || !session.rating)
}

function hasDoctorReport(session) {
  return Boolean(session.problemOverview || session.preliminaryAssessment || session.summary || session.advice || session.riskWarning)
}

function levelOf(score) {
  if (score >= 90) return { label: '状态良好', tone: 'excellent' }
  if (score >= 75) return { label: '总体平稳', tone: 'stable' }
  if (score >= 60) return { label: '需要关注', tone: 'watch' }
  return { label: '建议及时处理', tone: 'risk' }
}

function summaryOf(score, deductions) {
  if (!deductions.length) return '近期健康闭环完整，体征与服务状态都比较平稳。'
  const top = deductions[0]
  if (score >= 75) return `整体状态可控，主要需要处理：${top.label}。`
  return `当前存在需要优先处理的事项：${top.label}。`
}

function reportTypeName(type) {
  return ({ PHYSICAL: '体检报告', AI: '自动生成', OTHER: '其他' })[type] || type || '报告'
}

function sortByTime(rows, getter = timeOf) {
  return [...rows].sort((a, b) => {
    const aTime = toTimestamp(getter(a))
    const bTime = toTimestamp(getter(b))
    if (aTime !== bTime) return bTime - aTime
    return String(getter(b) || '').localeCompare(String(getter(a) || ''))
  })
}

function sessionTime(item) {
  const session = item.session || item
  return session.lastMessageTime || session.updateTime || session.createTime
}

function timeOf(row) {
  return row.time || row.measureTime || row.createTime || row.updateTime || row.reportDate || row.visitDate || row.patientConfirmTime
}

function toTimestamp(value) {
  if (!value) return 0
  if (value instanceof Date) return value.getTime()
  const normalized = String(value).trim().replace(' ', 'T')
  const time = new Date(normalized).getTime()
  return Number.isNaN(time) ? 0 : time
}

function formatNumber(value) {
  if (value == null || value === '') return '-'
  const num = Number(value)
  return Number.isNaN(num) ? value : Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}
