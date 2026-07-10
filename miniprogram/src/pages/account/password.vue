<template>
  <view class="page-pad">
    <view class="card enter">
      <view class="fld"><text class="lb">原密码</text><input class="ipt" password v-model="form.oldPassword" placeholder="请输入原密码" /></view>
      <view class="fld"><text class="lb">新密码</text><input class="ipt" password v-model="form.newPassword" placeholder="6-32 位新密码" /></view>
      <view class="fld"><text class="lb">确认密码</text><input class="ipt" password v-model="confirm" placeholder="再次输入新密码" /></view>
    </view>
    <button class="btn-primary save enter-2" @click="save">确认修改</button>
    <text class="tip muted enter-2">修改成功后需重新登录</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { changePassword } from '@/api/index'
const form = ref({ oldPassword: '', newPassword: '' })
const confirm = ref('')
async function save() {
  if (!form.value.oldPassword || !form.value.newPassword) return uni.showToast({ title: '请填写完整', icon: 'none' })
  if (form.value.newPassword !== confirm.value) return uni.showToast({ title: '两次新密码不一致', icon: 'none' })
  await changePassword(form.value)
  uni.showToast({ title: '修改成功' })
  setTimeout(() => {
    uni.removeStorageSync('token'); uni.removeStorageSync('userInfo')
    uni.reLaunch({ url: '/pages/login/login' })
  }, 800)
}
</script>

<style scoped>
.fld { display: flex; align-items: center; gap: 20rpx; padding: 26rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.fld:last-child { border-bottom: none; }
.lb { width: 160rpx; font-size: 30rpx; color: var(--ink-soft); }
.ipt { flex: 1; height: 72rpx; font-size: 32rpx; text-align: right; }
.save { width: 100%; height: 96rpx; line-height: 96rpx; margin-top: 32rpx; letter-spacing: 4rpx; }
.tip { display: block; text-align: center; margin-top: 20rpx; font-size: 24rpx; }
</style>
