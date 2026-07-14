<template>
  <div class="sessions-page">
    <div class="page-heading">
      <div>
        <h1>咨询会话</h1>
        <p>接收患者消息并进行实时健康咨询。</p>
      </div>
      <div class="connection" :class="{ online: connected }">
        <span></span>{{ connected ? '本端实时连接正常' : '正在重新连接' }}
      </div>
    </div>

    <el-card class="chat-shell">
      <aside class="session-list">
        <header class="list-head">
          <div><b>近期会话</b><span>{{ sessions.length }} 个</span></div>
          <el-button text :loading="loadingSessions" @click="loadSessions"><el-icon><Refresh /></el-icon></el-button>
        </header>
        <div v-if="loadingSessions && !sessions.length" class="session-loading">
          <el-skeleton v-for="index in 4" :key="index" animated>
            <template #template><div class="session-skeleton"><el-skeleton-item variant="circle" /><div><el-skeleton-item variant="text" /><el-skeleton-item variant="text" /></div></div></template>
          </el-skeleton>
        </div>
        <button
          v-for="item in sessions"
          :key="item.session.id"
          class="session-item"
          :class="{ active: active?.session.id === item.session.id }"
          @click="select(item)"
        >
          <el-avatar :size="42" :src="resolveServerUrl(item.patient?.avatar)">{{ patientInitial(item) }}</el-avatar>
          <span class="session-copy">
            <span class="session-name"><b>{{ item.patient?.nickname || item.patient?.username || '患者' }}</b><time>{{ shortTime(item.session.lastMessageTime) }}</time></span>
            <span class="last-message">{{ item.session.lastMessage || '暂无消息' }}</span>
          </span>
          <em v-if="item.session.unreadDoctor">{{ item.session.unreadDoctor }}</em>
        </button>
        <el-empty v-if="!loadingSessions && !sessions.length" description="暂无咨询会话" :image-size="76" />
      </aside>

      <section class="chat-panel">
        <template v-if="active">
          <header class="chat-head">
            <div class="patient-head">
              <el-avatar :size="44" :src="resolveServerUrl(active.patient?.avatar)">{{ patientInitial(active) }}</el-avatar>
              <div><strong>{{ active.patient?.nickname || active.patient?.username }}</strong><p>{{ genderText(active.patient?.gender) }} · {{ isClosed(active) ? '会话已结束' : '实时医生咨询' }}</p></div>
            </div>
            <div class="head-actions">
              <el-tag :type="isClosed(active) ? 'info' : 'success'">{{ isClosed(active) ? '已结束' : '进行中' }}</el-tag>
              <el-button plain @click="$router.push('/patients/' + active.patient.id)"><el-icon><Postcard /></el-icon>患者详情</el-button>
              <el-button v-if="!isClosed(active)" plain type="danger" @click="closeActiveSession">结束会话</el-button>
            </div>
          </header>

          <div ref="messageBox" v-loading="loadingMessages" class="messages">
            <transition-group name="message">
              <div v-for="message in messages" :key="message.id" :class="['message-row', message.senderType === 'DOCTOR' ? 'mine' : 'theirs']">
                <el-avatar v-if="message.senderType !== 'DOCTOR'" :size="34" :src="resolveServerUrl(active.patient?.avatar)">{{ patientInitial(active) }}</el-avatar>
                <div class="message-wrap">
                  <div class="bubble">
                    <template v-if="message.messageType === 'ATTACHMENT'">
                      <a :href="resolveServerUrl(message.attachmentUrl)" target="_blank" rel="noopener"><el-icon><Document /></el-icon>{{ message.attachmentName || '查看附件' }}</a>
                      <p v-if="message.content">{{ message.content }}</p>
                    </template>
                    <template v-else>{{ message.content }}</template>
                  </div>
                  <time>{{ messageTime(message.createTime) }}</time>
                </div>
              </div>
            </transition-group>
          </div>

          <footer class="composer">
            <el-upload :show-file-list="false" :http-request="upload" accept="image/*,.pdf,.doc,.docx">
              <el-tooltip content="发送附件" placement="top">
                <el-button class="icon-button" :disabled="isClosed(active)" :loading="uploading" aria-label="发送附件"><el-icon><Paperclip /></el-icon></el-button>
              </el-tooltip>
            </el-upload>
            <el-input v-model="text" size="large" :disabled="isClosed(active)" :placeholder="isClosed(active) ? '会话已结束，不能继续发送消息' : '输入回复内容，按 Enter 发送'" @keyup.enter="send" />
            <el-button type="primary" size="large" :disabled="isClosed(active) || !text.trim()" @click="send"><el-icon><Promotion /></el-icon>发送</el-button>
          </footer>
        </template>

        <div v-else class="chat-empty">
          <span><el-icon><ChatDotRound /></el-icon></span>
          <h2>选择一个咨询会话</h2>
          <p>患者发起咨询后，会话会实时出现在左侧列表。</p>
        </div>
      </section>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Document, Paperclip, Postcard, Promotion, Refresh } from '@element-plus/icons-vue'
