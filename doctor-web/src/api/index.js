import request from './request'
import { encryptPassword } from './crypto'
import { createWebSocketUrl } from '@/config/server'

export const login = async (data) =>
  request.post('/doctor-auth/login', { ...data, password: await encryptPassword(data.password) })

export const registerDoctor = async (data) =>
  request.post('/doctor-auth/register', { ...data, password: await encryptPassword(data.password) })

export const getMe = () => request.get('/doctor-portal/me')
export const updateMe = (data) => request.put('/doctor-portal/me', data)
export const getStats = () => request.get('/doctor-portal/stats')
export const pagePatients = (params) => request.get('/doctor-portal/patients', { params })
export const getPatientDetail = (id) => request.get(`/doctor-portal/patient/${id}`)
export const pageSessions = (params) => request.get('/doctor-portal/sessions', { params })
export const getSessionMessages = (id) => request.get(`/doctor-portal/session/${id}/messages`)
export const sendSessionMessage = (id, data) => request.post(`/doctor-portal/session/${id}/messages`, data)
export const closeSession = (id) => request.put(`/doctor-portal/session/${id}/close`)
export const uploadAttachment = (formData) =>
  request.post('/file/consult-attachment', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const uploadAvatar = (formData) =>
  request.post('/file/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

export function doctorWsUrl() {
  const token = localStorage.getItem('doctorToken') || ''
  return createWebSocketUrl('/ws/doctor-consult', { token })
}
