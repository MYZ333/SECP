import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isAdmin: (state) => state.userInfo.role === 'ADMIN'
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.refreshToken = data.refreshToken || ''
      this.userInfo = data
      localStorage.setItem('token', data.token)
      if (data.refreshToken) {
        localStorage.setItem('refreshToken', data.refreshToken)
      }
      localStorage.setItem('userInfo', JSON.stringify(data))
    },
    // 刷新令牌后仅更新 access token
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    updateUserInfo(data = {}) {
      this.userInfo = { ...this.userInfo, ...data }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    logout() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = {}
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    }
  }
})
