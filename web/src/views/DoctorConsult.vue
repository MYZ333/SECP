<template>
  <div class="doctor-consult-page">
    <el-card class="chat-card">
      <aside class="sessions">
        <div class="side-head">
          <div class="side-title">
            <strong>医生咨询</strong>
            <el-button class="history-trigger" text type="primary" @click="openHistoryDialog">历史对话</el-button>
          </div>
          <span class="muted">选择医生后开始实时对话</span>
        </div>
        <button v-for="item in sessions" :key="item.session.id" :class="{ on: active?.session.id === item.session.id }" @click="select(item)">
          <b>{{ item.doctor?.name || '医生' }}</b>
          <span>{{ item.session.lastMessage || `${item.doctor?.department || ''} · ${item.doctor?.title || ''}` }}</span>
          <em v-if="item.session.unreadUser">{{ item.session.unreadUser }}</em>
        </button>
        <div v-if="!sessions.length" class="session-empty">
          <p>暂无进行中的咨询</p>
        </div>
      </aside>

      <section class="chat">
        <template v-if="active">
          <header class="chat-head">
            <div class="doc">
              <el-avatar :size="46" :src="resolveServerUrl(active.doctor?.avatar)">{{ active.doctor?.name?.charAt(0) || '医' }}</el-avatar>
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
                  <a :href="resolveServerUrl(m.attachmentUrl)" target="_blank">{{ m.attachmentName || '查看附件' }}</a>
                  <p v-if="m.content">{{ m.content }}</p>
                </template>
                <template v-else>{{ m.content }}</template>
              </div>
            </div>
            <div v-if="isClosed(active) && !hasDoctorSummary(active)" class="result-panel waiting-summary">
              <div class="result-head">
                <strong>等待医生报告</strong>
                <el-tag type="info">已结束</el-tag>
              </div>
              <p>咨询已结束，医生正在整理本次咨询报告。报告完成后，这里会显示咨询结果并开放评价。</p>
            </div>
            <div v-else-if="isClosed(active)" class="result-panel">
              <div class="result-head">
                <strong>咨询报告</strong>
                <el-tag :type="active.session.recommendOffline ? 'warning' : 'success'">
                  {{ active.session.recommendOffline ? '建议线下就医' : '居家观察' }}
                </el-tag>
              </div>
              <div class="result-grid">
                <section>
                  <span>问题概述</span>
                  <p>{{ active.session.problemOverview || '医生暂未填写问题概述。' }}</p>
                </section>
                <section>
                  <span>初步判断</span>
                  <p>{{ active.session.preliminaryAssessment || '医生暂未填写初步判断。' }}</p>
                </section>
                <section>
                  <span>本次总结</span>
                  <p>{{ active.session.summary || '医生暂未填写本次咨询总结。' }}</p>
                </section>
                <section>
                  <span>处理建议</span>
                  <p>{{ active.session.advice || '暂无后续建议。' }}</p>
                </section>
                <section class="risk-section">
                  <span>风险提醒</span>
                  <p>{{ active.session.riskWarning || '暂无特别风险提醒。' }}</p>
                </section>
              </div>
              <div v-if="active.session.rating" class="feedback-done">
                <el-rate :model-value="active.session.rating" disabled />
                <span>已评价</span>
                <p v-if="active.session.feedback">{{ active.session.feedback }}</p>
              </div>
              <div v-else class="feedback-form">
                <div class="feedback-top">
                  <span>本次服务评价</span>
                  <el-rate v-model="rating" />
                </div>
                <el-checkbox-group v-model="feedbackTags" class="tag-group">
                  <el-checkbox-button v-for="tag in feedbackTagOptions" :key="tag" :label="tag">{{ tag }}</el-checkbox-button>
                </el-checkbox-group>
                <el-input v-model="feedbackText" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="也可以补充说明本次咨询体验" />
                <el-button type="primary" :loading="submittingFeedback" @click="submitFeedback">提交评价</el-button>
              </div>
              <div v-if="medicationAdvice" class="medication-panel">
                <div class="medication-head">
                  <span>用药建议</span>
                  <el-tag :type="medicationAdvice.status === 'CONFIRMED' ? 'success' : 'warning'">
                    {{ medicationAdvice.status === 'CONFIRMED' ? '已确认' : '待确认' }}
                  </el-tag>
                </div>
                <p v-if="medicationAdvice.doctorNote" class="doctor-note">{{ medicationAdvice.doctorNote }}</p>
                <div class="medicine-list">
                  <section v-for="item in medicationAdvice.items || []" :key="item.id || item.medicineId">
                    <b>{{ item.medicineName }}</b>
                    <small>{{ item.specification }}</small>
                    <p>{{ item.usageMethod || '用法未填' }} · {{ item.dosage }} · {{ item.frequency }} · {{ item.durationDays }}天</p>
                    <em v-if="item.precautions">{{ item.precautions }}</em>
                  </section>
                </div>
                <el-button v-if="medicationAdvice.status !== 'CONFIRMED'" class="confirm-advice" type="primary" :loading="confirmingMedication" @click="confirmAdvice">
                  我已知晓
                </el-button>
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

    <el-dialog v-model="historyDialogVisible" title="历史对话" width="620px" class="history-dialog">
      <div v-if="historySessions.length" class="history-list">
        <button v-for="item in historySessions" :key="item.session.id" type="button" :class="{ on: active?.session.id === item.session.id }" @click="selectHistory(item)">
          <span class="history-main">
            <b>{{ item.doctor?.name || '医生' }}</b>
            <small>{{ formatTime(item.session.lastMessageTime || item.session.updateTime || item.session.createTime) }}</small>
          </span>
          <span class="history-sub">{{ item.session.problemOverview || item.session.summary || item.session.advice || item.session.lastMessage || '已结束咨询' }}</span>
          <span class="history-meta">
            <el-tag size="small" type="info">已结束</el-tag>
            <el-rate v-if="item.session.rating" :model-value="item.session.rating" disabled size="small" />
            <em v-else>未评价</em>
          </span>
        </button>
      </div>
      <el-empty v-else description="暂无历史对话" :image-size="78" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Avatar, ChatDotRound, Promotion } from '@element-plus/icons-vue'
