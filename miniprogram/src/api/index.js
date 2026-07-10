import { request } from './request'

const qs = (p = {}) => {
  const s = Object.keys(p).filter(k => p[k] !== undefined && p[k] !== '' && p[k] !== null)
    .map(k => `${k}=${encodeURIComponent(p[k])}`).join('&')
  return s ? '?' + s : ''
}

// 认证
export const login = (data) => request({ url: '/auth/login', method: 'POST', data })
export const register = (data) => request({ url: '/auth/register', method: 'POST', data })

// 账户
export const getMe = () => request({ url: '/account/me' })
export const updateProfile = (data) => request({ url: '/account/profile', method: 'PUT', data })
export const changePassword = (data) => request({ url: '/account/password', method: 'PUT', data })
export const deactivate = () => request({ url: '/account/deactivate', method: 'DELETE' })

// 健康档案-基本信息
export const getHealthProfile = () => request({ url: '/health/profile' })
export const saveHealthProfile = (data) => request({ url: '/health/profile', method: 'POST', data })

// 体征数据
export const pageMetric = (p) => request({ url: '/health/metric/page' + qs(p) })
export const createMetric = (data) => request({ url: '/health/metric', method: 'POST', data })
export const updateMetric = (data) => request({ url: '/health/metric', method: 'PUT', data })
export const deleteMetric = (id) => request({ url: `/health/metric/${id}`, method: 'DELETE' })

// 就诊/用药记录
export const pageMedical = (p) => request({ url: '/health/medical-record/page' + qs(p) })
export const createMedical = (data) => request({ url: '/health/medical-record', method: 'POST', data })
export const updateMedical = (data) => request({ url: '/health/medical-record', method: 'PUT', data })
export const deleteMedical = (id) => request({ url: `/health/medical-record/${id}`, method: 'DELETE' })

// 健康报告
export const pageReport = (p) => request({ url: '/health/report/page' + qs(p) })
export const createReport = (data) => request({ url: '/health/report', method: 'POST', data })
export const updateReport = (data) => request({ url: '/health/report', method: 'PUT', data })
export const deleteReport = (id) => request({ url: `/health/report/${id}`, method: 'DELETE' })

// 积分
export const getPointBalance = () => request({ url: '/point/balance' })
export const pagePointRecords = (p) => request({ url: '/point/records' + qs(p) })
export const pagePointProducts = (p) => request({ url: '/point/products' + qs(p) })
export const exchangeProduct = (data) => request({ url: '/point/exchange', method: 'POST', data })
export const pageMyExchanges = (p) => request({ url: '/point/exchanges' + qs(p) })

// 医生专家库
export const pageDoctors = (p) => request({ url: '/doctor/page' + qs(p) })

// AI 健康咨询
export const consultChat = (data) => request({ url: '/consult/chat', method: 'POST', data })
export const consultHistory = (p) => request({ url: '/consult/history' + qs(p) })

// 健康预警
export const pageAlerts = (p) => request({ url: '/alert/page' + qs(p) })
export const generateAlert = () => request({ url: '/alert/generate', method: 'POST' })
export const readAlert = (id) => request({ url: `/alert/${id}/read`, method: 'PUT' })
