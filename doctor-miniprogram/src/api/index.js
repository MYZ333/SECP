import { request, uploadFile } from './request'

const qs = (p = {}) => {
  const s = Object.keys(p).filter(k => p[k] !== undefined && p[k] !== '' && p[k] !== null)
    .map(k => `${k}=${encodeURIComponent(p[k])}`).join('&')
  return s ? '?' + s : ''
}

export const login = (data) => request({ url: '/doctor-auth/login', method: 'POST', data })
export const registerDoctor = (data) => request({ url: '/doctor-auth/register', method: 'POST', data })
export const getMe = () => request({ url: '/doctor-portal/me' })
export const updateMe = (data) => request({ url: '/doctor-portal/me', method: 'PUT', data })
export const getStats = () => request({ url: '/doctor-portal/stats' })
export const pagePatients = (p) => request({ url: '/doctor-portal/patients' + qs(p) })
export const getPatientDetail = (id) => request({ url: `/doctor-portal/patient/${id}` })
export const pageSessions = (p) => request({ url: '/doctor-portal/sessions' + qs(p) })
export const getSessionMessages = (id) => request({ url: `/doctor-portal/session/${id}/messages` })
export const sendSessionMessage = (id, data) => request({ url: `/doctor-portal/session/${id}/messages`, method: 'POST', data })
export const uploadAttachment = (filePath) => uploadFile(filePath)