import {
  closeDoctorConsultSession,
  confirmMedicationAdvice,
  doctorConsultWsUrl,
  getPatientMedicationAdvice,
  getDoctorConsultMessages,
  pageDoctorConsultSessions,
  sendDoctorConsultMessage,
  startAlertHandling,
  startDoctorSession,
  submitDoctorConsultFeedback,
  uploadConsultAttachment
} from '@/api'
import { resolveServerUrl } from '@/config/server'

const route = useRoute()
const router = useRouter()
const allSessions = ref([])
const sessions = computed(() => allSessions.value.filter(item => !isClosed(item)))
const historySessions = computed(() => allSessions.value.filter(item => isClosed(item)))
const active = ref(null)
const messages = ref([])
const text = ref('')
const msgBox = ref(null)
const uploading = ref(false)
const submittingFeedback = ref(false)
const medicationAdvice = ref(null)
const confirmingMedication = ref(false)
const historyDialogVisible = ref(false)
const rating = ref(0)
const feedbackTags = ref([])
const feedbackText = ref('')
const feedbackTagOptions = ['回复及时', '解释清楚', '态度好', '建议有帮助']
let ws = null
let closing = false
let reconnectTimer = null

async function loadSessions() {
  const res = await pageDoctorConsultSessions({ pageNum: 1, pageSize: 50 })
  allSessions.value = res.data.records || []
  const activeId = active.value?.session?.id
  if (activeId) {
    const latest = allSessions.value.find(item => item.session.id === activeId)
    if (latest) active.value = latest
  }
}

