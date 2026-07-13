<template>
  <el-card class="chat-card">
    <aside class="sessions">
      <div class="side-head">
        <strong>医生咨询</strong>
        <span class="muted">选择医生后开始实时对话</span>
      </div>
      <button v-for="item in sessions" :key="item.session.id" :class="{ on: active?.session.id === item.session.id, closed: isClosed(item) }" @click="select(item)">
        <b>{{ item.doctor?.name || '医生' }}</b>
        <span>{{ item.session.lastMessage || `${item.doctor?.department || ''} · ${item.doctor?.title || ''}` }}</span>
        <small v-if="isClosed(item)">已结束</small>
        <em v-if="item.session.unreadUser">{{ item.session.unreadUser }}</em>
      </button>
    </aside>

    <section class="chat">
      <template v-if="active">
        <header class="chat-head">
          <div class="doc">
            <el-avatar :size="46" :src="active.doctor?.avatar">{{ active.doctor?.name?.charAt(0) || '医' }}</el-avatar>
            <div>
              <div class="doc-name">{{ active.doctor?.name }}</div>
              <div class="doc-sub">{{ active.doctor?.hospital }} · {{ active.doctor?.department }} · {{ isClosed(active) ? '会话已结束' : '实时咨询' }}</div>
            </div>
          </div>
          <div class="head-actions">
            <el-tag :type="isClosed(active) ? 'info' : 'success'">{{ isClosed(active) ? '已结束' : '实时咨询' }}</el-tag>
            <el-button v-if="!isClosed(active)" plain type="danger" @click="closeActiveSession">结束咨询</el-button>
          </div>
        </header>
        <div class="messages" ref="msgBox">
          <div v-for="m in messages" :key="m.id" :class="['msg', m.senderType === 'USER' ? 'user' : 'doctor']">
            <div v-if="m.senderType === 'DOCTOR'" class="face"><el-icon><Avatar /></el-icon></div>
            <div class="bubble">
              <template v-if="m.messageType === 'ATTACHMENT'">
                <a :href="m.attachmentUrl" target="_blank">{{ m.attachmentName || '查看附件' }}</a>
                <p v-if="m.content">{{ m.content }}</p>
              </template>
              <template v-else>{{ m.content }}</template>
            </div>
          </div>
        </div>
        <footer class="input-row">
          <el-upload :show-file-list="false" :http-request="doUpload">
            <el-button :disabled="isClosed(active)" :loading="uploading">附件</el-button>
          </el-upload>
          <el-input v-model="text" size="large" :disabled="isClosed(active)" :placeholder="isClosed(active) ? '会话已结束，不能继续发送消息' : '请输入想咨询医生的问题…'" @keyup.enter="send" />
          <el-button type="primary" size="large" :disabled="isClosed(active) || !text.trim()" @click="send">
            <el-icon><Promotion /></el-icon>&nbsp;发送
          </el-button>
        </footer>
      </template>
      <div v-else class="hello">
        <div class="hello-orb"><el-icon :size="40"><ChatDotRound /></el-icon></div>
        <h3>请选择医生开始咨询</h3>
        <el-button type="primary" @click="$router.push('/doctor')">前往医生专家库</el-button>
      </div>
    </section>
  </el-card>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Avatar, ChatDotRound, Promotion } from '@element-plus/icons-vue'
import {
  closeDoctorConsultSession,
  doctorConsultWsUrl,
  getDoctorConsultMessages,
  pageDoctorConsultSessions,
  sendDoctorConsultMessage,
  startDoctorSession,
  uploadConsultAttachment
} from '@/api'

const route = useRoute()
const sessions = ref([])
const active = ref(null)
const messages = ref([])
const text = ref('')
const msgBox = ref(null)
const uploading = ref(false)
let ws = null
let closing = false
let reconnectTimer = null

async function loadSessions() {
  const res = await pageDoctorConsultSessions({ pageNum: 1, pageSize: 50 })
  sessions.value = res.data.records || []
}

async function ensureSessionFromDoctor() {
  if (!route.query.doctorId) return
  const res = await startDoctorSession(route.query.doctorId)
  await loadSessions()
  const found = sessions.value.find(s => s.session.id === res.data.session.id)
  if (found) await select(found)
}

async function select(item) {
  active.value = item
  const res = await getDoctorConsultMessages(item.session.id)
  messages.value = res.data || []
  item.session.unreadUser = 0
  scroll()
}

async function send() {
  if (!active.value || isClosed(active.value) || !text.value.trim()) return
  const content = text.value.trim()
  text.value = ''
  const res = await sendDoctorConsultMessage(active.value.session.id, { content })
  messages.value.push(res.data)
  await loadSessions()
  scroll()
}

async function doUpload({ file }) {
  if (!active.value || isClosed(active.value)) return
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const uploaded = await uploadConsultAttachment(fd)
    const res = await sendDoctorConsultMessage(active.value.session.id, {
      content: text.value,
      attachmentUrl: uploaded.data,
      attachmentName: file.name
    })
    text.value = ''
    messages.value.push(res.data)
    await loadSessions()
    scroll()
  } finally { uploading.value = false }
}