import { closeSession, doctorWsUrl, getSessionMessages, pageSessions, sendSessionMessage, uploadAttachment } from '@/api'
import { resolveServerUrl } from '@/config/server'

const sessions = ref([])
const active = ref(null)
const messages = ref([])
const text = ref('')
const messageBox = ref(null)
const uploading = ref(false)
const loadingSessions = ref(true)
const loadingMessages = ref(false)
const connected = ref(false)
let websocket = null
let closing = false
let reconnectTimer = null

async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await pageSessions({ pageNum: 1, pageSize: 50 })
    const records = res.data.records || []
    const activeId = active.value?.session.id
    sessions.value = records
    if (activeId) active.value = records.find(item => item.session.id === activeId) || active.value
    if (!active.value && records.length) await select(records[0])
  } finally { loadingSessions.value = false }
}

async function select(item) {
  active.value = item
  loadingMessages.value = true
  try {
    const res = await getSessionMessages(item.session.id)
    messages.value = res.data || []
    item.session.unreadDoctor = 0
    scrollToBottom()
  } finally { loadingMessages.value = false }
}

async function send() {
  if (!active.value || isClosed(active.value) || !text.value.trim()) return
  const content = text.value.trim()
  text.value = ''
  const res = await sendSessionMessage(active.value.session.id, { content })
  messages.value.push(res.data)
  await loadSessions()
  scrollToBottom()
}

async function upload({ file }) {
  if (!active.value || isClosed(active.value)) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const uploaded = await uploadAttachment(formData)
    const res = await sendSessionMessage(active.value.session.id, {
      content: text.value.trim(),
      attachmentUrl: uploaded.data,
      attachmentName: file.name
    })
    text.value = ''
    messages.value.push(res.data)
    await loadSessions()
    scrollToBottom()
    ElMessage.success('附件已发送')
  } finally { uploading.value = false }
}

function connectWebSocket() {
  if (closing) return
  websocket = new WebSocket(doctorWsUrl())
  websocket.onopen = () => { connected.value = true }
  websocket.onmessage = async event => {
    const payload = JSON.parse(event.data)
    if (payload.type === 'DOCTOR_CONSULT_SESSION_CLOSED') {
      markSessionClosed(payload.data?.id)
      ElMessage.info('当前咨询会话已结束')
      await loadSessions()
      return
    }
    if (payload.type !== 'DOCTOR_CONSULT_MESSAGE') return
    const message = payload.data
    if (active.value?.session.id === message.sessionId && !messages.value.some(item => item.id === message.id)) {
      messages.value.push(message)
      scrollToBottom()
    }
    await loadSessions()
  }
  websocket.onerror = () => { connected.value = false }
  websocket.onclose = () => {
    connected.value = false
    websocket = null
    if (!closing) reconnectTimer = setTimeout(connectWebSocket, 3000)
  }
}

