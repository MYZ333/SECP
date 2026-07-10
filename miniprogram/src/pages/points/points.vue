<template>
  <view class="page-pad">
    <!-- 积分横幅 -->
    <view class="pt-banner enter">
      <view>
        <text class="pt-lb">我的可用积分</text>
        <text class="num pt-num">{{ balance }}</text>
      </view>
      <text class="pt-emoji">🎁</text>
    </view>

    <!-- 商品列表 -->
    <view class="list">
      <view class="prod card" :class="'enter-' + ((i % 4) + 1)" v-for="(p, i) in products" :key="p.id">
        <view class="p-thumb">{{ p.name.slice(0,1) }}</view>
        <view class="p-body">
          <text class="p-name">{{ p.name }}</text>
          <text class="p-desc muted">{{ p.description }}</text>
          <view class="p-foot">
            <text class="pill pill-warn">{{ p.pointsCost }} 积分</text>
            <text class="p-stock muted">库存 {{ p.stock }}</text>
          </view>
        </view>
        <button class="ex" :class="balance < p.pointsCost || p.stock < 1 ? 'off' : ''" @click="exchange(p)">
          {{ balance < p.pointsCost ? '积分不足' : '兑换' }}
        </button>
      </view>
    </view>

    <view v-if="!products.length" class="empty muted">暂无可兑换商品</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPointBalance, pagePointProducts, exchangeProduct } from '@/api/index'
const balance = ref(0), products = ref([])
async function load() {
  balance.value = (await getPointBalance()).data
  products.value = (await pagePointProducts({ pageNum: 1, pageSize: 20 })).data.records
}
function exchange(p) {
  if (balance.value < p.pointsCost || p.stock < 1) return
  uni.showModal({
    title: '兑换确认', content: `用 ${p.pointsCost} 积分兑换「${p.name}」？`,
    confirmText: '确认兑换', confirmColor: '#2FA37C',
    success: async (r) => { if (r.confirm) { await exchangeProduct({ productId: p.id, quantity: 1 }); uni.showToast({ title: '兑换成功' }); load() } }
  })
}
onMounted(load)
</script>

<style scoped>
.pt-banner {
  display: flex; justify-content: space-between; align-items: center;
  border-radius: 32rpx; padding: 40rpx; color: #fff; margin-bottom: 28rpx;
  background: linear-gradient(120deg, #F2A85E, #E8933B); box-shadow: 0 16rpx 36rpx rgba(232,147,59,.28);
}
.pt-lb { display: block; font-size: 26rpx; opacity: .92; }
.pt-num { font-size: 68rpx; line-height: 1.1; }
.pt-emoji { font-size: 84rpx; }

.list { display: flex; flex-direction: column; gap: 22rpx; }
.prod { display: flex; align-items: center; gap: 22rpx; padding: 26rpx; }
.p-thumb {
  width: 110rpx; height: 110rpx; border-radius: 22rpx; flex-shrink: 0; color: #fff; font-size: 44rpx; font-weight: 800;
  display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#7BC6A6,#2FA37C);
}
.p-body { flex: 1; min-width: 0; }
.p-name { display: block; font-size: 32rpx; font-weight: 700; }
.p-desc { display: block; font-size: 24rpx; margin: 6rpx 0 12rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-foot { display: flex; align-items: center; gap: 16rpx; }
.p-stock { font-size: 24rpx; }
.ex {
  flex-shrink: 0; height: 68rpx; line-height: 68rpx; padding: 0 30rpx; font-size: 28rpx; font-weight: 600;
  color: #fff; border-radius: 999rpx; background: linear-gradient(135deg,#37B389,#279470); box-shadow: 0 8rpx 18rpx rgba(47,163,124,.3);
}
.ex.off { background: #D6DBD7; color: #fff; box-shadow: none; }
.empty { text-align: center; padding: 80rpx 0; }
</style>
