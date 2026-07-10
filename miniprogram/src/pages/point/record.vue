<template>
  <view class="page-pad">
    <view class="tabs card enter">
      <text :class="['tab', tab==='record'?'on':'']" @click="tab='record'">积分明细</text>
      <text :class="['tab', tab==='exchange'?'on':'']" @click="tab='exchange'">兑换记录</text>
    </view>

    <view v-if="tab==='record'" class="list">
      <view class="card row enter-1" v-for="r in records" :key="r.id">
        <view class="mid">
          <text class="d">{{ r.description || r.type }}</text>
          <text class="time muted">{{ (r.createTime || '').replace('T',' ').slice(0,16) }}</text>
        </view>
        <text class="chg num" :style="{ color: r.changePoints >= 0 ? '#37A86B' : '#E5654B' }">
          {{ r.changePoints >= 0 ? '+' : '' }}{{ r.changePoints }}
        </text>
      </view>
      <view v-if="!records.length" class="empty muted">暂无积分明细</view>
    </view>

    <view v-else class="list">
      <view class="card row enter-1" v-for="e in exchanges" :key="e.id">
        <view class="mid">
          <text class="d">{{ e.productName }} ×{{ e.quantity }}</text>
          <text class="time muted">{{ (e.createTime || '').replace('T',' ').slice(0,16) }}</text>
        </view>
        <view class="rt">
          <text class="chg num" style="color:#E5654B">-{{ e.pointsCost }}</text>
          <text class="st pill pill-green">{{ ['待发货','已发货','已完成','已取消'][e.status] }}</text>
        </view>
      </view>
      <view v-if="!exchanges.length" class="empty muted">暂无兑换记录</view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pagePointRecords, pageMyExchanges } from '@/api/index'
const tab = ref('record'), records = ref([]), exchanges = ref([])
onMounted(async () => {
  try {
    records.value = (await pagePointRecords({ pageNum: 1, pageSize: 50 })).data.records
    exchanges.value = (await pageMyExchanges({ pageNum: 1, pageSize: 50 })).data.records
  } catch (e) {}
})
</script>

<style scoped>
.tabs { display: flex; padding: 10rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx 0; font-size: 32rpx; font-weight: 700; color: var(--ink-soft); border-radius: 18rpx; }
.tab.on { background: var(--primary); color: #fff; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.row { display: flex; align-items: center; justify-content: space-between; padding: 26rpx 28rpx; }
.mid { display: flex; flex-direction: column; flex: 1; min-width: 0; }
.d { font-size: 31rpx; font-weight: 600; }
.time { font-size: 22rpx; margin-top: 4rpx; }
.chg { font-size: 38rpx; }
.rt { display: flex; flex-direction: column; align-items: flex-end; gap: 8rpx; }
.st { font-size: 22rpx; }
.empty { text-align: center; padding: 80rpx 0; }
</style>
