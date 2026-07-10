<template>
  <view class="page-pad">
    <button class="btn-primary add enter" @click="openAdd">＋ 新增就诊记录</button>

    <view class="list">
      <view class="card item" :class="'enter-' + ((i % 4) + 1)" v-for="(m, i) in list" :key="m.id">
        <view class="row1">
          <text class="hosp">{{ m.hospital || '未填医院' }}</text>
          <text class="date muted">{{ m.visitDate || '' }}</text>
        </view>
        <view class="tags">
          <text v-if="m.department" class="pill pill-green">{{ m.department }}</text>
          <text v-if="m.doctorName" class="muted dm">{{ m.doctorName }} 医生</text>
        </view>
        <text v-if="m.diagnosis" class="diag">诊断：{{ m.diagnosis }}</text>
        <text v-if="m.prescription" class="pres muted">用药：{{ m.prescription }}</text>
        <text class="del" @click="remove(m.id)">删除</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有就诊记录</view>

    <view v-if="show" class="mask" @click="show=false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">新增就诊记录</text>
        <view class="fld"><text class="lb">就诊日期</text>
          <picker mode="date" :value="form.visitDate" @change="e => form.visitDate = e.detail.value">
            <text class="pkv">{{ form.visitDate || '请选择' }}</text>
          </picker>
        </view>
        <view class="fld"><text class="lb">医院</text><input class="ipt" v-model="form.hospital" placeholder="就诊医院" /></view>
        <view class="fld"><text class="lb">科室</text><input class="ipt" v-model="form.department" placeholder="科室" /></view>
        <view class="fld"><text class="lb">医生</text><input class="ipt" v-model="form.doctorName" placeholder="医生姓名" /></view>
        <view class="fld col"><text class="lb">诊断</text><textarea class="ta" v-model="form.diagnosis" placeholder="诊断结果" /></view>
        <view class="fld col"><text class="lb">处方/用药</text><textarea class="ta" v-model="form.prescription" placeholder="用药情况" /></view>
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
import { pageMedical, createMedical, deleteMedical } from '@/api/index'
const list = ref([]), show = ref(false), form = ref({})
async function load() { list.value = (await pageMedical({ pageNum: 1, pageSize: 50 })).data.records }
function openAdd() { form.value = { visitDate: new Date().toISOString().slice(0, 10) }; show.value = true }
async function save() { await createMedical(form.value); uni.showToast({ title: '已保存' }); show.value = false; load() }
function remove(id) {
  uni.showModal({ title: '提示', content: '确认删除？', confirmColor: '#E5654B',
    success: async r => { if (r.confirm) { await deleteMedical(id); load() } } })
}
onMounted(load)
</script>

<style scoped>
.add { width: 100%; height: 92rpx; line-height: 92rpx; margin-bottom: 26rpx; }
.list { display: flex; flex-direction: column; gap: 18rpx; }
.item { position: relative; padding: 28rpx; }
.row1 { display: flex; justify-content: space-between; align-items: baseline; }
.hosp { font-size: 34rpx; font-weight: 700; }
.date { font-size: 24rpx; }
.tags { display: flex; align-items: center; gap: 16rpx; margin: 14rpx 0; }
.dm { font-size: 26rpx; }
.diag { display: block; font-size: 30rpx; margin-top: 6rpx; }
.pres { display: block; font-size: 28rpx; margin-top: 8rpx; }
.del { position: absolute; right: 26rpx; bottom: 24rpx; color: #E5654B; font-size: 26rpx; }
.empty { text-align: center; padding: 80rpx 0; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: flex-end; z-index: 20; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx; max-height: 88vh; overflow-y: auto; animation: fadeUp .3s ease both; }
.sheet-title { display: block; font-size: 36rpx; font-weight: 800; margin-bottom: 20rpx; }
.fld { display: flex; align-items: center; gap: 20rpx; padding: 22rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.fld.col { flex-direction: column; align-items: stretch; gap: 12rpx; }
.lb { width: 160rpx; font-size: 30rpx; color: var(--ink-soft); }
.ipt { flex: 1; height: 70rpx; font-size: 32rpx; text-align: right; }
.pkv { font-size: 32rpx; }
.ta { width: 100%; min-height: 110rpx; font-size: 30rpx; background: #F7F5EF; border-radius: 16rpx; padding: 18rpx; box-sizing: border-box; }
.sheet-btns { display: flex; gap: 20rpx; margin-top: 30rpx; }
.half { flex: 1; height: 90rpx; line-height: 90rpx; }
</style>
