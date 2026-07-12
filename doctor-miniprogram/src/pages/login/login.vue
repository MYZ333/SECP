<template>
  <view class="wrap">
    <view class="hero">
      <text class="brand">智慧医养医生端</text>
      <text class="slogan">患者咨询 · 健康档案 · 实时沟通</text>
    </view>
    <view class="card panel">
      <view class="tabs">
        <text :class="['tab', mode==='login' ? 'on' : '']" @click="mode='login'">登录</text>
        <text :class="['tab', mode==='register' ? 'on' : '']" @click="mode='register'">注册</text>
      </view>
      <input class="ipt" v-model="form.username" placeholder="用户名" />
      <input class="ipt" v-model="form.password" password placeholder="密码" />
      <template v-if="mode==='register'">
        <input class="ipt" v-model="form.name" placeholder="姓名" />
        <input class="ipt" v-model="form.phone" placeholder="手机号" />
        <input class="ipt" v-model="form.hospital" placeholder="医院" />
        <input class="ipt" v-model="form.department" placeholder="科室" />
        <input class="ipt" v-model="form.title" placeholder="职称" />
        <input class="ipt" v-model="form.speciality" placeholder="擅长领域" />
      </template>
      <button class="btn-primary submit" @click="submit">{{ mode === 'login' ? '登录' : '提交审核' }}</button>
      <text class="muted tip">注册后需管理员审核，通过后可登录。</text>
    </view>
  </view>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { login, registerDoctor } from '@/api'
const mode = ref('login')
const form = reactive({ username: '', password: '', name: '', phone: '', hospital: '', department: '', title: '', speciality: '' })
onMounted(() => { if (uni.getStorageSync('doctorToken')) uni.reLaunch({ url: '/pages/dashboard/dashboard' }) })
async function submit() {
  if (!form.username || !form.password) return uni.showToast({ title: '请输入账号密码', icon: 'none' })
  if (mode.value === 'login') {
    const res = await login({ username: form.username, password: form.password })
    if (res.data.role !== 'DOCTOR') return uni.showToast({ title: '请使用医生账号登录', icon: 'none' })
    uni.setStorageSync('doctorToken', res.data.token)
    uni.setStorageSync('doctorInfo', res.data)
    uni.reLaunch({ url: '/pages/dashboard/dashboard' })
  } else {
    await registerDoctor(form)
    uni.showToast({ title: '已提交审核', icon: 'none' })
    mode.value = 'login'
  }
}
</script>

<style scoped>
.wrap { min-height: 100vh; }
.hero { padding: 150rpx 40rpx 100rpx; background: linear-gradient(135deg, #3E86EC, #2458B8); color: #fff; }
.brand { display: block; font-size: 52rpx; font-weight: 900; }
.slogan { display: block; margin-top: 14rpx; opacity: .9; }
.panel { margin: -54rpx 32rpx 0; }
.tabs { display: flex; gap: 40rpx; margin-bottom: 28rpx; }
.tab { font-size: 36rpx; font-weight: 800; color: var(--ink-soft); }
.tab.on { color: var(--primary); }
.ipt { height: 88rpx; padding: 0 24rpx; margin-bottom: 22rpx; background: #F7FAFD; border: 2rpx solid var(--line); }
.submit { width: 100%; margin-top: 10rpx; }
.tip { display: block; text-align: center; margin-top: 18rpx; font-size: 24rpx; }
</style>
