import request from './request'
import { encryptPassword } from './crypto'
import { apiUrl, createWebSocketUrl } from '@/config/server'

// 认证（登录/注册/重置：密码经 RSA 加密后再提交）
export const login = async (data) =>
  request.post('/auth/login', { ...data, password: await encryptPassword(data.password) })
export const register = async (data) =>
  request.post('/auth/register', { ...data, password: await encryptPassword(data.password) })
export const sendSmsCode = (data) => request.post('/auth/sms-code', data)
export const phoneLogin = (data) => request.post('/auth/phone-login', data)
export const resetPassword = async (data) =>
  request.post('/auth/reset-password', { ...data, newPassword: await encryptPassword(data.newPassword) })
export const refreshToken = (data) => request.post('/auth/refresh', data)
export const logoutApi = () => request.post('/auth/logout')
export const getIdempotencyToken = () => request.get('/idempotent/token')

// 账户
export const getMe = () => request.get('/account/me')
export const updateProfile = (data) => request.put('/account/profile', data)
export const changePassword = (data) => request.put('/account/password', data)
export const deactivate = () => request.delete('/account/deactivate')

// 健康档案-基本信息
export const getHealthProfile = () => request.get('/health/profile')
export const saveHealthProfile = (data) => request.post('/health/profile', data)

// 体征/体检
export const pageMetric = (params) => request.get('/health/metric/page', { params })
export const createMetric = (data) => request.post('/health/metric', data)
export const updateMetric = (data) => request.put('/health/metric', data)
export const deleteMetric = (id) => request.delete(`/health/metric/${id}`)

// 就诊/用药
export const pageMedical = (params) => request.get('/health/medical-record/page', { params })
export const createMedical = (data) => request.post('/health/medical-record', data)
export const updateMedical = (data) => request.put('/health/medical-record', data)
export const deleteMedical = (id) => request.delete(`/health/medical-record/${id}`)

// 健康报告
export const pageReport = (params) => request.get('/health/report/page', { params })
export const createReport = (data) => request.post('/health/report', data)
export const updateReport = (data) => request.put('/health/report', data)
export const deleteReport = (id) => request.delete(`/health/report/${id}`)

// 积分
export const getPointBalance = () => request.get('/point/balance')
export const checkIn = () => request.post('/point/check-in')
export const getPointTasks = () => request.get('/point/tasks')
export const claimTask = (type) => request.post('/point/claim/' + type)
export const pagePointRecords = (params) => request.get('/point/records', { params })
export const pagePointProducts = (params) => request.get('/point/products', { params })
export const getProductCategories = () => request.get('/point/categories')
// 兑换：先领幂等令牌，随 Idempotency-Key 头提交，防重复兑换
export const exchangeProduct = async (data) => {
  const res = await getIdempotencyToken()
  return request.post('/point/exchange', data, {
    headers: { 'Idempotency-Key': res.data.idempotencyKey }
  })
}
export const pageMyExchanges = (params) => request.get('/point/exchanges', { params })

// AI 模块
export const pageDoctors = (params) => request.get('/doctor/page', { params })
export const getDoctorDepartments = () => request.get('/doctor/departments')
export const consultChat = (data) => request.post('/consult/chat', data)
export const consultHistory = (params) => request.get('/consult/history', { params })
export const consultSessions = () => request.get('/consult/sessions')

export const adminPageKnowledge = (params) => request.get('/admin/knowledge/page', { params })
export const adminKnowledgeChunks = (id) => request.get(`/admin/knowledge/${id}/chunks`)
const KNOWLEDGE_PARSE_TIMEOUT = 120000
export const adminUploadKnowledge = (formData) => request.post('/admin/knowledge', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: KNOWLEDGE_PARSE_TIMEOUT })
export const adminImportKnowledgeSeeds = () => request.post('/admin/knowledge/seed', null, { timeout: KNOWLEDGE_PARSE_TIMEOUT })
export const adminPublishKnowledge = (id) => request.post(`/admin/knowledge/${id}/publish`)
export const adminInactiveKnowledge = (id) => request.put(`/admin/knowledge/${id}/inactive`)
export const adminDeleteKnowledge = (id) => request.delete(`/admin/knowledge/${id}`)
export const adminPublishKnowledgeBatch = (ids) => request.post('/admin/knowledge/batch/publish', ids, { timeout: 120000 })
export const adminInactiveKnowledgeBatch = (ids) => request.put('/admin/knowledge/batch/inactive', ids)
export const adminPageApplicationKnowledge = (params) => request.get('/admin/knowledge/application/page', { params })
export const adminUploadApplicationKnowledge = (formData) => request.post('/admin/knowledge/application', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: KNOWLEDGE_PARSE_TIMEOUT })
export const adminImportApplicationKnowledgeSeeds = () => request.post('/admin/knowledge/application/seed', null, { timeout: KNOWLEDGE_PARSE_TIMEOUT })

