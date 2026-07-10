<template>
  <view class="page-pad">
    <button class="btn-primary add enter" @click="openAdd">＋ 新增健康报告</button>

    <view class="list">
      <view class="card item" :class="'enter-' + ((i % 4) + 1)" v-for="(r, i) in list" :key="r.id">
        <view class="row1">
          <text class="t">{{ r.title }}</text>
          <text class="pill pill-green">{{ typeName(r.reportType) }}</text>
        </view>
        <text class="date muted">{{ r.reportDate || '' }}</text>
        <text v-if="r.content" class="content">{{ r.content }}</text>
        <text class="del" @click="remove(r.id)">删除</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有健康报告</view>

    <view v-if="show" class="mask" @click="show=false">
      <view class="sheet" @click.stop>
        <text class="sheet-title">新增健康报告</text>
        <view class="fld"><text class="lb">标题</text><input class="ipt" v-model="form.title" placeholder="报告标题" /></view>
        <view class="fld"><text class="lb">类型</text>
          <picker class="pk" :range="types.map(t=>t.n)" @change="e => form.reportType = types[e.detail.value].v">
            <text>{{ typeName(form.reportType) }}</text>
          </picker>
        </view>
        <view class="fld"><text class="lb">报告日期</text>
          <picker mode="date" :value="form.reportDate" @change="e => form.reportDate = e.detail.value">
            <text class="pkv">{{ form.reportDate || '请选择' }}</text>
          </picker>
        </view>
        <view class="fld col"><text class="lb">内容/结论</text><textarea class="ta" v-model="form.content" placeholder="报告内容或结论" /></view>
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
import { pageReport, createReport, deleteReport } from '@/api/index'
const types = [{ n: '体检报告', v: 'PHYSICAL' }, { n: 'AI生成', v: 'AI' }, { n: '其他', v: 'OTHER' }]
const list = ref([]), show = ref(false), form = ref({})
function typeName(v) { const t = types.find(t => t.v === v); return t ? t.n : '其他' }
async function load() { list.value = (await pageReport({ pageNum: 1, pageSize: 50 })).data.records }
function openAdd() { form.value = { reportType: 'OTHER', reportDate: new Date().toISOString().slice(0, 10) }; show.value = true }
async function save() {
  if (!form.value.title) return uni.showToast({ title: '请填标题', icon: 'none' })
  await createReport(form.value); uni.showToast({ title: '已保存' }); show.value = false; load()
}
function remove(id) {
  uni.showModal({ title: '提示', content: '确认删除？', confirmColor: '#E5654B',
    success: async r => { if (r.confirm) { await deleteReport(id); load() } } })
}
onMounted(load)
</script>

<style scoped>
.add { width: 100%; height: 92rpx; line-height: 92rpx; margin-bottom: 26rpx; }
.list { display: flex; flex-direction: column; gap: 18rpx; }
.item { position: relative; padding: 28rpx; }
.row1 { display: flex; justify-content: space-between; align-items: center; }
.t { font-size: 34rpx; font-weight: 700; flex: 1; }
.date { display: block; font-size: 24rpx; margin: 10rpx 0; }
.content { display: block; font-size: 29rpx; color: var(--ink-soft); line-height: 1.6; }
.del { position: absolute; right: 26rpx; bottom: 24rpx; color: #E5654B; font-size: 26rpx; }
.empty { text-align: center; padding: 80rpx 0; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: flex-end; z-index: 20; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx; animation: fadeUp .3s ease both; }
.sheet-title { display: block; font-size: 36rpx; font-weight: 800; margin-bottom: 20rpx; }
.fld { display: flex; align-items: center; gap: 20rpx; padding: 22rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.fld.col { flex-direction: column; align-items: stretch; gap: 12rpx; }
.lb { width: 160rpx; font-size: 30rpx; color: var(--ink-soft); }
.ipt { flex: 1; height: 70rpx; font-size: 32rpx; text-align: right; }
.pk { flex: 1; text-align: right; font-size: 32rpx; }
.pkv { font-size: 32rpx; }
.ta { width: 100%; min-height: 130rpx; font-size: 30rpx; background: #F7F5EF; border-radius: 16rpx; padding: 18rpx; box-sizing: border-box; }
.sheet-btns { display: flex; gap: 20rpx; margin-top: 30rpx; }
.half { flex: 1; height: 90rpx; line-height: 90rpx; }
</style>
