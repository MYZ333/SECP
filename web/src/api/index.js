import request from './request'
import { encryptPassword } from './crypto'

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
export const startDoctorSession = (doctorId) => request.post(`/doctor-consult/session/${doctorId}`)
export const pageDoctorConsultSessions = (params) => request.get('/doctor-consult/sessions', { params })
export const getDoctorConsultMessages = (sessionId) => request.get(`/doctor-consult/session/${sessionId}/messages`)
export const sendDoctorConsultMessage = (sessionId, data) => request.post(`/doctor-consult/session/${sessionId}/messages`, data)
export const uploadConsultAttachment = (formData) => request.post('/file/consult-attachment', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export function doctorConsultWsUrl() {
  const token = localStorage.getItem('token') || ''
  const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
  return `${protocol}://${location.host}/ws/doctor-consult?token=${encodeURIComponent(token)}`
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
export const adminPageDoctors = (params) => request.get('/admin/doctor/page', { params })
export const adminCreateDoctor = (data) => request.post('/admin/doctor', data)
export const adminUpdateDoctor = (data) => request.put('/admin/doctor', data)
export const adminDeleteDoctor = (id) => request.delete(`/admin/doctor/${id}`)
export const adminAuditDoctor = (id, approved) => request.put(`/admin/doctor/${id}/audit`, null, { params: { approved } })
