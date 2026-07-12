<template>
  <view class="page-pad">
    <view v-if="!active">
      <view v-for="session in sessions" :key="session.session.id" class="card session" @click="open(session)">
        <text class="name">{{ session.patient?.nickname || session.patient?.username }}</text>
        <text class="muted">{{ session.session.lastMessage || '暂无消息' }}</text>
        <text v-if="session.session.unreadDoctor" class="unread">{{ session.session.unreadDoctor }}</text>
      </view>
    </view>

    <view v-else class="chat">
      <view class="chat-head card">
        <text class="name">{{ active.patient?.nickname || active.patient?.username }}</text>
        <button class="btn-ghost back" @click="active = null">返回</button>
      </view>
      <scroll-view scroll-y class="msgs" :scroll-into-view="scrollTarget">
        <view
          v-for="message in messages"
          :id="`message-${message.id}`"
          :key="message.id"
          :class="['msg', message.senderType === 'DOCTOR' ? 'me' : 'other']"
        >
          <view class="bubble" @click="openAttachment(message)">
            <text v-if="message.messageType === 'ATTACHMENT'" class="attachment">
              {{ message.attachmentName || '查看附件' }}
            </text>
            <text v-if="message.content">{{ message.content }}</text>
          </view>
        </view>
      </scroll-view>
      <view class="inputbar">
        <button class="btn-ghost attach" :loading="uploading" @click="chooseAttachment">附件</button>
        <input v-model="text" confirm-type="send" placeholder="输入回复内容" @confirm="send" />
        <button class="btn-primary send" @click="send">发送</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import {
  getSessionMessages,
  pageSessions,
  sendSessionMessage,
  uploadAttachment
} from '@/api'

const sessions = ref([])
const active = ref(null)
const messages = ref([])
const text = ref('')
const uploading = ref(false)
const scrollTarget = ref('')
let socketTask = null
let reconnectTimer = null
let closing = false

async function load() {
  const res = await pageSessions({ pageNum: 1, pageSize: 50 })
  sessions.value = res.data.records || []
}

async function open(session) {
  active.value = session
  const res = await getSessionMessages(session.session.id)
  messages.value = res.data || []
  session.session.unreadDoctor = 0
  scrollToBottom()
}

async function send() {
  if (!active.value || !text.value.trim()) return
  const content = text.value.trim()
  text.value = ''
  const res = await sendSessionMessage(active.value.session.id, { content })
  messages.value.push(res.data)
  await load()
  scrollToBottom()
}

function chooseAttachment() {
  if (!active.value || uploading.value) return
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    success: async ({ tempFiles }) => {
      const file = tempFiles[0]
      uploading.value = true
      try {
        const uploaded = await uploadAttachment(file.path)
        const res = await sendSessionMessage(active.value.session.id, {
          content: text.value,
          attachmentUrl: uploaded.data,
          attachmentName: file.name
        })
        text.value = ''
        messages.value.push(res.data)
        await load()
        scrollToBottom()
      } finally {
        uploading.value = false
      }
    }
  })
}

function openAttachment(message) {
  if (message.messageType !== 'ATTACHMENT' || !message.attachmentUrl) return
  const url = message.attachmentUrl.startsWith('http')
    ? message.attachmentUrl
    : `http://localhost:8080${message.attachmentUrl}`
  uni.downloadFile({
    url,
    success: ({ tempFilePath }) => uni.openDocument({ filePath: tempFilePath, showMenu: true })
  })
}

function connectSocket() {
  if (closing || socketTask) return
  const token = uni.getStorageSync('doctorToken')
  socketTask = uni.connectSocket({
    url: `ws://localhost:8080/ws/doctor-consult?token=${encodeURIComponent(token)}`
  })
  socketTask.onMessage(async ({ data }) => {
    const payload = JSON.parse(data)
    if (payload.type !== 'DOCTOR_CONSULT_MESSAGE') return
    const message = payload.data
    if (active.value?.session.id === message.sessionId) {
      messages.value.push(message)
      scrollToBottom()
    }
    await load()
  })
  socketTask.onClose(() => {
    socketTask = null
    if (!closing) reconnectTimer = setTimeout(connectSocket, 3000)
  })
  socketTask.onError(() => socketTask?.close())
}

function closeSocket() {
  closing = true
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (socketTask) socketTask.close()
  socketTask = null
}

function scrollToBottom() {
  nextTick(() => {
    const last = messages.value[messages.value.length - 1]
    scrollTarget.value = last ? `message-${last.id}` : ''
  })
}

onShow(() => {
  closing = false
  load()
  connectSocket()
})
onHide(closeSocket)
onUnload(closeSocket)
</script>

<style scoped>
.session { position: relative; margin-bottom: 18rpx; }
.name { display: block; font-size: 34rpx; font-weight: 800; }
.unread { position: absolute; top: 24rpx; right: 24rpx; min-width: 42rpx; height: 42rpx; line-height: 42rpx; text-align: center; color: #fff; background: #E44D61; border-radius: 50%; font-size: 22rpx; }
.chat { height: calc(100vh - 56rpx); display: flex; flex-direction: column; }
.chat-head { display: flex; justify-content: space-between; align-items: center; }
.back { width: 140rpx; font-size: 26rpx; }
.msgs { flex: 1; padding: 24rpx 0; }
.msg { margin: 14rpx 0; display: flex; }
.bubble { display: flex; flex-direction: column; max-width: 70%; padding: 20rpx 24rpx; background: #fff; line-height: 1.6; }
.msg.me { justify-content: flex-end; }
.msg.me .bubble { background: var(--primary); color: #fff; }
.attachment { color: var(--primary); text-decoration: underline; }
.msg.me .attachment { color: #fff; }
.inputbar { display: flex; gap: 12rpx; align-items: center; background: #fff; padding: 16rpx; }
.inputbar input { flex: 1; height: 76rpx; background: #F7FAFD; padding: 0 20rpx; }
.attach, .send { width: 126rpx; font-size: 25rpx; }
</style>