async function ensureSessionFromDoctor() {
  if (!route.query.doctorId) return
  const healthAssistantSessionId = typeof route.query.healthAssistantSessionId === 'string'
    ? route.query.healthAssistantSessionId
    : ''
  const res = await startDoctorSession(route.query.doctorId, { healthAssistantSessionId: healthAssistantSessionId || null })
  await loadSessions()
  const found = allSessions.value.find(s => s.session.id === res.data.session.id)
  if (found) {
    await select(found)
    if (healthAssistantSessionId) ElMessage.success('健康助手问诊摘要已发送给医生')
    if (route.query.alertId) {
      try {
        await startAlertHandling(route.query.alertId, {
          channel: 'DOCTOR_CONSULT',
          sessionId: String(found.session.id)
        })
      } catch { ElMessage.warning('咨询会话已创建，但预警状态同步失败，请稍后在预警页查看') }
      if (typeof route.query.q === 'string') text.value = route.query.q
    }
    const nextQuery = { ...route.query }
    delete nextQuery.doctorId
    delete nextQuery.healthAssistantSessionId
    await router.replace({ path: route.path, query: nextQuery })
  }
}

async function ensureSessionFromRoute() {
  const sessionId = Number(route.query.sessionId)
  if (!sessionId) return false
  const found = allSessions.value.find(item => Number(item.session.id) === sessionId)
  if (!found) {
    ElMessage.warning('未找到该历史咨询')
    return false
  }
  await select(found)
  return true
}

async function select(item) {
  active.value = item
  resetFeedbackDraft()
  const res = await getDoctorConsultMessages(item.session.id)
  messages.value = res.data || []
  await loadMedicationAdvice()
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
      markSessionClosed(payload.data)
      ElMessage.info('当前咨询会话已结束')
      await loadSessions()
      return
    }
    if (payload.type === 'DOCTOR_CONSULT_SUMMARY_READY') {
      mergeSession(payload.data)
      ElMessage.success('医生已提交本次咨询总结，可以评价了')
      await loadSessions()
      await loadMedicationAdvice()
      return
    }
    if (payload.type === 'DOCTOR_CONSULT_MEDICATION_ADVICE') {
      if (active.value?.session.id === payload.data?.sessionId) {
        medicationAdvice.value = payload.data || null
        ElMessage.success('医生已发送用药建议，请查看并确认')
      }
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
    markSessionClosed(res.data.session)
    await loadSessions()
    ElMessage.success('咨询已结束')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') throw e
  }
}

function openHistoryDialog() {
  historyDialogVisible.value = true
}

async function selectHistory(item) {
  await select(item)
  historyDialogVisible.value = false
}

async function submitFeedback() {
  if (!active.value) return
  if (!rating.value) {
    ElMessage.warning('请先点击星级评分')
    return
  }
  submittingFeedback.value = true
  try {
    const res = await submitDoctorConsultFeedback(active.value.session.id, {
      rating: rating.value,
      tags: feedbackTags.value,
      feedback: feedbackText.value.trim()
    })
    active.value = res.data
    const item = allSessions.value.find(record => record.session.id === res.data.session.id)
    if (item) item.session = res.data.session
    ElMessage.success('评价已提交，感谢反馈')
  } finally {
    submittingFeedback.value = false
  }
}

async function loadMedicationAdvice() {
  medicationAdvice.value = null
  if (!active.value || !isClosed(active.value) || !hasDoctorSummary(active.value)) return
  const res = await getPatientMedicationAdvice(active.value.session.id)
  medicationAdvice.value = res.data || null
}

async function confirmAdvice() {
  if (!medicationAdvice.value) return
  confirmingMedication.value = true
  try {
    const res = await confirmMedicationAdvice(medicationAdvice.value.id)
    medicationAdvice.value = res.data
    ElMessage.success('已确认用药建议')
  } finally {
    confirmingMedication.value = false
  }
}

