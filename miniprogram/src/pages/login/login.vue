<template>
  <view class="wrap">
    <!-- 顶部渐变品牌区 -->
    <view class="hero">
      <view class="orb"><text class="orb-ic">✚</text></view>
      <text class="brand">智慧医养</text>
      <text class="slogan">个人健康档案 · 随身管理</text>
    </view>

    <!-- 登录卡片 -->
    <view class="card login-card enter">
      <view class="tabs">
        <text :class="['tab', mode==='login' ? 'on' : '']" @click="mode='login'">登录</text>
        <text :class="['tab', mode==='register' ? 'on' : '']" @click="mode='register'">注册</text>
      </view>

      <view class="field">
        <text class="ic">👤</text>
        <input class="ipt" v-model="form.username" placeholder="请输入用户名" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="ic">🔒</text>
        <input class="ipt" v-model="form.password" password placeholder="请输入密码" placeholder-class="ph" />
      </view>
      <template v-if="mode==='register'">
        <view class="field">
          <text class="ic">📝</text>
          <input class="ipt" v-model="form.nickname" placeholder="昵称（可选）" placeholder-class="ph" />
        </view>
        <view class="field">
          <text class="ic">📱</text>
          <input class="ipt" v-model="form.phone" placeholder="手机号（可选）" placeholder-class="ph" />
        </view>
      </template>

      <button class="btn-primary submit" @click="submit">{{ mode==='login' ? '登 录' : '注 册' }}</button>
      <text class="tip muted">演示账号：user001 / 123456</text>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { onMounted } from 'vue'
import { login, register } from '@/api/index'
const mode = ref('login')
const form = reactive({ username: '', password: '', nickname: '', phone: '' })

// 已登录则直接进入健康咨询页
onMounted(() => {
  if (uni.getStorageSync('token')) uni.reLaunch({ url: '/pages/consult/consult' })
})

async function submit() {
  if (!form.username || !form.password) return uni.showToast({ title: '请输入账号密码', icon: 'none' })
  try {
    if (mode.value === 'login') {
      const res = await login({ username: form.username, password: form.password })
      uni.setStorageSync('token', res.data.token)
      uni.setStorageSync('userInfo', res.data)
      uni.reLaunch({ url: '/pages/consult/consult' })
    } else {
      await register(form)
      uni.showToast({ title: '注册成功，请登录', icon: 'none' })
      mode.value = 'login'
    }
  } catch (e) {}
}
</script>

<style scoped>
.wrap { min-height: 100vh; }
.hero {
  padding: 150rpx 40rpx 90rpx; display: flex; flex-direction: column; align-items: center;
  background: linear-gradient(160deg, #37B389 0%, #2FA37C 55%, #23856A 100%);
  border-bottom-left-radius: 60rpx; border-bottom-right-radius: 60rpx;
}
.orb {
  width: 128rpx; height: 128rpx; border-radius: 40rpx; background: rgba(255,255,255,.2);
  display: flex; align-items: center; justify-content: center; margin-bottom: 24rpx;
  animation: floatY 4s ease-in-out infinite;
}
.orb-ic { font-size: 64rpx; color: #fff; }
.brand { color: #fff; font-size: 52rpx; font-weight: 800; letter-spacing: 2rpx; }
.slogan { color: rgba(255,255,255,.9); font-size: 28rpx; margin-top: 12rpx; }

.login-card { margin: -60rpx 40rpx 0; position: relative; }
.tabs { display: flex; gap: 40rpx; margin-bottom: 36rpx; }
.tab { font-size: 38rpx; font-weight: 700; color: var(--ink-soft); padding-bottom: 10rpx; position: relative; }
.tab.on { color: var(--primary); }
.tab.on::after { content: ''; position: absolute; left: 0; right: 0; bottom: 0; height: 6rpx; border-radius: 6rpx; background: var(--primary); }

.field {
  display: flex; align-items: center; gap: 16rpx; background: #F6F4EE;
  border-radius: 18rpx; padding: 8rpx 24rpx; margin-bottom: 24rpx;
}
.ic { font-size: 34rpx; }
.ipt { flex: 1; height: 92rpx; font-size: 32rpx; }
.ph { color: #B4BDB7; }
.submit { width: 100%; height: 96rpx; line-height: 96rpx; margin-top: 12rpx; letter-spacing: 8rpx; }
.tip { display: block; text-align: center; margin-top: 24rpx; font-size: 24rpx; }
</style>
