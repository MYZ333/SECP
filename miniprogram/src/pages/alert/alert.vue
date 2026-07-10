<template>
  <view class="page-pad">
    <button class="btn-primary gen enter" @click="generate">🔎 生成健康预警（AI 分析）</button>

    <view class="list">
      <view class="card item" :class="'enter-' + ((i % 4) + 1)" v-for="(a, i) in list" :key="a.id" @click="markRead(a)">
        <view class="top">
          <text class="lv" :style="{ background: lvBg(a.level), color: lvColor(a.level) }">{{ lvName(a.level) }}</text>
          <text class="type">{{ a.alertType }}</text>
          <text v-if="!a.readFlag" class="dot"></text>
        </view>
        <text class="content">{{ a.content }}</text>
        <text class="time muted">{{ (a.createTime || '').replace('T',' ').slice(0,16) }}</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">暂无健康预警</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageAlerts, generateAlert, readAlert } from '@/api/index'
const list = ref([])
const lvName = l => ({ LOW: '低', MEDIUM: '中', HIGH: '高' }[l] || '低')
const lvBg = l => ({ LOW: '#EAF2FB', MEDIUM: '#FDF0E2', HIGH: '#FBE7E2' }[l] || '#EAF2FB')
const lvColor = l => ({ LOW: '#3E8ED0', MEDIUM: '#E8933B', HIGH: '#E5654B' }[l] || '#3E8ED0')
async function load() { list.value = (await pageAlerts({ pageNum: 1, pageSize: 50 })).data.records }
async function generate() {
  uni.showLoading({ title: '分析中…' })
  try { await generateAlert(); uni.hideLoading(); uni.showToast({ title: '已生成' }); load() }
  catch (e) { uni.hideLoading() }
}
async function markRead(a) { if (!a.readFlag) { await readAlert(a.id); a.readFlag = 1 } }
onMounted(load)
</script>

<style scoped>
.gen { width: 100%; height: 92rpx; line-height: 92rpx; margin-bottom: 26rpx; }
.list { display: flex; flex-direction: column; gap: 18rpx; }
.item { padding: 28rpx; }
.top { display: flex; align-items: center; gap: 16rpx; margin-bottom: 14rpx; }
.lv { font-size: 24rpx; font-weight: 700; padding: 6rpx 18rpx; border-radius: 999rpx; }
.type { font-size: 30rpx; font-weight: 700; flex: 1; }
.dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #E5654B; }
.content { display: block; font-size: 30rpx; line-height: 1.6; color: var(--ink); }
.time { display: block; font-size: 22rpx; margin-top: 12rpx; }
.empty { text-align: center; padding: 80rpx 0; }
</style>
