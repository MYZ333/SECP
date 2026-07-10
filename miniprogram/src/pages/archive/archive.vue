<template>
  <view class="page-pad">
    <!-- 档案模块入口 -->
    <view class="mods enter">
      <view class="mod" @click="go('/pages/health/profileEdit')" style="--c:#2FA37C"><text class="m-ic">📇</text><text class="m-n">基本信息</text></view>
      <view class="mod" @click="go('/pages/health/metric')" style="--c:#37A86B"><text class="m-ic">📈</text><text class="m-n">体征数据</text></view>
      <view class="mod" @click="go('/pages/health/medical')" style="--c:#3E8ED0"><text class="m-ic">🏥</text><text class="m-n">就诊记录</text></view>
      <view class="mod" @click="go('/pages/health/report')" style="--c:#E8933B"><text class="m-ic">📄</text><text class="m-n">健康报告</text></view>
    </view>

    <!-- 基本信息卡 -->
    <view class="card enter-1" @click="go('/pages/health/profileEdit')">
      <view class="card-head">
        <text class="h">健康基本信息</text>
        <text class="pill pill-green">点击编辑 ›</text>
      </view>
      <view class="info-grid">
        <view class="info"><text class="k muted">身高</text><text class="v num">{{ profile.height || '—' }}<text class="u"> cm</text></text></view>
        <view class="info"><text class="k muted">体重</text><text class="v num">{{ profile.weight || '—' }}<text class="u"> kg</text></text></view>
        <view class="info"><text class="k muted">血型</text><text class="v">{{ profile.bloodType || '—' }}</text></view>
        <view class="info"><text class="k muted">BMI</text><text class="v num">{{ bmi }}</text></view>
      </view>
      <view class="line" v-if="profile.allergyHistory"><text class="k muted">过敏史</text><text class="lv">{{ profile.allergyHistory }}</text></view>
    </view>

    <!-- 最近体征 -->
    <view class="card enter-2" style="margin-top:24rpx" @click="go('/pages/health/metric')">
      <view class="card-head"><text class="h">最近体征记录</text><text class="pill pill-green">全部 ›</text></view>
      <view class="metric" v-for="m in metrics" :key="m.id">
        <view class="m-dot" :style="{ background: m.abnormal ? '#E5654B' : '#2FA37C' }"></view>
        <text class="m-type">{{ typeName(m.metricType) }}</text>
        <text class="m-val num">{{ m.metricValue }}<text class="m-unit"> {{ m.unit || '' }}</text></text>
        <text class="m-time muted">{{ (m.measureTime || '').slice(5,16) }}</text>
      </view>
      <view v-if="!metrics.length" class="empty muted">暂无体征数据</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHealthProfile, pageMetric } from '@/api/index'
const profile = ref({}), metrics = ref([])
const bmi = computed(() => {
  const { height, weight } = profile.value
  if (!height || !weight) return '—'
  const h = height / 100
  return (weight / (h * h)).toFixed(1)
})
const names = { BLOOD_PRESSURE: '血压', BLOOD_SUGAR: '血糖', HEART_RATE: '心率', TEMPERATURE: '体温', WEIGHT: '体重' }
function typeName(t) { return names[t] || t }
function go(url) { uni.navigateTo({ url }) }
onMounted(async () => {
  try {
    profile.value = (await getHealthProfile()).data || {}
    metrics.value = (await pageMetric({ pageNum: 1, pageSize: 10 })).data.records
  } catch (e) {}
})
</script>

<style scoped>
.mods { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.mod { flex: 1; background: #fff; border-radius: 22rpx; padding: 26rpx 0; display: flex; flex-direction: column; align-items: center; gap: 10rpx;
  box-shadow: 0 6rpx 20rpx rgba(33,80,63,.06); }
.mod:active { transform: scale(.95); }
.m-ic { font-size: 46rpx; }
.m-n { font-size: 24rpx; font-weight: 600; color: var(--ink); }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.h { font-size: 34rpx; font-weight: 700; }
.info-grid { display: flex; flex-wrap: wrap; }
.info { width: 50%; padding: 18rpx 0; }
.k { display: block; font-size: 26rpx; }
.v { font-size: 40rpx; font-weight: 700; }
.u, .m-unit { font-size: 24rpx; color: var(--ink-soft); font-weight: 500; }
.line { margin-top: 12rpx; padding-top: 20rpx; border-top: 2rpx solid #F0EDE4; }
.lv { display: block; margin-top: 8rpx; font-size: 30rpx; }

.metric { display: flex; align-items: center; gap: 18rpx; padding: 22rpx 0; border-bottom: 2rpx solid #F4F1E9; }
.metric:last-child { border-bottom: none; }
.m-dot { width: 16rpx; height: 16rpx; border-radius: 50%; flex-shrink: 0; }
.m-type { width: 120rpx; font-size: 30rpx; font-weight: 600; }
.m-val { flex: 1; font-size: 34rpx; }
.m-time { font-size: 24rpx; }
.empty { text-align: center; padding: 50rpx 0; }
</style>
