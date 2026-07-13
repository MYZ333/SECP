import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({ baseURL: '/api', timeout: 15000 })
const rawAxios = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.request.use(config => {
  const token = localStorage.getItem('doctorToken')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

let isRefreshing = false
let pendingQueue = []

function flushQueue(error, newToken) {
  pendingQueue.forEach(({ resolve, reject, config }) => {
    if (error) {
      reject(error)
      return
    }
    config._retry = true
    config.headers = config.headers || {}
    config.headers.Authorization = 'Bearer ' + newToken
    resolve(request(config))
  })
  pendingQueue = []
}

function forceLogout(payload) {
  const userStore = useUserStore()
  userStore.logout()
  router.push('/login')
  return Promise.reject(payload)
}

async function handleUnauthorized(data, config) {
  const userStore = useUserStore()

  if (data.code === 1101 || data.code === 1102) {
    ElMessage.error(data.message || '登录状态已失效，请重新登录')
    return forceLogout(data)
  }

  if (!userStore.refreshToken || config._retry) {
    ElMessage.error(data.message || '登录已过期，请重新登录')
    return forceLogout(data)
  }

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push({ resolve, reject, config })
    })
  }

  isRefreshing = true
  try {
    const r = await rawAxios.post('/auth/refresh', { refreshToken: userStore.refreshToken })
    const body = r.data
    if (body.code === 200 && body.data && body.data.token) {
      userStore.setLogin(body.data)
      const newToken = body.data.token
      flushQueue(null, newToken)
      config._retry = true
      config.headers = config.headers || {}
      config.headers.Authorization = 'Bearer ' + newToken
      return request(config)
    }
    flushQueue(body)
    return forceLogout(body)
  } catch (e) {
    flushQueue(e)
    return forceLogout(e)
  } finally {
    isRefreshing = false
  }
}

request.interceptors.response.use(
  async response => {
    const res = response.data
    if (res.code === 200) return res
    if (res.code === 401) {
      return handleUnauthorized(res, response.config)
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  async error => {
    const status = error.response?.status
    const data = error.response?.data || {}
    if (status === 401) {
      return handleUnauthorized(data, error.config)
    }
    ElMessage.error(data.message || error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default request
