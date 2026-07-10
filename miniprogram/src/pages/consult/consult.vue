<template>
  <view class="chat">
    <!-- 顶栏：品牌 + 更多功能入口 -->
    <view class="topbar">
      <view class="tb-left">
        <view class="tb-orb">✚</view>
        <view class="tb-meta">
          <text class="tb-name">健康助手</text>
          <text class="tb-sub"><text class="live"></text>在线 · 有问必答</text>
        </view>
      </view>
      <view class="more" @click="goHome">
        <text class="more-ic">▤</text><text class="more-t">更多功能</text>
      </view>
    </view>

    <scroll-view class="msgs" scroll-y :scroll-top="scrollTop" :scroll-with-animation="true">
      <view v-if="!messages.length && !loading" class="hello">
        <view class="orb">💬</view>
        <text class="h">您好，有什么健康问题想咨询？</text>
        <view class="samples">
          <text v-for="s in samples" :key="s" class="sp" @click="quick(s)">{{ s }}</text>
        </view>
      </view>

      <view v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
        <view v-if="m.role==='assistant'" class="face">🤖</view>
        <text class="bubble">{{ m.content }}</text>
      </view>

      <view v-if="loading" class="msg assistant">
        <view class="face">🤖</view>
        <view class="bubble typing"><text class="d"></text><text class="d"></text><text class="d"></text></view>
      </view>
    </scroll-view>

    <view class="bar">
      <input class="ipt" v-model="text" placeholder="请输入健康问题…" :disabled="loading" @confirm="send" />
      <button class="btn-primary send" :disabled="loading" @click="send">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { consultChat } from '@/api/index'
const messages = ref([]), text = ref(''), loading = ref(false), sessionId = ref(''), scrollTop = ref(0)
const samples = ['最近血压有点高怎么办？', '老年人补钙吃什么好？', '血糖偏高饮食注意什么？']
function quick(s) { text.value = s; send() }
async function send() {
  if (!text.value.trim() || loading.value) return
  const q = text.value
  messages.value.push({ role: 'user', content: q }); text.value = ''; toBottom()
  loading.value = true
  try {
    const res = await consultChat({ message: q, sessionId: sessionId.value })
    sessionId.value = res.data.sessionId
    messages.value.push({ role: 'assistant', content: res.data.content })
  } catch (e) { uni.showToast({ title: '发送失败', icon: 'none' }) } finally { loading.value = false; toBottom() }
}
function toBottom() { nextTick(() => { scrollTop.value = 999999 + Math.random() }) }
function goHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style scoped>
.chat { height: 100vh; display: flex; flex-direction: column; }

.topbar { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 28rpx;
  background: #fff; border-bottom: 2rpx solid var(--line); }
.tb-left { display: flex; align-items: center; gap: 16rpx; }
.tb-orb { width: 72rpx; height: 72rpx; border-radius: 20rpx; color: #fff; font-size: 40rpx;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg,#37B389,#279470); animation: floatY 4s ease-in-out infinite; }
.tb-meta { display: flex; flex-direction: column; }
.tb-name { font-size: 34rpx; font-weight: 800; }
.tb-sub { font-size: 24rpx; color: var(--ink-soft); display: flex; align-items: center; gap: 8rpx; }
.live { width: 14rpx; height: 14rpx; border-radius: 50%; background: #37A86B; animation: pulse 1.8s infinite; }
.more { display: flex; align-items: center; gap: 8rpx; padding: 14rpx 26rpx; border-radius: 999rpx;
  background: var(--primary); }
.more:active { transform: scale(.95); }
.more-ic { color: #fff; font-size: 30rpx; }
.more-t { color: #fff; font-size: 28rpx; font-weight: 700; }

.msgs { flex: 1; padding: 24rpx; box-sizing: border-box; }
.msg { display: flex; align-items: flex-end; gap: 14rpx; margin: 18rpx 0; }
.msg.user { justify-content: flex-end; }
.face { width: 60rpx; height: 60rpx; border-radius: 16rpx; background: #EAF6F2; display: flex; align-items: center; justify-content: center; font-size: 32rpx; flex-shrink: 0; }
.bubble { max-width: 74%; padding: 22rpx 26rpx; border-radius: 26rpx; font-size: 31rpx; line-height: 1.6; background: #fff; color: var(--ink); box-shadow: 0 4rpx 16rpx rgba(33,80,63,.06); border-bottom-left-radius: 8rpx; }
.msg.user .bubble { background: linear-gradient(135deg,#37B389,#2FA37C); color: #fff; border-bottom-left-radius: 26rpx; border-bottom-right-radius: 8rpx; }
.typing { display: flex; gap: 10rpx; }
.typing .d { width: 14rpx; height: 14rpx; border-radius: 50%; background: #9FD3C0; animation: bnc 1.2s infinite; }
.typing .d:nth-child(2){ animation-delay:.18s } .typing .d:nth-child(3){ animation-delay:.36s }
@keyframes bnc { 0%,60%,100%{ transform: translateY(0); opacity:.5 } 30%{ transform: translateY(-10rpx); opacity:1 } }

.hello { text-align: center; padding: 80rpx 20rpx; }
.orb { width: 130rpx; height: 130rpx; border-radius: 50%; margin: 0 auto 24rpx; font-size: 60rpx; display: flex; align-items: center; justify-content: center; color: #fff; background: linear-gradient(135deg,#37B389,#279470); box-shadow: 0 16rpx 34rpx rgba(39,148,112,.4); animation: floatY 4s ease-in-out infinite; }
.h { display: block; font-size: 36rpx; font-weight: 700; margin-bottom: 28rpx; }
.samples { display: flex; flex-direction: column; gap: 18rpx; align-items: center; }
.sp { padding: 20rpx 32rpx; border-radius: 999rpx; background: #fff; font-size: 30rpx; box-shadow: inset 0 0 0 2rpx #C1E3D8; }

.bar { display: flex; gap: 16rpx; padding: 18rpx 24rpx; background: #fff; border-top: 2rpx solid var(--line); }
.ipt { flex: 1; height: 84rpx; background: #F5F3EC; border-radius: 999rpx; padding: 0 28rpx; font-size: 30rpx; }
.send { height: 84rpx; line-height: 84rpx; padding: 0 40rpx; font-size: 30rpx; }
</style>