// Axios 不直接消费浏览器 POST 响应流；AI 助手接口统一使用 fetch 解析 SSE。
async function postSseStream (url, data, handlers, assistantName) {
  const response = await fetch(apiUrl(url), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(localStorage.getItem('token') ? { Authorization: `Bearer ${localStorage.getItem('token')}` } : {})
    },
    body: JSON.stringify(data),
    signal: handlers.signal
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message || `${assistantName}请求失败（HTTP ${response.status}）`)
  }
  if (!response.body) throw new Error('当前浏览器不支持流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const consume = (block) => {
    if (!block.trim()) return
    let eventName = 'message'
    const dataLines = []
    block.split(/\r?\n/).forEach(line => {
      if (line.startsWith('event:')) eventName = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    })
    if (!dataLines.length) return
    let event
    try { event = JSON.parse(dataLines.join('\n')) } catch { throw new Error(`${assistantName}返回了无法识别的流式数据`) }
    const type = event.type || eventName
    if (type === 'meta') handlers.onMeta?.(event)
    else if (type === 'stage') handlers.onStage?.(event)
    else if (type === 'risk') handlers.onRisk?.(event)
    else if (type === 'citation') handlers.onCitation?.(event.citation)
    else if (type === 'intake') handlers.onIntake?.(event.intakeQuestion)
    else if (type === 'delta') handlers.onDelta?.(event.content || '')
    else if (type === 'done') handlers.onDone?.()
    else if (type === 'error') throw new Error(event.content || `${assistantName}暂时不可用`)
  }

  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      const blocks = buffer.split(/\r?\n\r?\n/)
      buffer = blocks.pop() || ''
      blocks.forEach(consume)
      if (done) break
    }
    if (buffer.trim()) consume(buffer)
  } finally {
    reader.releaseLock()
  }
}

export function consultChatStream (data, handlers = {}) {
  return postSseStream('/consult/chat/stream', data, handlers, '健康助手')
}

export function applicationAssistantChatStream (data, handlers = {}) {
  return postSseStream('/app-assistant/chat/stream', data, handlers, '应用助手')
}

export const startDoctorSession = (doctorId) => request.post(`/doctor-consult/session/${doctorId}`)
export const pageDoctorConsultSessions = (params) => request.get('/doctor-consult/sessions', { params })
export const getDoctorConsultMessages = (sessionId) => request.get(`/doctor-consult/session/${sessionId}/messages`)
export const sendDoctorConsultMessage = (sessionId, data) => request.post(`/doctor-consult/session/${sessionId}/messages`, data)
export const closeDoctorConsultSession = (sessionId) => request.put(`/doctor-consult/session/${sessionId}/close`)
export const submitDoctorConsultFeedback = (sessionId, data) => request.post(`/doctor-consult/session/${sessionId}/feedback`, data)
export const getPatientMedicationAdvice = (sessionId) => request.get(`/doctor-consult/session/${sessionId}/medication-advice`)
export const confirmMedicationAdvice = (adviceId) => request.put(`/doctor-consult/medication-advice/${adviceId}/confirm`)
export const uploadConsultAttachment = (formData) => request.post('/file/consult-attachment', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export function doctorConsultWsUrl() {
  const token = localStorage.getItem('token') || ''
  return createWebSocketUrl('/ws/doctor-consult', { token })
}
export const pageAlerts = (params) => request.get('/alert/page', { params })
export const generateAlert = () => request.post('/alert/generate')
export const markAlertRead = (id) => request.put(`/alert/${id}/read`)
export const generateReport = () => request.post('/health/report/generate')
export const uploadAvatar = (formData) => request.post('/file/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// 管理端
export const adminPageUsers = (params) => request.get('/admin/user/page', { params })
export const adminPageProducts = (params) => request.get('/admin/product/page', { params })
export const adminCreateProduct = (data) => request.post('/admin/product', data)
export const adminUpdateProduct = (data) => request.put('/admin/product', data)
export const adminDeleteProduct = (id) => request.delete(`/admin/product/${id}`)
export const adminPageMedicines = (params) => request.get('/admin/medicine/page', { params })
export const adminCreateMedicine = (data) => request.post('/admin/medicine', data)
export const adminUpdateMedicine = (data) => request.put('/admin/medicine', data)
export const adminDeleteMedicine = (id) => request.delete(`/admin/medicine/${id}`)
export const adminPageDoctors = (params) => request.get('/admin/doctor/page', { params })
export const adminCreateDoctor = (data) => request.post('/admin/doctor', data)
export const adminUpdateDoctor = (data) => request.put('/admin/doctor', data)
export const adminDeleteDoctor = (id) => request.delete(`/admin/doctor/${id}`)
export const adminAuditDoctor = (id, approved) => request.put(`/admin/doctor/${id}/audit`, null, { params: { approved } })
