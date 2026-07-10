<template>
  <view class="page-pad">
    <view class="search enter">
      <text class="s-ic">🔍</text>
      <input class="s-ipt" v-model="keyword" placeholder="搜索医生姓名" @confirm="reload" />
      <text class="s-btn" @click="reload">搜索</text>
    </view>

    <view class="list">
      <view class="card doc" :class="'enter-' + ((i % 4) + 1)" v-for="(d, i) in list" :key="d.id">
        <view class="top">
          <view class="ava">{{ d.name.slice(0,1) }}</view>
          <view class="meta">
            <view class="nr"><text class="name">{{ d.name }}</text><text class="pill pill-green">{{ d.title }}</text></view>
            <text class="place muted">{{ d.hospital }} · {{ d.department }}</text>
          </view>
        </view>
        <text class="spec"><text class="k">擅长</text>{{ d.speciality || '—' }}</text>
        <text class="intro muted">{{ d.introduction }}</text>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">没有找到医生</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { pageDoctors } from '@/api/index'
const list = ref([]), keyword = ref('')
async function load() { list.value = (await pageDoctors({ pageNum: 1, pageSize: 30, keyword: keyword.value })).data.records }
function reload() { load() }
onMounted(load)
</script>

<style scoped>
.search { display: flex; align-items: center; gap: 14rpx; background: #fff; border-radius: 999rpx; padding: 12rpx 24rpx; margin-bottom: 26rpx; box-shadow: 0 6rpx 20rpx rgba(33,80,63,.06); }
.s-ic { font-size: 32rpx; }
.s-ipt { flex: 1; height: 68rpx; font-size: 30rpx; }
.s-btn { color: var(--primary); font-weight: 700; font-size: 30rpx; padding-left: 12rpx; }
.list { display: flex; flex-direction: column; gap: 20rpx; }
.doc { padding: 30rpx; border-top: 6rpx solid var(--primary); }
.top { display: flex; gap: 20rpx; align-items: center; }
.ava { width: 96rpx; height: 96rpx; border-radius: 28rpx; flex-shrink: 0; color: #fff; font-size: 44rpx; font-weight: 800; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg,#7BC6A6,#2FA37C); }
.nr { display: flex; align-items: center; gap: 14rpx; }
.name { font-size: 36rpx; font-weight: 800; }
.place { display: block; font-size: 26rpx; margin-top: 6rpx; }
.spec { display: block; margin: 20rpx 0 10rpx; font-size: 28rpx; }
.k { display: inline-block; background: #EAF6F2; color: var(--primary); font-weight: 600; padding: 4rpx 16rpx; border-radius: 10rpx; margin-right: 14rpx; font-size: 24rpx; }
.intro { display: block; font-size: 27rpx; line-height: 1.6; }
.empty { text-align: center; padding: 80rpx 0; }
</style>
