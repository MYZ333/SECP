<template>
  <view class="page-pad">
    <!-- 用户头部 -->
    <view class="profile enter">
      <view class="avatar">{{ (user.nickname || user.username || 'U').slice(0,1) }}</view>
      <view class="p-meta">
        <text class="p-name">{{ user.nickname || user.username }}</text>
        <text class="p-role">{{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}</text>
      </view>
      <view class="p-points">
        <text class="num pp-num">{{ user.points || 0 }}</text>
        <text class="pp-lb">积分</text>
      </view>
    </view>

    <!-- 资料卡 -->
    <view class="card enter-2">
      <view class="row"><text class="k muted">用户名</text><text class="v">{{ user.username }}</text></view>
      <view class="row"><text class="k muted">昵称</text><text class="v">{{ user.nickname || '—' }}</text></view>
      <view class="row"><text class="k muted">手机号</text><text class="v">{{ user.phone || '—' }}</text></view>
      <view class="row last"><text class="k muted">性别</text><text class="v">{{ ['未知','男','女'][user.gender || 0] }}</text></view>
    </view>

    <!-- 功能菜单 -->
    <view class="menu card enter-3">
      <view class="mrow" v-for="(m, i) in menus" :key="m.url" :class="{ last: i === menus.length - 1 }" @click="go(m.url)">
        <text class="mi">{{ m.ic }}</text><text class="mn">{{ m.n }}</text><text class="ma">›</text>
      </view>
    </view>

    <button class="logout enter-4" @click="logout">退出登录</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMe } from '@/api/index'
const user = ref({})
const menus = [
  { n: '编辑资料', ic: '✏️', url: '/pages/account/edit' },
  { n: '修改密码', ic: '🔑', url: '/pages/account/password' },
  { n: '健康预警', ic: '🔔', url: '/pages/alert/alert' },
  { n: '健康咨询', ic: '💬', url: '/pages/consult/consult' },
  { n: '医生专家库', ic: '👨‍⚕️', url: '/pages/doctor/doctor' },
  { n: '积分明细', ic: '🎁', url: '/pages/point/record' }
]
function go(url) { uni.navigateTo({ url }) }
onMounted(async () => { try { user.value = (await getMe()).data } catch (e) {} })
function logout() {
  uni.showModal({
    title: '提示', content: '确认退出登录？', confirmColor: '#E5654B',
    success: (r) => { if (r.confirm) { uni.removeStorageSync('token'); uni.removeStorageSync('userInfo'); uni.reLaunch({ url: '/pages/login/login' }) } }
  })
}
</script>

<style scoped>
.profile {
  display: flex; align-items: center; gap: 24rpx; padding: 40rpx; border-radius: 32rpx; color: #fff; margin-bottom: 28rpx;
  background: linear-gradient(135deg, #37B389 0%, #2FA37C 55%, #23856A 100%); box-shadow: 0 16rpx 40rpx rgba(35,133,106,.28);
}
.avatar { width: 120rpx; height: 120rpx; border-radius: 36rpx; background: rgba(255,255,255,.22);
  display: flex; align-items: center; justify-content: center; font-size: 56rpx; font-weight: 800; }
.p-meta { flex: 1; display: flex; flex-direction: column; }
.p-name { font-size: 40rpx; font-weight: 800; }
.p-role { font-size: 26rpx; opacity: .9; margin-top: 6rpx; }
.p-points { text-align: center; }
.pp-num { display: block; font-size: 48rpx; }
.pp-lb { font-size: 24rpx; opacity: .9; }

.card { padding: 8rpx 32rpx; }
.row { display: flex; justify-content: space-between; align-items: center; padding: 30rpx 0; border-bottom: 2rpx solid #F0EDE4; }
.row.last { border-bottom: none; }
.k { font-size: 30rpx; }
.v { font-size: 32rpx; font-weight: 600; }

.menu { padding: 8rpx 28rpx; }
.mrow { display: flex; align-items: center; gap: 20rpx; padding: 30rpx 0; border-bottom: 2rpx solid #F0EDE4; }
.mrow.last { border-bottom: none; }
.mrow:active { opacity: .6; }
.mi { font-size: 38rpx; }
.mn { flex: 1; font-size: 32rpx; font-weight: 600; }
.ma { font-size: 42rpx; color: #C7CFC9; }
.logout { margin-top: 30rpx; height: 96rpx; line-height: 96rpx; border-radius: 999rpx; font-size: 32rpx; font-weight: 600;
  color: #E5654B; background: #fff; box-shadow: 0 6rpx 20rpx rgba(33,80,63,.06); }
.logout:active { opacity: .7; }
</style>
