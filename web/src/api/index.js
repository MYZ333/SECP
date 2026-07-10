import request from './request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)

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
export const pagePointRecords = (params) => request.get('/point/records', { params })
export const pagePointProducts = (params) => request.get('/point/products', { params })
export const exchangeProduct = (data) => request.post('/point/exchange', data)
export const pageMyExchanges = (params) => request.get('/point/exchanges', { params })

// AI 模块
export const pageDoctors = (params) => request.get('/doctor/page', { params })
export const consultChat = (data) => request.post('/consult/chat', data)
export const consultHistory = (params) => request.get('/consult/history', { params })
export const pageAlerts = (params) => request.get('/alert/page', { params })
export const generateAlert = () => request.post('/alert/generate')

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
