<template>
  <view class="page-pad">
    <view class="card">
      <text class="title-lg">{{ detail.patient?.nickname || detail.patient?.username || '患者' }}</text>
      <text class="muted info">{{ genderText(detail.patient?.gender) }} · {{ detail.age || '年龄未知' }}岁</text>
    </view>
    <view class="card sec">
      <text class="h">健康档案</text>
      <text class="row">身高：{{ detail.profile?.height || '-' }} cm</text>
      <text class="row">体重：{{ detail.profile?.weight || '-' }} kg</text>
      <text class="row">血型：{{ detail.profile?.bloodType || '-' }}</text>
      <text class="row">既往史：{{ detail.profile?.pastHistory || '-' }}</text>
    </view>
    <view class="card sec">
      <text class="h">近期体征</text>
      <text class="row" v-for="m in detail.metrics || []" :key="m.id">{{ m.metricType }}：{{ m.metricValue }} {{ m.unit || '' }}</text>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getPatientDetail } from '@/api'
const detail = ref({})
function genderText(v) { return v === 1 ? '男' : v === 2 ? '女' : '保密' }
onLoad(async (q) => { detail.value = (await getPatientDetail(q.id)).data || {} })
</script>
<style scoped>
.info { display: block; margin-top: 8rpx; }
.sec { margin-top: 20rpx; }
.h { display: block; font-weight: 900; margin-bottom: 14rpx; }
.row { display: block; padding: 8rpx 0; color: var(--ink-soft); }
</style>