function mergeSession(sessionData) {
  if (!sessionData?.id) return
  const item = allSessions.value.find(record => record.session.id === sessionData.id)
  if (item) item.session = { ...item.session, ...sessionData }
  if (active.value?.session.id === sessionData.id) active.value.session = { ...active.value.session, ...sessionData }
}

function resetFeedbackDraft() {
  rating.value = 0
  feedbackTags.value = []
  feedbackText.value = ''
}

function markSessionClosed(sessionData) {
  const sessionId = typeof sessionData === 'object' ? sessionData?.id : sessionData
  if (!sessionId) return
  const item = allSessions.value.find(record => record.session.id === sessionId)
  if (item) {
    item.session = { ...item.session, ...(typeof sessionData === 'object' ? sessionData : {}), status: 'CLOSED', lastMessage: '[会话已结束]', unreadUser: 0 }
  }
  if (active.value?.session.id === sessionId) {
    active.value.session = { ...active.value.session, ...(typeof sessionData === 'object' ? sessionData : {}), status: 'CLOSED', lastMessage: '[会话已结束]', unreadUser: 0 }
  }
}

function isClosed(item) { return item?.session?.status === 'CLOSED' }
function hasDoctorSummary(item) {
  const session = item?.session
  return Boolean(session?.problemOverview || session?.preliminaryAssessment || session?.summary || session?.advice || session?.riskWarning)
}
function scroll() { nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight }) }
function formatTime(value) { return value ? String(value).slice(0, 16).replace('T', ' ') : '未知时间' }

onMounted(async () => {
  closing = false
  await loadSessions()
  const selectedFromRoute = await ensureSessionFromRoute()
  if (!selectedFromRoute) await ensureSessionFromDoctor()
  if (!active.value && sessions.value.length) await select(sessions.value[0])
  connectWs()
})

watch(() => route.query.sessionId, async (sessionId) => {
  if (!sessionId || !allSessions.value.length) return
  await ensureSessionFromRoute()
})
onBeforeUnmount(() => {
  closing = true
  if (reconnectTimer) clearTimeout(reconnectTimer)
  if (ws) ws.close()
})
</script>

