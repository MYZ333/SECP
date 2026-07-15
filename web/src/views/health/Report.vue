<template>
  <section class="report-page">
    <header class="page-toolbar">
      <div>
        <h2>健康报告</h2>
        <p>汇总体征趋势与异常记录，报告仅用于个人健康管理。</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="openDialog()">新增健康报告</el-button>
        <el-button type="primary" :loading="generating" @click="generateDialog = true">生成分析报告</el-button>
      </div>
    </header>

    <el-card class="list-panel" shadow="never">
      <el-table :data="list" border>
        <el-table-column prop="title" label="标题" min-width="210" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="{ PHYSICAL:'primary', AI:'success', OTHER:'info' }[row.reportType]">
              {{ { PHYSICAL:'体检报告', AI:'自动分析', OTHER:'其他' }[row.reportType] || row.reportType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关注等级" width="130">
          <template #default="{ row }">
            <span v-if="row.riskLevel" class="risk-text" :class="`risk-${row.riskLevel.toLowerCase()}`">
              {{ riskName(row.riskLevel) }}
            </span>
            <span v-else class="muted">旧版报告</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportDate" label="报告日期" width="130" />
        <el-table-column prop="content" label="内容/结论" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="view(row)">查看详情</el-button>
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无健康报告，可先记录体征后生成分析报告" />
      <el-pagination class="pager" layout="prev, pager, next" :total="total"
                     :page-size="query.pageSize" @current-change="onPage" />
    </el-card>

    <el-dialog v-model="generateDialog" title="生成健康分析报告" width="480px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="统计周期">
          <el-radio-group v-model="generateForm.rangeDays">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
            <el-radio-button :value="90">近 90 天</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报告文案">
          <el-switch v-model="generateForm.useAiNarrative" active-text="AI 解读" inactive-text="规则模板" />
          <p class="field-help">医学风险由固定规则计算，AI 仅负责整理表达；服务不可用时自动使用规则模板。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialog = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="doGenerate">生成报告</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialog" :title="viewRow.title || '报告详情'" width="min(980px, 94vw)" append-to-body
               class="report-detail-dialog" @closed="viewRow = {}">
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="viewRow.legacy">
        <el-alert title="这是旧版文本报告，未包含结构化分析数据。" type="info" :closable="false" show-icon />
        <pre class="report-content">{{ viewRow.content }}</pre>
      </template>
      <article v-else-if="viewRow.id" class="detail-content">
        <section class="report-overview" :class="`overview-${viewRow.riskLevel?.toLowerCase()}`">
          <div class="risk-block">
            <span>综合关注等级</span>
            <strong>{{ riskName(viewRow.riskLevel) }}</strong>
          </div>
          <dl class="overview-meta">
            <div><dt>统计周期</dt><dd>{{ shortDate(viewRow.periodStart) }} 至 {{ shortDate(viewRow.periodEnd) }}</dd></div>
            <div><dt>有效记录</dt><dd class="num">{{ viewRow.dataCount }} 条</dd></div>
            <div><dt>数据完整度</dt><dd class="num">{{ percent(viewRow.dataQuality) }}</dd></div>
            <div><dt>生成方式</dt><dd>{{ viewRow.generationMode === 'RULE_AI' ? '规则分析 + AI 文案' : '规则分析模板' }}</dd></div>
          </dl>
        </section>

        <el-alert v-if="viewRow.riskLevel === 'INSUFFICIENT'" title="有效记录不足，当前报告不判断趋势。建议补充至少 3 次且跨 3 个测量日的记录。"
                  type="warning" :closable="false" show-icon />
        <el-alert v-if="viewRow.generationMode === 'RULE'" title="本报告使用规则模板生成，医学统计和风险结果不受 AI 状态影响。"
                  type="info" :closable="false" show-icon />

        <section class="narrative-section">
          <h3>报告摘要</h3>
          <p>{{ viewRow.narrative?.summary }}</p>
          <div v-if="viewRow.riskReasons?.length" class="reason-list">
            <span v-for="reason in viewRow.riskReasons" :key="reason">{{ reason }}</span>
          </div>
        </section>

        <section class="metric-section" v-for="metric in viewRow.metrics" :key="metric.metricType">
          <header class="section-heading">
            <div>
              <h3>{{ metric.metricName }}</h3>
              <span class="metric-count">{{ metric.validCount }} 条有效记录</span>
            </div>
            <span class="risk-badge" :class="`risk-${metric.riskLevel.toLowerCase()}`">{{ riskName(metric.riskLevel) }}</span>
          </header>

          <dl class="stat-row">
            <div><dt>最新值</dt><dd class="num">{{ displayValue(metric) }}</dd></div>
            <div><dt>均值</dt><dd class="num">{{ format(metric.primary.average) }} {{ metric.unit }}</dd></div>
            <div><dt>中位数</dt><dd class="num">{{ format(metric.primary.median) }} {{ metric.unit }}</dd></div>
            <div><dt>范围</dt><dd class="num">{{ format(metric.primary.minimum) }} 至 {{ format(metric.primary.maximum) }}</dd></div>
            <div><dt>异常率</dt><dd class="num">{{ percent(metric.abnormalRate) }}</dd></div>
            <div><dt>趋势</dt><dd>{{ trendName(metric.trend) }}</dd></div>
          </dl>

          <div v-if="metric.secondary" class="secondary-note">
            舒张压均值 {{ format(metric.secondary.average) }} {{ metric.unit }}，中位数 {{ format(metric.secondary.median) }}，
            范围 {{ format(metric.secondary.minimum) }} 至 {{ format(metric.secondary.maximum) }}，趋势{{ trendName(metric.secondaryTrend) }}。
            <span v-if="metric.missingSecondaryCount">有 {{ metric.missingSecondaryCount }} 条记录缺少舒张压。</span>
          </div>

          <div v-if="metric.chart?.points?.length > 1" class="chart-wrap" :aria-label="`${metric.metricName}趋势图`">
            <svg class="trend-chart" :viewBox="`0 0 ${metric.chart.width} ${metric.chart.height}`" role="img">
              <g class="grid-lines">
                <line v-for="tick in metric.chart.ticks" :key="tick.label" x1="46" :x2="metric.chart.width - 18" :y1="tick.y" :y2="tick.y" />
                <text v-for="tick in metric.chart.ticks" :key="`t-${tick.label}`" x="38" :y="tick.y + 4" text-anchor="end">{{ tick.label }}</text>
              </g>
              <path class="trend-line primary-line" :d="metric.chart.path1" />
              <path v-if="metric.chart.path2" class="trend-line secondary-line" :d="metric.chart.path2" />
              <circle v-for="point in metric.chart.points" :key="point.key" :cx="point.x" :cy="point.y"
                      :r="point.abnormal ? 5 : 3.5" :class="['chart-dot', { abnormal: point.abnormal }]">
                <title>{{ point.tip }}</title>
              </circle>
              <g class="x-labels">
                <text v-for="label in metric.chart.labels" :key="label.text + label.x" :x="label.x" :y="metric.chart.height - 5" text-anchor="middle">{{ label.text }}</text>
              </g>
            </svg>
            <div v-if="metric.secondary" class="chart-legend"><span>收缩压</span><span>舒张压</span></div>
          </div>
          <el-empty v-else description="数据点不足，暂不绘制趋势图" :image-size="54" />
        </section>

        <section v-if="viewRow.abnormalEvents?.length" class="abnormal-section">
          <h3>异常记录</h3>
          <ol class="event-list">
            <li v-for="event in viewRow.abnormalEvents" :key="`${event.metricType}-${event.time}-${event.value}`">
              <time>{{ formatTime(event.time) }}</time>
              <div><strong>{{ event.metricName }} {{ eventValue(event) }}</strong><p>{{ event.message }}</p></div>
              <el-tag :type="event.level === 'HIGH' ? 'danger' : 'warning'">{{ event.level === 'HIGH' ? '高度关注' : '需要关注' }}</el-tag>
            </li>
          </ol>
        </section>

        <section class="recommendation-section">
          <h3>行动建议</h3>
          <ul><li v-for="item in viewRow.narrative?.recommendations" :key="item">{{ item }}</li></ul>
        </section>
        <footer class="report-footer">
          <p>{{ viewRow.narrative?.disclaimer }}</p>
          <span>算法版本 {{ viewRow.algorithmVersion }}</span>
        </footer>
      </article>
    </el-dialog>

    <el-dialog v-model="dialog" :title="form.id ? '编辑健康报告' : '新增健康报告'" width="520px" append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.reportType">
            <el-option label="体检报告" value="PHYSICAL" /><el-option label="AI生成" value="AI" /><el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告日期"><el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="内容/结论"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="附件URL"><el-input v-model="form.fileUrl" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存报告</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import gsap from 'gsap'
import { pageReport, createReport, updateReport, deleteReport, generateReport, getReportDetail } from '@/api'

const list = ref([]), total = ref(0), loading = ref(false), dialog = ref(false), form = ref({})
const generating = ref(false), generateDialog = ref(false), detailLoading = ref(false)
const viewDialog = ref(false), viewRow = ref({})
const generateForm = ref({ rangeDays: 30, useAiNarrative: true })
const query = ref({ pageNum: 1, pageSize: 10 })
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

async function doGenerate() {
  generating.value = true
  try {
    const res = await generateReport(generateForm.value)
    generateDialog.value = false
    ElMessage.success(res.data.generationMode === 'RULE_AI' ? '分析报告已生成' : '报告已生成，AI 不可用时已使用规则模板')
    await load()
    openDetail(prepareDetail(res.data))
  } finally { generating.value = false }
}

async function view(row) {
  viewDialog.value = true
  detailLoading.value = true
  try {
    const res = await getReportDetail(row.id)
    viewRow.value = prepareDetail(res.data)
    await animateCharts()
  } finally { detailLoading.value = false }
}

function openDetail(detail) {
  viewRow.value = detail
  viewDialog.value = true
  animateCharts()
}

function prepareDetail(detail) {
  if (!detail || detail.legacy) return detail || {}
  return { ...detail, metrics: (detail.metrics || []).map(metric => ({ ...metric, chart: buildChart(metric) })) }
}

function buildChart(metric) {
  const rows = metric.chartPoints || []
  if (rows.length < 2) return { points: [] }
  const width = 760, height = 220, left = 46, right = 18, top = 18, bottom = 30
  const values = rows.flatMap(row => row.value2 == null ? [row.value] : [row.value, row.value2]).filter(Number.isFinite)
  const rawMin = Math.min(...values), rawMax = Math.max(...values), span = Math.max(rawMax - rawMin, 1)
  const min = rawMin - span * 0.12, max = rawMax + span * 0.12
  const x = i => left + i * (width - left - right) / Math.max(rows.length - 1, 1)
  const y = value => top + (1 - (value - min) / Math.max(max - min, 1)) * (height - top - bottom)
  const points = rows.map((row, i) => ({
    x: x(i), y: y(row.value), abnormal: row.abnormal, key: `${row.time}-${i}`,
    tip: `${formatTime(row.time)}  ${row.value}${row.value2 == null ? '' : '/' + row.value2} ${metric.unit || ''}${row.abnormal ? '（异常）' : ''}`,
  }))
  const path = getter => rows.map((row, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(getter(row)).toFixed(1)}`).join(' ')
  const path2 = rows.every(row => row.value2 != null) ? path(row => row.value2) : null
  const ticks = Array.from({ length: 5 }, (_, i) => {
    const value = min + (max - min) * i / 4
    return { label: format(value), y: y(value) }
  })
  const step = Math.max(1, Math.ceil(rows.length / 6))
  const labels = rows.map((row, i) => ({ x: x(i), text: shortDate(row.time), i }))
    .filter(label => label.i % step === 0 || label.i === rows.length - 1)
  return { width, height, points, path1: path(row => row.value), path2, ticks, labels }
}

async function animateCharts() {
  if (reducedMotion) return
  await nextTick()
  document.querySelectorAll('.report-detail-dialog .trend-line').forEach(line => {
    const length = line.getTotalLength?.() || 0
    if (!length) return
    gsap.fromTo(line, { strokeDasharray: length, strokeDashoffset: length },
      { strokeDashoffset: 0, duration: 0.65, ease: 'power3.out' })
  })
}

async function load() {
  loading.value = true
  try { const res = await pageReport(query.value); list.value = res.data.records; total.value = res.data.total }
  finally { loading.value = false }
}
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) { form.value = row ? { ...row } : { reportType: 'OTHER' }; dialog.value = true }
async function save() {
  form.value.id ? await updateReport(form.value) : await createReport(form.value)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(id) { await ElMessageBox.confirm('确认删除这份报告？', '删除报告'); await deleteReport(id); ElMessage.success('已删除'); load() }

const riskName = risk => ({ HIGH: '高度关注', WARNING: '建议关注', ATTENTION: '轻度关注', NORMAL: '正常', INSUFFICIENT: '数据不足' }[risk] || risk || '未评估')
const trendName = trend => ({ UP: '上升', DOWN: '下降', STABLE: '平稳', INSUFFICIENT: '数据不足' }[trend] || '数据不足')
const format = value => Number.isFinite(Number(value)) ? Number(value).toFixed(Number(value) % 1 === 0 ? 0 : 1) : '—'
const percent = value => Number.isFinite(Number(value)) ? `${(Number(value) * 100).toFixed(1)}%` : '—'
const shortDate = value => String(value || '').slice(5, 10)
const formatTime = value => String(value || '').replace('T', ' ').slice(0, 16)
const displayValue = metric => `${format(metric.primary.latest)}${metric.secondary ? '/' + format(metric.secondary.latest) : ''} ${metric.unit || ''}`
const eventValue = event => `${format(event.value)}${event.value2 == null ? '' : '/' + format(event.value2)} ${event.unit || ''}`

onMounted(load)
</script>

<style scoped>
.report-page { display: flex; flex-direction: column; gap: 20px; }
.page-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.page-toolbar h2 { margin: 0; font-size: 28px; }
.page-toolbar p { margin: 7px 0 0; color: var(--hda-ink-soft); }
.toolbar-actions { display: flex; gap: 10px; flex-shrink: 0; }
.list-panel { background: #fff; }
.pager { margin-top: 18px; justify-content: flex-end; }
.muted, .metric-count { color: var(--hda-ink-soft); font-size: 14px; }
.field-help { margin: 10px 0 0; color: var(--hda-ink-soft); font-size: 14px; line-height: 1.6; max-width: 62ch; }
.risk-text, .risk-badge { font-weight: 700; }
.risk-badge { display: inline-flex; align-items: center; min-height: 30px; padding: 3px 12px; border-radius: 999px; background: #eef3f8; }
.risk-high { color: #b63622; }.risk-warning { color: #a65b0d; }.risk-attention { color: #8a6500; }
.risk-normal { color: #17775a; }.risk-insufficient { color: #52687f; }

.detail-content { display: flex; flex-direction: column; gap: 22px; color: var(--hda-ink); }
.report-overview { display: flex; gap: 28px; padding: 22px; background: #f3f7fc; border-top: 4px solid #6e86a1; }
.overview-high { background: #fff3f0; border-color: #d94d35; }.overview-warning { background: #fff8ee; border-color: #dd8129; }
.overview-attention { background: #fffbed; border-color: #c89a19; }.overview-normal { background: #effaf6; border-color: #2f9a76; }
.risk-block { min-width: 160px; display: flex; flex-direction: column; justify-content: center; }
.risk-block span, .overview-meta dt, .stat-row dt { color: var(--hda-ink-soft); font-size: 14px; }
.risk-block strong { margin-top: 6px; font-size: 28px; line-height: 1.2; }
.overview-meta { margin: 0; flex: 1; display: grid; grid-template-columns: repeat(2, minmax(180px, 1fr)); gap: 16px 24px; }
.overview-meta div, .stat-row div { min-width: 0; }.overview-meta dd, .stat-row dd { margin: 4px 0 0; font-weight: 650; }
.narrative-section, .recommendation-section, .abnormal-section { padding: 2px 4px; }
.detail-content h3 { margin: 0 0 12px; font-size: 20px; }.narrative-section p { margin: 0; max-width: 72ch; }
.reason-list { margin-top: 14px; display: flex; flex-wrap: wrap; gap: 8px; }
.reason-list span { padding: 5px 10px; background: #f3f6fa; color: #354e69; font-size: 14px; }
.metric-section { padding: 20px 0 8px; border-top: 1px solid var(--hda-line); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.section-heading > div { display: flex; align-items: baseline; gap: 10px; }.section-heading h3 { margin: 0; }
.stat-row { margin: 0; display: grid; grid-template-columns: repeat(6, minmax(100px, 1fr)); background: #f7f9fc; }
.stat-row div { padding: 13px 14px; border-right: 1px solid var(--hda-line); }.stat-row div:last-child { border-right: 0; }
.secondary-note { margin-top: 12px; color: #415b77; font-size: 14px; }
.chart-wrap { margin-top: 16px; min-height: 220px; overflow-x: auto; }
.trend-chart { display: block; width: 100%; min-width: 620px; height: 220px; }
.grid-lines line { stroke: #e4eaf2; stroke-width: 1; }.grid-lines text, .x-labels text { fill: #60758d; font-size: 11px; }
.trend-line { fill: none; stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; }
.primary-line { stroke: var(--el-color-primary); }.secondary-line { stroke: #37a889; stroke-dasharray: 6 4; }
.chart-dot { fill: #fff; stroke: var(--el-color-primary); stroke-width: 2; }.chart-dot.abnormal { fill: #e5654b; stroke: #fff; }
.chart-legend { display: flex; justify-content: center; gap: 22px; color: var(--hda-ink-soft); font-size: 13px; }
.chart-legend span::before { content: ''; display: inline-block; width: 16px; height: 3px; margin-right: 6px; vertical-align: middle; background: var(--el-color-primary); }
.chart-legend span:last-child::before { background: #37a889; }
.event-list { list-style: none; margin: 0; padding: 0; }.event-list li { display: grid; grid-template-columns: 130px 1fr auto; gap: 16px; align-items: start; padding: 14px 0; border-bottom: 1px solid var(--hda-line); }
.event-list time { color: var(--hda-ink-soft); font-variant-numeric: tabular-nums; }.event-list strong { font-size: 16px; }.event-list p { margin: 3px 0 0; color: #405872; }
.recommendation-section ul { margin: 0; padding-left: 22px; }.recommendation-section li { margin: 7px 0; }
.report-footer { display: flex; justify-content: space-between; gap: 24px; padding-top: 16px; border-top: 1px solid var(--hda-line); color: var(--hda-ink-soft); font-size: 13px; }
.report-footer p { margin: 0; max-width: 72ch; }.report-footer span { white-space: nowrap; }
.report-content { margin: 16px 0 0; padding: 16px; white-space: pre-wrap; word-break: break-word; font-family: inherit; font-size: 16px; line-height: 1.8; color: var(--hda-ink); background: var(--hda-bg-soft); max-height: 60vh; overflow-y: auto; }

@media (max-width: 900px) {
  .stat-row { grid-template-columns: repeat(3, 1fr); }.stat-row div:nth-child(3) { border-right: 0; }
}
@media (max-width: 680px) {
  .page-toolbar, .report-overview { align-items: stretch; flex-direction: column; }.toolbar-actions { width: 100%; }.toolbar-actions .el-button { flex: 1; }
  .overview-meta { grid-template-columns: 1fr 1fr; }.stat-row { grid-template-columns: repeat(2, 1fr); }
  .stat-row div:nth-child(3) { border-right: 1px solid var(--hda-line); }.stat-row div:nth-child(even) { border-right: 0; }
  .event-list li { grid-template-columns: 1fr auto; }.event-list time { grid-column: 1 / -1; }.report-footer { flex-direction: column; }
}
@media (prefers-reduced-motion: reduce) { .trend-line { stroke-dasharray: none !important; stroke-dashoffset: 0 !important; } }
</style>
