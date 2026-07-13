import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 无拦截器的裸实例，专用于刷新令牌，避免拦截器递归
const rawAxios = axios.create({ baseURL: '/api', timeout: 15000 })

// 请求拦截：自动携带 JWT
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

// —— 令牌刷新的并发控制：刷新期间其它请求排队，拿到新 token 后统一重放 ——
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
    config.headers['Authorization'] = 'Bearer ' + newToken
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

  // 被挤下线 / token 被登出拉黑：直接退出，不刷新
  if (data.code === 1101 || data.code === 1102) {
    ElMessage.error(data.message || '登录状态已失效，请重新登录')
    return forceLogout(data)
  }
  // 无 refreshToken，或刷新后仍未通过：退出
  if (!userStore.refreshToken || config._retry) {
    ElMessage.error(data.message || '登录已过期，请重新登录')
    return forceLogout(data)
  }
  // 刷新进行中：本请求排队，等新 token 后重放
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push({ resolve, reject, config })
    })
  }
  // 发起刷新
  isRefreshing = true
  try {
    const r = await rawAxios.post('/auth/refresh', { refreshToken: userStore.refreshToken })
    const body = r.data
    if (body.code === 200 && body.data && body.data.token) {
      userStore.setLogin(body.data) // 轮换 access + refresh
      const newToken = body.data.token
      flushQueue(null, newToken)
      config._retry = true
      config.headers = config.headers || {}
      config.headers['Authorization'] = 'Bearer ' + newToken
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

// 响应拦截
request.interceptors.response.use(
  async response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    // 后端未认证入口当前返回 HTTP 200 + 业务码 401，也需要走 refreshToken 续期
    if (res.code === 401) {
      return handleUnauthorized(res, response.config)
    }
    // HTTP 200 但业务码非 200（如限流、幂等重复等）
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  async error => {
    const { response, config } = error
    if (!response) {
      ElMessage.error(error.message || '网络错误')
      return Promise.reject(error)
    }
    const status = response.status
    const data = response.data || {}

    // 限流
    if (status === 429) {
      ElMessage.error(data.message || '请求过于频繁，请稍后再试')
      return Promise.reject(data)
    }

    if (status === 401) {
      return handleUnauthorized(data, config)
    }

    ElMessage.error(data.message || error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default request
