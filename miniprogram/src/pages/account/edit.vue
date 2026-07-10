<template>
  <view class="page-pad">
    <view class="card enter">
      <view class="fld"><text class="lb">昵称</text><input class="ipt" v-model="form.nickname" placeholder="昵称" /></view>
      <view class="fld"><text class="lb">手机号</text><input class="ipt" type="number" v-model="form.phone" placeholder="手机号" /></view>
      <view class="fld"><text class="lb">性别</text>
        <picker class="pk" :range="genders" @change="e => form.gender = e.detail.value">
          <text>{{ genders[form.gender || 0] }}</text>
        </picker>
      </view>
      <view class="fld"><text class="lb">生日</text>
        <picker mode="date" :value="form.birthday" @change="e => form.birthday = e.detail.value">
          <text class="pkv">{{ form.birthday || '请选择' }}</text>
        </picker>
      </view>
    </view>
    <button class="btn-primary save enter-2" @click="save">保 存</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMe, updateProfile } from '@/api/index'
const genders = ['未知', '男', '女']
const form = ref({ gender: 0 })
onMounted(async () => { try { const u = (await getMe()).data; form.value = { nickname: u.nickname, phone: u.phone, gender: u.gender || 0, birthday: u.birthday } } catch (e) {} })
async function save() {
  await updateProfile(form.value)
  const info = uni.getStorageSync('userInfo') || {}
  info.nickname = form.value.nickname; uni.setStorageSync('userInfo', info)
  uni.showToast({ title: '保存成功' }); setTimeout(() => uni.navigateBack(), 600)
}
</script>

<style scoped>
.fld { display: flex; align-items: center; gap: 20rpx; padding: 26rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.fld:last-child { border-bottom: none; }
.lb { width: 150rpx; font-size: 30rpx; color: var(--ink-soft); }
.ipt { flex: 1; height: 72rpx; font-size: 32rpx; text-align: right; }
.pk, .pkv { flex: 1; text-align: right; font-size: 32rpx; }
.save { width: 100%; height: 96rpx; line-height: 96rpx; margin-top: 32rpx; letter-spacing: 8rpx; }
</style>