function connectWs() {
  if (closing) return
  ws = new WebSocket(doctorConsultWsUrl())
  ws.onmessage = async event => {
    const payload = JSON.parse(event.data)
    if (payload.type === 'DOCTOR_CONSULT_SESSION_CLOSED') {
      markSessionClosed(payload.data?.id)
      ElMessage.info('当前咨询会话已结束')
      await loadSessions()
      return
    }
    if (payload.type !== 'DOCTOR_CONSULT_MESSAGE') return
    const msg = payload.data
    if (active.value?.session.id === msg.sessionId) {
      messages.value.push(msg)
      scroll()
    }
    await loadSessions()
  }
  ws.onclose = () => {
    if (!closing) reconnectTimer = setTimeout(connectWs, 3000)
  }
}

async function closeActiveSession() {
  if (!active.value || isClosed(active.value)) return
  try {
    await ElMessageBox.confirm('结束后双方将不能继续发送消息，但仍可查看历史记录。确定结束该咨询吗？', '结束咨询', {
      type: 'warning',
      confirmButtonText: '结束咨询',
      cancelButtonText: '取消'
    })
    const res = await closeDoctorConsultSession(active.value.session.id)
    active.value = res.data
    markSessionClosed(res.data.session.id)
    await loadSessions()
    ElMessage.success('咨询已结束')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') throw e
  }
}

function markSessionClosed(sessionId) {
  if (!sessionId) return
  const item = sessions.value.find(record => record.session.id === sessionId)
  if (item) {
    item.session.status = 'CLOSED'
    item.session.lastMessage = '[会话已结束]'
    item.session.unreadUser = 0
  }
  if (active.value?.session.id === sessionId) {
    active.value.session.status = 'CLOSED'
    active.value.session.lastMessage = '[会话已结束]'
    active.value.session.unreadUser = 0
  }
}

function isClosed(item) { return item?.session?.status === 'CLOSED' }
function scroll() { nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight }) }

onMounted(async () => {
  closing = false
  await loadSessions()
  await ensureSessionFromDoctor()
  if (!active.value && sessions.value.length) await select(sessions.value[0])
  connectWs()
})
onBeforeUnmount(() => {
  closing = true
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) ws.close()
})
</script>

<style scoped>
.chat-card { height: calc(100dvh - 120px); overflow: hidden; }
.chat-card :deep(.el-card__body) { height: 100%; min-height: 0; display: grid; grid-template-columns: 320px 1fr; padding: 0; }
.sessions { min-height: 0; border-right: 1px solid var(--hda-line); overflow-y: auto; overscroll-behavior: contain; }
.side-head { padding: 18px 20px; border-bottom: 1px solid var(--hda-line); }
.side-head strong { display: block; font-size: 19px; color: var(--hda-ink); }
.side-head span { font-size: 13px; }
.sessions button { position: relative; width: 100%; text-align: left; border: 0; background: transparent; padding: 18px 20px; cursor: pointer; border-bottom: 1px solid var(--hda-line); }
.sessions button.on { background: var(--el-color-primary-light-9); }
.sessions button.closed { opacity: .72; }
.sessions b { display: block; font-size: 17px; color: var(--hda-ink); }
.sessions span { display: block; color: var(--hda-ink-soft); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.sessions small { display: inline-block; margin-top: 4px; color: #8B9AAF; font-size: 12px; }
.sessions em { position: absolute; top: 18px; right: 18px; min-width: 22px; height: 22px; line-height: 22px; text-align: center; background: var(--el-color-danger); color: #fff; font-style: normal; font-size: 12px; }
.chat { min-width: 0; min-height: 0; display: flex; flex-direction: column; }
.chat-head { height: 78px; display: flex; align-items: center; justify-content: space-between; padding: 0 22px; border-bottom: 1px solid var(--hda-line); }
.head-actions { display: flex; align-items: center; gap: 10px; }
.doc { display: flex; align-items: center; gap: 12px; }
.doc-name { font-size: 18px; font-weight: 800; color: var(--hda-ink); }
.doc-sub { font-size: 14px; color: var(--hda-ink-soft); }
.messages { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; padding: 22px; }
.msg { display: flex; align-items: flex-end; gap: 10px; margin: 14px 0; }
.msg.user { justify-content: flex-end; }
.face { width: 38px; height: 38px; display: grid; place-items: center; flex-shrink: 0; background: var(--el-color-primary-light-8); color: var(--el-color-primary); }
.bubble { max-width: 68%; padding: 14px 18px; font-size: 17px; line-height: 1.65; background: #F3F1EA; color: var(--hda-ink); box-shadow: var(--hda-shadow-sm); }
.msg.user .bubble { background: linear-gradient(135deg, #37B389, #2FA37C); color: #fff; }
.msg.user .bubble a { color: #fff; }
.input-row { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--hda-line); }
.input-row .el-input { flex: 1; }
.hello { text-align: center; margin: auto; }
.hello-orb { width: 88px; height: 88px; border-radius: 50%; display: grid; place-items: center; margin: 0 auto 18px; color: #fff; background: linear-gradient(135deg, #37B389, #279470); }
@media (max-width: 900px) { .chat-card :deep(.el-card__body) { grid-template-columns: 1fr; } .sessions { max-height: 220px; border-right: 0; } }
</style>
