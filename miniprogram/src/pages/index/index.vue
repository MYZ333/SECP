<template>
  <view class="page-pad">
    <!-- 欢迎横幅 -->
    <view class="banner enter">
      <view class="ring"></view>
      <text class="hi">{{ greeting }}，{{ userInfo.nickname || '朋友' }}</text>
      <text class="big">祝您健康每一天</text>
      <text class="date">{{ today }}</text>
    </view>

    <!-- 统计卡 -->
    <view class="stats">
      <view class="stat enter-1" style="--c:#E8933B">
        <text class="num s-num">{{ balance }}</text><text class="s-lb">我的积分</text>
      </view>
      <view class="stat enter-2" style="--c:#2FA37C">
        <text class="num s-num">{{ metricCount }}</text><text class="s-lb">体征记录</text>
      </view>
      <view class="stat enter-3" style="--c:#E5654B">
        <text class="num s-num">{{ alertCount }}</text><text class="s-lb">健康预警</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <text class="sec enter-3">快捷入口</text>
    <view class="grid enter-4">
      <view class="tile" v-for="q in quicks" :key="q.url" @click="go(q.url)" :style="{ '--c': q.c }">
        <text class="t-ic">{{ q.ic }}</text>
        <text class="t-n">{{ q.n }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPointBalance, pageMetric, pageAlerts } from '@/api/index'
const balance = ref(0), metricCount = ref(0), alertCount = ref(0), userInfo = ref({})
const h = new Date().getHours()
const greeting = h < 6 ? '凌晨好' : h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const quicks = [
  { n: '基本信息', ic: '📇', c: '#2FA37C', url: '/pages/health/profileEdit' },
  { n: '体征数据', ic: '📈', c: '#37A86B', url: '/pages/health/metric' },
  { n: '就诊记录', ic: '🏥', c: '#3E8ED0', url: '/pages/health/medical' },
  { n: '健康报告', ic: '📄', c: '#E8933B', url: '/pages/health/report' },
  { n: '医生专家', ic: '👨‍⚕️', c: '#5AA9C9', url: '/pages/doctor/doctor' },
  { n: '健康咨询', ic: '💬', c: '#7C6FD6', url: '/pages/consult/consult' },
  { n: '健康预警', ic: '🔔', c: '#E5654B', url: '/pages/alert/alert' },
  { n: '积分明细', ic: '🎁', c: '#E8933B', url: '/pages/point/record' }
]
function go(url) { uni.navigateTo({ url }) }
onMounted(async () => {
  try {
    userInfo.value = uni.getStorageSync('userInfo') || {}
    balance.value = (await getPointBalance()).data
    metricCount.value = (await pageMetric({ pageNum: 1, pageSize: 1 })).data.total
    alertCount.value = (await pageAlerts({ pageNum: 1, pageSize: 1 })).data.total
  } catch (e) {}
})
</script>

<style scoped>
.banner { display: flex; flex-direction: column; }
.ring { position: absolute; right: -40rpx; top: -40rpx; width: 200rpx; height: 200rpx; border-radius: 50%;
  border: 3rpx dashed rgba(255,255,255,.35); animation: spin 18s linear infinite; }
.hi { font-size: 28rpx; opacity: .92; }
.big { font-size: 52rpx; font-weight: 800; margin: 10rpx 0; letter-spacing: -1rpx; }
.date { font-size: 26rpx; opacity: .85; }

.stats { display: flex; gap: 20rpx; margin: 28rpx 0; }
.stat { flex: 1; background: var(--card); border-radius: 24rpx; padding: 30rpx 20rpx; text-align: center;
  box-shadow: 0 6rpx 22rpx rgba(33,80,63,.07); display: flex; flex-direction: column; align-items: center; }
.s-num { font-size: 56rpx; color: var(--c); line-height: 1.1; }
.s-lb { font-size: 26rpx; color: var(--ink-soft); margin-top: 6rpx; }

.sec { display: block; font-size: 34rpx; font-weight: 700; margin: 8rpx 4rpx 18rpx; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18rpx; }
.tile { background: #fff; border-radius: 24rpx; padding: 28rpx 0; display: flex; flex-direction: column; align-items: center; gap: 12rpx;
  box-shadow: 0 6rpx 20rpx rgba(33,80,63,.06); }
.tile:active { transform: scale(.94); }
.t-ic { width: 84rpx; height: 84rpx; border-radius: 24rpx; display: flex; align-items: center; justify-content: center; font-size: 42rpx;
  background: var(--c); color: #fff; }
.t-n { font-size: 24rpx; font-weight: 600; color: var(--ink); }
</style>
