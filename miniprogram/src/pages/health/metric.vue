<template>
  <view class="page-pad">
    <button class="btn-primary add enter" @click="openAdd">＋ 新增体征记录</button>

    <view class="list">
      <view class="card item" :class="'enter-' + ((i % 4) + 1)" v-for="(m, i) in list" :key="m.id">
        <view class="dot" :style="{ background: m.abnormal ? '#E5654B' : '#2FA37C' }"></view>
        <view class="mid">
          <text class="t">{{ typeName(m.metricType) }}</text>
          <text class="time muted">{{ (m.measureTime || '').replace('T',' ').slice(0,16) }}</text>
        </view>
        <text class="val num">{{ m.metricValue }}<text v-if="m.metricValue2">/{{ m.metricValue2 }}</text><text class="u"> {{ m.unit }}</text></text>
        <text class="del" @click="remove(m.id)">🗑</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有体征记录，点上方按钮新增</view>

    <!-- 新增弹层 -->
    <view v-if="show" class="mask" @click="show=false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">新增体征记录</text>
        <view class="fld"><text class="lb">类型</text>
          <picker class="pk" :range="types.map(t=>t.n)" @change="e => form.metricType = types[e.detail.value].v">
            <text>{{ typeName(form.metricType) || '请选择' }}</text>
          </picker>
        </view>
        <view class="fld"><text class="lb">数值</text><input class="ipt" type="digit" v-model="form.metricValue" placeholder="如 120" /></view>
        <view class="fld"><text class="lb">第二值</text><input class="ipt" type="digit" v-model="form.metricValue2" placeholder="血压舒张压，选填" /></view>
        <view class="fld"><text class="lb">单位</text><input class="ipt" v-model="form.unit" placeholder="mmHg / mmol/L …" /></view>
        <view class="fld"><text class="lb">异常</text><switch :checked="form.abnormal===1" @change="e => form.abnormal = e.detail.value ? 1 : 0" color="#2FA37C" /></view>
        <view class="sheet-btns">
          <button class="btn-ghost half" @click="show=false">取消</button>
          <button class="btn-primary half" @click="save">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageMetric, createMetric, deleteMetric } from '@/api/index'
const types = [
  { n: '血压', v: 'BLOOD_PRESSURE' }, { n: '血糖', v: 'BLOOD_SUGAR' },
  { n: '心率', v: 'HEART_RATE' }, { n: '体温', v: 'TEMPERATURE' }, { n: '体重', v: 'WEIGHT' }
]
const list = ref([]), show = ref(false), form = ref({})
function typeName(v) { const t = types.find(t => t.v === v); return t ? t.n : (v || '') }
async function load() { list.value = (await pageMetric({ pageNum: 1, pageSize: 50 })).data.records }
function openAdd() {
  form.value = { abnormal: 0, measureTime: new Date().toISOString().slice(0, 19) }
  show.value = true
}
async function save() {
  if (!form.value.metricType || !form.value.metricValue) return uni.showToast({ title: '请选类型并填数值', icon: 'none' })
  await createMetric(form.value); uni.showToast({ title: '已保存' }); show.value = false; load()
}
function remove(id) {
  uni.showModal({ title: '提示', content: '确认删除这条记录？', confirmColor: '#E5654B',
    success: async r => { if (r.confirm) { await deleteMetric(id); load() } } })
}
onMounted(load)
</script>

<style scoped>
.add { width: 100%; height: 92rpx; line-height: 92rpx; margin-bottom: 26rpx; }
.list { display: flex; flex-direction: column; gap: 18rpx; }
.item { display: flex; align-items: center; gap: 18rpx; padding: 28rpx; }
.dot { width: 18rpx; height: 18rpx; border-radius: 50%; flex-shrink: 0; }
.mid { flex: 1; display: flex; flex-direction: column; }
.t { font-size: 32rpx; font-weight: 700; }
.time { font-size: 22rpx; margin-top: 4rpx; }
.val { font-size: 36rpx; color: var(--ink); }
.u { font-size: 22rpx; color: var(--ink-soft); font-weight: 500; }
.del { font-size: 34rpx; padding-left: 10rpx; }
.empty { text-align: center; padding: 80rpx 0; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: flex-end; z-index: 20; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx; animation: fadeUp .3s ease both; }
.sheet-title { display: block; font-size: 36rpx; font-weight: 800; margin-bottom: 20rpx; }
.fld { display: flex; align-items: center; gap: 20rpx; padding: 22rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.lb { width: 130rpx; font-size: 30rpx; color: var(--ink-soft); }
.ipt { flex: 1; height: 70rpx; font-size: 32rpx; text-align: right; }
.pk { flex: 1; text-align: right; font-size: 32rpx; }
.sheet-btns { display: flex; gap: 20rpx; margin-top: 30rpx; }
.half { flex: 1; height: 90rpx; line-height: 90rpx; }
</style>
