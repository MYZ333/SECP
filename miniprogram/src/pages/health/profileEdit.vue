<template>
  <view class="page-pad">
    <view class="card enter">
      <view class="fld"><text class="lb">身高 (cm)</text><input class="ipt" type="digit" v-model="form.height" placeholder="如 170" /></view>
      <view class="fld"><text class="lb">体重 (kg)</text><input class="ipt" type="digit" v-model="form.weight" placeholder="如 65" /></view>
      <view class="fld">
        <text class="lb">血型</text>
        <picker class="ipt pk" :range="bloods" @change="e => form.bloodType = bloods[e.detail.value]">
          <text>{{ form.bloodType || '请选择' }}</text>
        </picker>
      </view>
      <view class="fld col"><text class="lb">过敏史</text><textarea class="ta" v-model="form.allergyHistory" placeholder="如无请留空" /></view>
      <view class="fld col"><text class="lb">家族病史</text><textarea class="ta" v-model="form.familyHistory" placeholder="如无请留空" /></view>
      <view class="fld col"><text class="lb">既往病史</text><textarea class="ta" v-model="form.pastHistory" placeholder="如无请留空" /></view>
      <view class="fld"><text class="lb">紧急联系人</text><input class="ipt" v-model="form.emergencyContact" placeholder="姓名" /></view>
      <view class="fld"><text class="lb">紧急电话</text><input class="ipt" type="number" v-model="form.emergencyPhone" placeholder="电话" /></view>
    </view>
    <button class="btn-primary save enter-2" @click="save">保 存</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHealthProfile, saveHealthProfile } from '@/api/index'
const bloods = ['A', 'B', 'O', 'AB', '未知']
const form = ref({})
onMounted(async () => { try { const r = await getHealthProfile(); if (r.data) form.value = r.data } catch (e) {} })
async function save() {
  await saveHealthProfile(form.value)
  uni.showToast({ title: '保存成功' })
  setTimeout(() => uni.navigateBack(), 600)
}
</script>

<style scoped>
.fld { display: flex; align-items: center; gap: 20rpx; padding: 24rpx 0; border-bottom: 2rpx solid #F2EFE7; }
.fld.col { flex-direction: column; align-items: stretch; gap: 12rpx; }
.fld:last-child { border-bottom: none; }
.lb { width: 160rpx; font-size: 30rpx; color: var(--ink-soft); flex-shrink: 0; }
.ipt { flex: 1; height: 76rpx; font-size: 32rpx; text-align: right; }
.pk { display: flex; align-items: center; justify-content: flex-end; }
.ta { width: 100%; min-height: 120rpx; font-size: 30rpx; background: #F7F5EF; border-radius: 16rpx; padding: 18rpx; box-sizing: border-box; }
.save { width: 100%; height: 96rpx; line-height: 96rpx; margin-top: 32rpx; letter-spacing: 8rpx; }
</style>
