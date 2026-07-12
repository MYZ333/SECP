<template>
  <view class="page-pad">
    <view class="card">
      <input class="ipt" v-model="form.name" placeholder="姓名" />
      <input class="ipt" v-model="form.phone" placeholder="手机号" />
      <input class="ipt" v-model="form.hospital" placeholder="医院" />
      <input class="ipt" v-model="form.department" placeholder="科室" />
      <input class="ipt" v-model="form.title" placeholder="职称" />
      <input class="ipt" v-model="form.speciality" placeholder="擅长" />
      <textarea class="area" v-model="form.introduction" placeholder="简介" />
      <button class="btn-primary" @click="save">保存资料</button>
      <button class="btn-ghost logout" @click="logout">退出登录</button>
    </view>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMe, updateMe } from '@/api'
const form = ref({})
async function save() { await updateMe(form.value); uni.showToast({ title: '保存成功', icon: 'none' }) }
function logout() { uni.removeStorageSync('doctorToken'); uni.reLaunch({ url: '/pages/login/login' }) }
onShow(async () => { form.value = (await getMe()).data || {} })
</script>
<style scoped>
.ipt { height: 86rpx; padding: 0 20rpx; margin-bottom: 18rpx; background: #F7FAFD; border: 2rpx solid var(--line); }
.area { width: 100%; min-height: 180rpx; padding: 20rpx; margin-bottom: 18rpx; background: #F7FAFD; border: 2rpx solid var(--line); box-sizing: border-box; }
.logout { margin-top: 18rpx; }
</style>
