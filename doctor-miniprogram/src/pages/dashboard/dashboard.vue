<template>
  <view class="page-pad">
    <view class="banner">
      <text class="title-lg">医生工作台</text>
      <text class="muted sub">优先处理未读咨询，及时查看患者健康记录。</text>
    </view>
    <view class="grid">
      <view class="card stat" v-for="s in cards" :key="s.label">
        <text class="num">{{ s.value }}</text>
        <text class="muted">{{ s.label }}</text>
      </view>
    </view>
  </view>
</template>
<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getStats } from '@/api'
const stats = ref({})
const cards = computed(() => [
  { label: '咨询患者', value: stats.value.patientCount || 0 },
  { label: '进行中', value: stats.value.openSessionCount || 0 },
  { label: '未读消息', value: stats.value.unreadCount || 0 },
  { label: '今日消息', value: stats.value.todayMessageCount || 0 }
])
onShow(async () => { stats.value = (await getStats()).data || {} })
</script>
<style scoped>
.banner { background: #fff; padding: 34rpx; box-shadow: 0 8rpx 28rpx rgba(28,63,120,.08); }
.sub { display: block; margin-top: 8rpx; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20rpx; margin-top: 24rpx; }
.stat .num { display: block; color: var(--primary); font-size: 52rpx; }
</style>