<style scoped>
.doctor-consult-page { min-height: calc(100dvh - 120px); }
.chat-card { height: calc(100dvh - 120px); overflow: hidden; }
.chat-card :deep(.el-card__body) { height: 100%; min-height: 0; display: grid; grid-template-columns: 320px 1fr; padding: 0; }
.sessions { min-height: 0; border-right: 1px solid var(--hda-line); overflow-y: auto; overscroll-behavior: contain; }
.side-head { padding: 18px 20px; border-bottom: 1px solid var(--hda-line); }
.side-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.side-title .history-trigger {
  flex: 0 0 auto;
  min-width: 82px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(47, 163, 124, .24);
  background: #F0FAF6;
  color: #15765C;
  font-weight: 800;
  box-shadow: 0 6px 14px rgba(47, 163, 124, .10);
  transition: transform .2s ease, box-shadow .2s ease, background-color .2s ease, border-color .2s ease, color .2s ease;
}
.side-title .history-trigger:hover,
.side-title .history-trigger:focus {
  border-color: rgba(47, 163, 124, .42);
  background: #DFF5EC;
  color: #0F5F49;
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(47, 163, 124, .16);
}
.side-title .history-trigger:active {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(47, 163, 124, .12);
}
.side-head strong { display: block; font-size: 19px; color: var(--hda-ink); }
.side-head span { font-size: 13px; }
.sessions > button { position: relative; width: 100%; text-align: left; border: 0; background: transparent; padding: 18px 20px; cursor: pointer; border-bottom: 1px solid var(--hda-line); }
.sessions > button.on { background: var(--el-color-primary-light-9); }
.sessions > button.closed { opacity: .72; }
.sessions > button b { display: block; font-size: 17px; color: var(--hda-ink); }
.sessions > button span { display: block; color: var(--hda-ink-soft); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.sessions > button small { display: inline-block; margin-top: 4px; color: #8B9AAF; font-size: 12px; }
.sessions > button em { position: absolute; top: 18px; right: 18px; min-width: 22px; height: 22px; line-height: 22px; text-align: center; background: var(--el-color-danger); color: #fff; font-style: normal; font-size: 12px; }
.session-empty { padding: 28px 20px; color: var(--hda-ink-soft); text-align: center; }
.session-empty p { margin: 0; }
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
.result-panel { margin: 22px auto 4px; width: min(760px, 100%); padding: 18px; border: 1px solid var(--hda-line); background: #fff; box-shadow: var(--hda-shadow-sm); }
.waiting-summary p { margin: 0; color: var(--hda-ink-soft); line-height: 1.7; }
.result-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.result-head strong { color: var(--hda-ink); font-size: 18px; }
.result-grid { display: grid; gap: 12px; }
.result-grid section { padding: 13px 14px; background: #F8FAF7; border: 1px solid rgba(211,222,215,.72); }
.result-grid .risk-section { background: #FFF8ED; border-color: rgba(246, 195, 67, .35); }
.result-grid span, .feedback-top span { display: block; margin-bottom: 5px; color: var(--hda-ink-soft); font-size: 13px; }
.result-grid p { margin: 0; color: var(--hda-ink); line-height: 1.7; white-space: pre-wrap; }
.feedback-form, .feedback-done { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--hda-line); }
.feedback-form { display: grid; gap: 10px; }
.feedback-top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.tag-group { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-group :deep(.el-checkbox-button__inner) { border-left: 1px solid var(--el-border-color); }
.feedback-done { color: var(--hda-ink-soft); }
.feedback-done span { margin-left: 8px; }
.feedback-done p { margin: 8px 0 0; color: var(--hda-ink); }
.medication-panel { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--hda-line); }
.medication-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.medication-head span { color: var(--hda-ink-soft); font-size: 13px; }
.doctor-note { margin: 0 0 10px; color: var(--hda-ink); line-height: 1.7; white-space: pre-wrap; }
.medicine-list { display: grid; gap: 10px; }
.medicine-list section { padding: 12px; border: 1px solid rgba(211,222,215,.72); background: #F8FAF7; }
.medicine-list b { color: var(--hda-ink); }
.medicine-list small { margin-left: 8px; color: #8B9AAF; }
.medicine-list p { margin: 6px 0 0; color: var(--hda-ink); line-height: 1.6; }
.medicine-list em { display: block; margin-top: 6px; color: var(--hda-ink-soft); font-style: normal; line-height: 1.6; }
.confirm-advice { width: 100%; margin-top: 12px; }
.input-row { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--hda-line); }
.input-row .el-input { flex: 1; }
.hello { text-align: center; margin: auto; }
.hello-orb { width: 88px; height: 88px; border-radius: 50%; display: grid; place-items: center; margin: 0 auto 18px; color: #fff; background: linear-gradient(135deg, #37B389, #279470); }
.history-list { display: grid; gap: 10px; max-height: min(60dvh, 560px); overflow-y: auto; padding-right: 4px; }
.history-list button { width: 100%; padding: 14px 15px; border: 1px solid var(--hda-line); background: #fff; text-align: left; cursor: pointer; transition: border-color .2s, background-color .2s, transform .2s; }
.history-list button:hover, .history-list button.on { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); transform: translateY(-1px); }
.history-main { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.history-main b { color: var(--hda-ink); font-size: 16px; }
.history-main small { flex: 0 0 auto; color: #8B9AAF; font-size: 12px; }
.history-sub { display: block; margin-top: 6px; color: var(--hda-ink-soft); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.history-meta { display: flex; align-items: center; gap: 10px; margin-top: 10px; color: #8B9AAF; }
.history-meta em { font-style: normal; font-size: 12px; }
@media (max-width: 900px) { .chat-card :deep(.el-card__body) { grid-template-columns: 1fr; } .sessions { max-height: 220px; border-right: 0; } }
</style>