async function closeActiveSession() {
  if (!active.value || isClosed(active.value)) return
  try {
    await ElMessageBox.confirm('结束后双方将不能继续发送消息，但仍可查看历史记录。确定结束该咨询会话吗？', '结束会话', {
      type: 'warning',
      confirmButtonText: '结束会话',
      cancelButtonText: '取消'
    })
    const res = await closeSession(active.value.session.id)
    active.value = res.data
    markSessionClosed(res.data.session.id)
    await loadSessions()
    ElMessage.success('会话已结束')
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
    item.session.unreadDoctor = 0
  }
  if (active.value?.session.id === sessionId) {
    active.value.session.status = 'CLOSED'
    active.value.session.lastMessage = '[会话已结束]'
    active.value.session.unreadDoctor = 0
  }
}

function isClosed(item) { return item?.session?.status === 'CLOSED' }
function scrollToBottom() { nextTick(() => { if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight }) }
function patientInitial(item) { return (item.patient?.nickname || item.patient?.username || '患').charAt(0) }
function genderText(value) { return value === 1 ? '男' : value === 2 ? '女' : '性别保密' }
function shortTime(value) { return value ? String(value).slice(5, 16).replace('T', ' ') : '' }
function messageTime(value) { return value ? String(value).slice(11, 16) : '' }

onMounted(() => { closing = false; loadSessions(); connectWebSocket() })
onBeforeUnmount(() => {
  closing = true
  connected.value = false
  if (reconnectTimer) clearTimeout(reconnectTimer)
  websocket?.close()
})
</script>

