export const BASE_URL = 'http://localhost:8080/api'

export function request(options) {
  const token = uni.getStorageSync('doctorToken')
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
        if (r.code === 200) return resolve(r)
        if (res.statusCode === 401 || r.code === 401) {
          uni.removeStorageSync('doctorToken')
          uni.reLaunch({ url: '/pages/login/login' })
        }
        uni.showToast({ title: r.message || '请求失败', icon: 'none' })
        reject(r)
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function uploadFile(filePath) {
  const token = uni.getStorageSync('doctorToken')
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/file/consult-attachment',
      filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
        if (body.code === 200) return resolve(body)
        uni.showToast({ title: body.message || '上传失败', icon: 'none' })
        reject(body)
      },
      fail: reject
    })
  })
}
