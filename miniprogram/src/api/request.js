// 后端地址：真机调试请改为电脑局域网IP，如 http://192.168.1.100:8080
const BASE_URL = 'http://localhost:8080/api'

export function request(options) {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: 'Bearer ' + token } : {})
      },
      success: (res) => {
        const r = res.data
        if (r.code === 200) {
          resolve(r)
        } else if (r.code === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(r)
        } else {
          uni.showToast({ title: r.message || '请求失败', icon: 'none' })
          reject(r)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}
