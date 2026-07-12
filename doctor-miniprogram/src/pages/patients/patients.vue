<template>
  <view class="page-pad">
    <view class="card patient" v-for="p in list" :key="p.id" @click="go(p.id)">
      <text class="name">{{ p.nickname || p.username }}</text>
      <text class="muted">{{ genderText(p.gender) }} · {{ p.birthday || '生日未知' }}</text>
    </view>
    <view v-if="!list.length" class="empty muted">暂无咨询患者</view>
  </view>
</template>
<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { pagePatients } from '@/api'
const list = ref([])
function genderText(v) { return v === 1 ? '男' : v === 2 ? '女' : '保密' }
function go(id) { uni.navigateTo({ url: '/pages/patients/detail?id=' + id }) }
onShow(async () => { list.value = ((await pagePatients({ pageNum: 1, pageSize: 50 })).data.records || []) })
</script>
<style scoped>
.patient { margin-bottom: 18rpx; }
.name { display: block; font-size: 34rpx; font-weight: 800; }
.empty { text-align: center; margin-top: 120rpx; }
</style>
