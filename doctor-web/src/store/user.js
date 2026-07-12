import { defineStore } from 'pinia'

export const useUserStore = defineStore('doctorUser', {
  state: () => ({
    token: localStorage.getItem('doctorToken') || '',
    refreshToken: localStorage.getItem('doctorRefreshToken') || '',
    userInfo: JSON.parse(localStorage.getItem('doctorUserInfo') || '{}')
  }),
  getters: {
    isLogin: state => !!state.token,
    isDoctor: state => state.userInfo.role === 'DOCTOR'
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.refreshToken = data.refreshToken || ''
      this.userInfo = data
      localStorage.setItem('doctorToken', data.token)
      if (data.refreshToken) localStorage.setItem('doctorRefreshToken', data.refreshToken)
      localStorage.setItem('doctorUserInfo', JSON.stringify(data))
    },
    updateUserInfo(data = {}) {
      this.userInfo = { ...this.userInfo, ...data }
      localStorage.setItem('doctorUserInfo', JSON.stringify(this.userInfo))
    },
    logout() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = {}
      localStorage.removeItem('doctorToken')
      localStorage.removeItem('doctorRefreshToken')
      localStorage.removeItem('doctorUserInfo')
    }
  }
})