<style scoped>
.connection { display: flex; align-items: center; gap: 8px; color: var(--el-color-warning); font-size: 14px; }
.connection span { width: 9px; height: 9px; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 5px rgba(232,147,59,.12); }
.connection.online { color: var(--el-color-success); }
.connection.online span { box-shadow: 0 0 0 5px rgba(47,163,124,.12); animation: connection-pulse 2s ease-in-out infinite; }
@keyframes connection-pulse { 50% { box-shadow: 0 0 0 8px rgba(47,163,124,0); } }
.sessions-page { height: calc(100dvh - 120px); overflow: hidden; display: flex; flex-direction: column; }
.sessions-page .page-heading { flex: 0 0 auto; }
.chat-shell { flex: 1; min-height: 0; overflow: hidden; }
.chat-shell :deep(.el-card__body) { height: 100%; min-height: 0; padding: 0; display: grid; grid-template-columns: 330px 1fr; }
.session-list { min-width: 0; min-height: 0; overflow-y: auto; overscroll-behavior: contain; border-right: 1px solid var(--hda-line); background: rgba(247,250,253,.54); }
.list-head { position: sticky; top: 0; z-index: 2; min-height: 66px; padding: 0 18px; border-bottom: 1px solid var(--hda-line); background: rgba(255,255,255,.88); backdrop-filter: blur(12px); display: flex; align-items: center; justify-content: space-between; }
.list-head b { color: var(--hda-ink); font-size: 17px; }
.list-head span { margin-left: 8px; color: #93A2B5; font-size: 13px; }
.session-item { position: relative; width: 100%; min-height: 82px; padding: 14px 17px; border: 0; border-bottom: 1px solid rgba(223,231,242,.78); background: transparent; display: flex; align-items: center; gap: 11px; text-align: left; cursor: pointer; transition: background-color .2s, transform .25s var(--ease-out); }
.session-item:hover { z-index: 1; background: #F2F7FE; transform: translateX(3px); }
.session-item.active { background: var(--el-color-primary-light-9); box-shadow: inset 3px 0 0 var(--el-color-primary); }
.session-item .el-avatar { flex: 0 0 auto; color: var(--el-color-primary); background: #fff; font-weight: 700; }
.session-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.session-name { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.session-name b { overflow: hidden; color: var(--hda-ink); font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.session-name time { color: #9AA7B8; font-size: 11px; }
.last-message { overflow: hidden; color: var(--hda-ink-soft); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.session-item em { position: absolute; top: 43px; right: 16px; min-width: 20px; height: 20px; padding: 0 5px; border-radius: 10px; color: #fff; background: var(--el-color-danger); font: normal 11px/20px var(--hda-font-display); text-align: center; }
.session-loading { padding: 10px 16px; }
.session-skeleton { height: 70px; display: flex; align-items: center; gap: 12px; }
.session-skeleton > .el-skeleton__item { width: 40px; height: 40px; }
.session-skeleton div { flex: 1; }
.chat-panel { min-width: 0; min-height: 0; display: flex; flex-direction: column; background: rgba(255,255,255,.42); }
.chat-head { min-height: 72px; padding: 0 22px; border-bottom: 1px solid var(--hda-line); display: flex; align-items: center; justify-content: space-between; background: rgba(255,255,255,.74); }
.head-actions { display: flex; align-items: center; gap: 10px; }
.patient-head { display: flex; align-items: center; gap: 12px; }
.patient-head .el-avatar { color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 700; }
.patient-head strong { color: var(--hda-ink); font-size: 18px; }
.patient-head p { margin: 0; color: var(--hda-ink-soft); font-size: 13px; }
.messages { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior: contain; padding: 24px; background-image: linear-gradient(rgba(223,231,242,.26) 1px, transparent 1px), linear-gradient(90deg, rgba(223,231,242,.26) 1px, transparent 1px); background-size: 32px 32px; }
.message-row { margin: 14px 0; display: flex; align-items: flex-end; gap: 9px; }
.message-row.mine { justify-content: flex-end; }
.message-row .el-avatar { flex: 0 0 auto; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 12px; }
.message-wrap { max-width: min(68%, 680px); display: flex; flex-direction: column; }
.mine .message-wrap { align-items: flex-end; }
.bubble { padding: 11px 15px; color: var(--hda-ink); background: #F1F4F8; box-shadow: 0 4px 12px rgba(28,63,120,.06); line-height: 1.65; overflow-wrap: anywhere; }
.mine .bubble { color: #fff; background: linear-gradient(135deg, #3E86EC, #2E6FE0 62%, #2458B8); box-shadow: 0 8px 18px rgba(46,111,224,.22); }
.bubble a { display: flex; align-items: center; gap: 6px; color: var(--el-color-primary); font-weight: 700; }
.mine .bubble a { color: #fff; }
.bubble p { margin: 6px 0 0; }
.message-wrap time { margin-top: 3px; color: #9AA7B8; font-size: 10px; }
.message-enter-active { transition: opacity .3s var(--ease-out), transform .3s var(--ease-out); }
.message-enter-from { opacity: 0; transform: translateY(10px) scale(.98); }
.composer { min-height: 76px; padding: 13px 18px; border-top: 1px solid var(--hda-line); display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,.88); }
.composer .el-input { flex: 1; }
.icon-button { width: 48px; padding: 0; font-size: 20px; }
.chat-empty { margin: auto; padding: 30px; text-align: center; }
.chat-empty > span { width: 78px; height: 78px; margin: 0 auto 18px; display: grid; place-items: center; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 36px; }
.chat-empty h2 { margin: 0; color: var(--hda-ink); font-size: 23px; }
.chat-empty p { margin: 7px 0 0; color: var(--hda-ink-soft); }
@media (max-width: 820px) { .chat-shell { height: auto; min-height: 700px; } .chat-shell :deep(.el-card__body) { grid-template-columns: 1fr; grid-template-rows: 230px 1fr; } .session-list { border-right: 0; border-bottom: 1px solid var(--hda-line); } .message-wrap { max-width: 82%; } }
@media (max-width: 560px) { .chat-head { padding: 0 14px; } .chat-head > .el-button { width: 44px; padding: 0; font-size: 0; } .chat-head > .el-button .el-icon { margin: 0; font-size: 17px; } .messages { padding: 16px 12px; } .composer { padding: 10px; } .composer > .el-button { width: 48px; padding: 0; font-size: 0; } .composer > .el-button .el-icon { margin: 0; font-size: 18px; } }
</style>
