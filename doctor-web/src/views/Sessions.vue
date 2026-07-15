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
          <div class="list-actions">
            <el-button class="history-trigger" text type="primary" @click="openHistoryDialog">历史对话</el-button>
            <el-button text :loading="loadingSessions" @click="loadSessions"><el-icon><Refresh /></el-icon></el-button>
          </div>
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
          :class="{ active: active?.session.id === item.session.id, pending: isPendingReport(item) }"
          @click="select(item)"
        >
          <el-avatar :size="42" :src="resolveServerUrl(item.patient?.avatar)">{{ patientInitial(item) }}</el-avatar>
          <span class="session-copy">
            <span class="session-name"><b>{{ item.patient?.nickname || item.patient?.username || '患者' }}</b><time>{{ shortTime(item.session.lastMessageTime) }}</time></span>
            <span class="last-message">{{ isPendingReport(item) ? '患者已结束，待填写咨询报告' : (item.session.lastMessage || '暂无消息') }}</span>
          </span>
          <el-tag v-if="isPendingReport(item)" size="small" type="warning">待报告</el-tag>
          <em v-if="item.session.unreadDoctor">{{ item.session.unreadDoctor }}</em>
        </button>
        <el-empty v-if="!loadingSessions && !sessions.length" description="暂无待处理会话" :image-size="76" />
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
              <el-button v-else-if="!hasDoctorSummary(active)" plain type="primary" @click="openSummaryDialog">填写报告</el-button>
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
            <div v-if="isClosed(active) && !hasDoctorSummary(active)" class="session-result waiting-summary">
              <div class="result-head">
                <strong>待填写咨询报告</strong>
                <el-tag type="warning">患者等待中</el-tag>
              </div>
              <p>会话已结束，请补充本次咨询报告。提交后患者端才会看到结果并进行评价。</p>
              <el-button type="primary" @click="openSummaryDialog">填写报告</el-button>
            </div>
            <div v-else-if="isClosed(active)" class="session-result">
              <div class="result-head">
                <strong>咨询报告</strong>
                <el-tag :type="active.session.recommendOffline ? 'warning' : 'success'">
                  {{ active.session.recommendOffline ? '建议线下就医' : '居家观察' }}
                </el-tag>
              </div>
              <div class="result-copy">
                <section><span>问题概述</span><p>{{ active.session.problemOverview || '未填写问题概述' }}</p></section>
                <section><span>初步判断</span><p>{{ active.session.preliminaryAssessment || '未填写初步判断' }}</p></section>
                <section><span>本次总结</span><p>{{ active.session.summary || '未填写总结' }}</p></section>
                <section><span>处理建议</span><p>{{ active.session.advice || '未填写建议' }}</p></section>
                <section><span>风险提醒</span><p>{{ active.session.riskWarning || '未填写风险提醒' }}</p></section>
              </div>
              <div class="patient-feedback">
                <template v-if="active.session.rating">
                  <span>患者评价</span>
                  <div class="rating-row"><el-rate :model-value="active.session.rating" disabled /><time>{{ shortTime(active.session.feedbackTime) }}</time></div>
                  <div v-if="feedbackTags(active.session.feedbackTags).length" class="feedback-tags">
                    <el-tag v-for="tag in feedbackTags(active.session.feedbackTags)" :key="tag" size="small">{{ tag }}</el-tag>
                  </div>
                  <p v-if="active.session.feedback">{{ active.session.feedback }}</p>
                </template>
                <template v-else>
                  <span>患者评价</span>
                  <p>患者尚未评价本次咨询。</p>
                </template>
              </div>
              <div class="medication-panel">
                <div class="medication-head">
                  <span>用药建议</span>
                  <el-button v-if="!active.session.recommendOffline && medicationAdvice?.status !== 'CONFIRMED'" size="small" type="primary" @click="openMedicationDialog">
                    {{ medicationAdvice ? '编辑建议' : '添加建议' }}
                  </el-button>
                </div>
                <template v-if="medicationAdvice">
                  <div class="advice-status">
                    <el-tag :type="medicationAdvice.status === 'CONFIRMED' ? 'success' : 'warning'">
                      {{ medicationAdvice.status === 'CONFIRMED' ? '患者已确认' : '待患者确认' }}
                    </el-tag>
                    <time v-if="medicationAdvice.patientConfirmTime">{{ shortTime(medicationAdvice.patientConfirmTime) }}</time>
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
                </template>
                <p v-else class="empty-tip">{{ active.session.recommendOffline ? '已建议线下就医，不添加线上用药建议。' : '尚未添加用药建议。' }}</p>
              </div>
            </div>
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

    <el-dialog v-model="historyDialogVisible" title="历史对话" width="660px" class="history-dialog">
      <div v-if="historySessions.length" class="history-list">
        <button v-for="item in historySessions" :key="item.session.id" type="button" :class="{ on: active?.session.id === item.session.id }" @click="selectHistory(item)">
          <span class="history-main">
            <b>{{ item.patient?.nickname || item.patient?.username || '患者' }}</b>
            <small>{{ fullTime(item.session.lastMessageTime || item.session.updateTime || item.session.createTime) }}</small>
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

    <el-dialog v-model="closeDialogVisible" title="填写咨询报告" width="720px">
      <el-form label-position="top">
        <el-form-item label="问题概述">
          <el-input v-model="closeForm.problemOverview" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="用一两句话概括患者本次主要咨询问题" />
        </el-form-item>
        <el-form-item label="初步判断">
          <el-input v-model="closeForm.preliminaryAssessment" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="说明当前倾向判断、风险程度或需要进一步确认的方向" />
        </el-form-item>
        <el-form-item label="本次咨询总结">
          <el-input v-model="closeForm.summary" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="记录关键沟通结论和本次咨询要点" />
        </el-form-item>
        <el-form-item label="处理建议">
          <el-input v-model="closeForm.advice" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="写下复查、监测、生活方式或用药提醒等下一步建议" />
        </el-form-item>
        <el-form-item label="风险提醒">
          <el-input v-model="closeForm.riskWarning" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="提醒患者哪些症状出现时应及时线下就医" />
        </el-form-item>
        <el-checkbox v-model="closeForm.recommendOffline">建议患者线下就医或复诊</el-checkbox>
      </el-form>
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="closingSession" @click="submitCloseSession">提交报告</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="medicationDialogVisible" title="用药建议单" width="860px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="医生说明">
          <el-input v-model="medicationForm.doctorNote" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="说明用药目的、观察重点或需线下就医的情况" />
        </el-form-item>
        <div class="medicine-editor">
          <section v-for="(item, index) in medicationForm.items" :key="index" class="medicine-editor-item">
            <div class="editor-head">
              <b>药品 {{ index + 1 }}</b>
              <el-button v-if="medicationForm.items.length > 1" link type="danger" @click="removeMedicationItem(index)">移除</el-button>
            </div>
            <el-form-item label="选择药品">
              <el-select v-model="item.medicineId" filterable placeholder="搜索并选择药品" style="width: 100%" @change="medicineId => onMedicineChange(item, medicineId)">
                <el-option v-for="medicine in medicineOptions" :key="medicine.id" :label="medicineLabel(medicine)" :value="medicine.id">
                  <span>{{ medicine.name }}</span>
                  <small>{{ medicine.specification }} · {{ medicine.category }}</small>
                </el-option>
              </el-select>
            </el-form-item>
            <div class="editor-grid">
              <el-form-item label="用法"><el-input v-model="item.usageMethod" placeholder="如：口服" /></el-form-item>
              <el-form-item label="剂量"><el-input v-model="item.dosage" placeholder="如：一次1片" /></el-form-item>
              <el-form-item label="频次"><el-input v-model="item.frequency" placeholder="如：每日2次" /></el-form-item>
              <el-form-item label="天数"><el-input-number v-model="item.durationDays" :min="1" /></el-form-item>
              <el-form-item label="数量"><el-input v-model="item.quantity" placeholder="如：1盒" /></el-form-item>
            </div>
            <el-form-item label="注意事项">
              <el-input v-model="item.precautions" type="textarea" :rows="2" maxlength="1000" show-word-limit />
            </el-form-item>
          </section>
        </div>
        <el-button plain type="primary" @click="addMedicationItem">添加药品</el-button>
      </el-form>
      <template #footer>
        <el-button @click="medicationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMedication" @click="submitMedicationAdvice">发送给患者</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Document, Paperclip, Postcard, Promotion, Refresh } from '@element-plus/icons-vue'
import {
  closeSession,
  doctorWsUrl,
  getMedicationAdvice,
  getSessionMessages,
  pageSessions,
  saveMedicationAdvice,
  searchMedicines,
  sendSessionMessage,
  uploadAttachment
} from '@/api'
import { resolveServerUrl } from '@/config/server'

const allSessions = ref([])
const sessions = computed(() => allSessions.value.filter(item => !isClosed(item) || isPendingReport(item)))
const historySessions = computed(() => allSessions.value.filter(item => isClosed(item) && !isPendingReport(item)))
const active = ref(null)
const messages = ref([])
const text = ref('')
const messageBox = ref(null)
const uploading = ref(false)
const loadingSessions = ref(true)
const loadingMessages = ref(false)
const connected = ref(false)
const historyDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const closingSession = ref(false)
const medicationAdvice = ref(null)
const medicationDialogVisible = ref(false)
const savingMedication = ref(false)
const medicineOptions = ref([])
const medicationForm = ref({ doctorNote: '', items: [] })
const closeForm = ref({
  problemOverview: '',
  preliminaryAssessment: '',
  summary: '',
  advice: '',
  riskWarning: '',
  recommendOffline: false
})
let websocket = null
let closing = false
let reconnectTimer = null

async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await pageSessions({ pageNum: 1, pageSize: 50 })
    const records = res.data.records || []
    const activeId = active.value?.session.id
    allSessions.value = records
    if (activeId) active.value = records.find(item => item.session.id === activeId) || active.value
    if (!active.value && sessions.value.length) await select(sessions.value[0])
  } finally { loadingSessions.value = false }
}

async function select(item) {
  active.value = item
  loadingMessages.value = true
  try {
    const res = await getSessionMessages(item.session.id)
    messages.value = res.data || []
    item.session.unreadDoctor = 0
    await loadMedicationAdvice()
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
      markSessionClosed(payload.data)
      ElMessage.info('当前咨询会话已结束')
      await loadSessions()
      openSummaryDialog(payload.data)
      return
    }
    if (payload.type === 'DOCTOR_CONSULT_FEEDBACK') {
      mergeSession(payload.data)
      ElMessage.success('患者提交了本次咨询评价')
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
  openSummaryDialog()
}

function openHistoryDialog() {
  historyDialogVisible.value = true
}

async function selectHistory(item) {
  await select(item)
  historyDialogVisible.value = false
}

function openSummaryDialog(sessionData) {
  if (sessionData?.id) {
    const matched = allSessions.value.find(record => record.session.id === sessionData.id)
    if (matched) active.value = matched
    else if (!active.value || active.value.session.id !== sessionData.id) active.value = { session: sessionData, patient: active.value?.patient }
    mergeSession(sessionData)
  }
  if (!active.value) return
  closeForm.value = {
    problemOverview: active.value.session.problemOverview || '',
    preliminaryAssessment: active.value.session.preliminaryAssessment || '',
    summary: active.value.session.summary || '',
    advice: active.value.session.advice || '',
    riskWarning: active.value.session.riskWarning || '',
    recommendOffline: Boolean(active.value.session.recommendOffline)
  }
  closeDialogVisible.value = true
}

async function submitCloseSession() {
  if (!active.value) return
  closingSession.value = true
  try {
    const problemOverview = closeForm.value.problemOverview.trim()
    const preliminaryAssessment = closeForm.value.preliminaryAssessment.trim()
    const summary = closeForm.value.summary.trim()
    const advice = closeForm.value.advice.trim()
    const riskWarning = closeForm.value.riskWarning.trim()
    if (!problemOverview && !preliminaryAssessment && !summary && !advice && !riskWarning) {
      ElMessage.warning('请至少填写一项咨询报告内容')
      return
    }
    const res = await closeSession(active.value.session.id, {
      problemOverview,
      preliminaryAssessment,
      summary,
      advice,
      riskWarning,
      recommendOffline: closeForm.value.recommendOffline
    })
    active.value = res.data
    markSessionClosed(res.data.session)
    closeDialogVisible.value = false
    await loadSessions()
    await loadMedicationAdvice()
    ElMessage.success('咨询报告已发送给患者')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') throw e
  } finally {
    closingSession.value = false
  }
}

async function loadMedicationAdvice() {
  medicationAdvice.value = null
  if (!active.value || !isClosed(active.value) || !hasDoctorSummary(active.value)) return
  const res = await getMedicationAdvice(active.value.session.id)
  medicationAdvice.value = res.data || null
}

async function openMedicationDialog() {
  if (!active.value) return
  await ensureMedicineOptions()
  medicationForm.value = {
    doctorNote: medicationAdvice.value?.doctorNote || '',
    items: medicationAdvice.value?.items?.length
      ? medicationAdvice.value.items.map(item => ({
        medicineId: item.medicineId,
        usageMethod: item.usageMethod || '',
        dosage: item.dosage || '',
        frequency: item.frequency || '',
        durationDays: item.durationDays || 1,
        quantity: item.quantity || '',
        precautions: item.precautions || ''
      }))
      : [emptyMedicationItem()]
  }
  medicationDialogVisible.value = true
}

async function ensureMedicineOptions() {
  if (medicineOptions.value.length) return
  const res = await searchMedicines({ pageSize: 50 })
  medicineOptions.value = res.data || []
}

function emptyMedicationItem() {
  return { medicineId: null, usageMethod: '', dosage: '', frequency: '', durationDays: 1, quantity: '', precautions: '' }
}

function addMedicationItem() {
  medicationForm.value.items.push(emptyMedicationItem())
}

function removeMedicationItem(index) {
  medicationForm.value.items.splice(index, 1)
}

function onMedicineChange(item, medicineId) {
  const medicine = medicineOptions.value.find(record => record.id === medicineId)
  if (!medicine) return
  item.usageMethod = item.usageMethod || medicine.defaultUsage || ''
  item.dosage = item.dosage || medicine.defaultDosage || ''
  item.frequency = item.frequency || medicine.defaultFrequency || ''
  item.durationDays = item.durationDays || medicine.defaultDurationDays || 1
  item.precautions = item.precautions || medicine.precautions || ''
}

async function submitMedicationAdvice() {
  if (!active.value) return
  savingMedication.value = true
  try {
    const items = medicationForm.value.items
      .filter(item => item.medicineId)
      .map(item => ({
        medicineId: item.medicineId,
        usageMethod: item.usageMethod?.trim(),
        dosage: item.dosage?.trim(),
        frequency: item.frequency?.trim(),
        durationDays: item.durationDays,
        quantity: item.quantity?.trim(),
        precautions: item.precautions?.trim()
      }))
    if (!items.length) {
      ElMessage.warning('请至少选择一种药品')
      return
    }
    const res = await saveMedicationAdvice(active.value.session.id, {
      doctorNote: medicationForm.value.doctorNote?.trim(),
      items
    })
    medicationAdvice.value = res.data
    medicationDialogVisible.value = false
    ElMessage.success('用药建议已发送给患者')
  } finally {
    savingMedication.value = false
  }
}

function mergeSession(sessionData) {
  if (!sessionData?.id) return
  const item = allSessions.value.find(record => record.session.id === sessionData.id)
  if (item) item.session = { ...item.session, ...sessionData }
  if (active.value?.session.id === sessionData.id) active.value.session = { ...active.value.session, ...sessionData }
}

function markSessionClosed(sessionData) {
  const sessionId = typeof sessionData === 'object' ? sessionData?.id : sessionData
  if (!sessionId) return
  const item = allSessions.value.find(record => record.session.id === sessionId)
  if (item) {
    item.session = { ...item.session, ...(typeof sessionData === 'object' ? sessionData : {}), status: 'CLOSED', lastMessage: '[会话已结束]', unreadDoctor: 0 }
  }
  if (active.value?.session.id === sessionId) {
    active.value.session = { ...active.value.session, ...(typeof sessionData === 'object' ? sessionData : {}), status: 'CLOSED', lastMessage: '[会话已结束]', unreadDoctor: 0 }
  }
}

function isClosed(item) { return item?.session?.status === 'CLOSED' }
function isPendingReport(item) { return isClosed(item) && !hasDoctorSummary(item) }
function hasDoctorSummary(item) {
  const session = item?.session
  return Boolean(session?.problemOverview || session?.preliminaryAssessment || session?.summary || session?.advice || session?.riskWarning)
}
function scrollToBottom() { nextTick(() => { if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight }) }
function patientInitial(item) { return (item.patient?.nickname || item.patient?.username || '患').charAt(0) }
function genderText(value) { return value === 1 ? '男' : value === 2 ? '女' : '性别保密' }
function shortTime(value) { return value ? String(value).slice(5, 16).replace('T', ' ') : '' }
function fullTime(value) { return value ? String(value).slice(0, 16).replace('T', ' ') : '未知时间' }
function messageTime(value) { return value ? String(value).slice(11, 16) : '' }
function feedbackTags(value) { return value ? String(value).split(',').filter(Boolean) : [] }
function medicineLabel(medicine) { return [medicine.name, medicine.specification, medicine.category].filter(Boolean).join(' · ') }

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
.list-actions { display: flex; align-items: center; gap: 4px; }
.list-actions .el-button { padding: 0 4px; font-weight: 700; }
.list-actions .history-trigger {
  flex: 0 0 auto;
  min-width: 82px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(46, 111, 224, .24);
  background: #EEF5FF;
  color: #2458B8;
  font-weight: 800;
  box-shadow: 0 6px 14px rgba(46, 111, 224, .10);
  transition: transform .2s ease, box-shadow .2s ease, background-color .2s ease, border-color .2s ease, color .2s ease;
}
.list-actions .history-trigger:hover,
.list-actions .history-trigger:focus {
  border-color: rgba(46, 111, 224, .44);
  background: #DCEAFF;
  color: #1C4695;
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(46, 111, 224, .16);
}
.list-actions .history-trigger:active {
  transform: translateY(0);
  box-shadow: 0 4px 10px rgba(46, 111, 224, .12);
}
.session-item { position: relative; width: 100%; min-height: 82px; padding: 14px 17px; border: 0; border-bottom: 1px solid rgba(223,231,242,.78); background: transparent; display: flex; align-items: center; gap: 11px; text-align: left; cursor: pointer; transition: background-color .2s, transform .25s var(--ease-out); }
.session-item:hover { z-index: 1; background: #F2F7FE; transform: translateX(3px); }
.session-item.active { background: var(--el-color-primary-light-9); box-shadow: inset 3px 0 0 var(--el-color-primary); }
.session-item.pending { background: #FFF8ED; }
.session-item.pending:hover { background: #FFF2D8; }
.session-item .el-avatar { flex: 0 0 auto; color: var(--el-color-primary); background: #fff; font-weight: 700; }
.session-item > .el-tag { flex: 0 0 auto; }
.session-copy { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.session-name { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
.session-name b { overflow: hidden; color: var(--hda-ink); font-size: 15px; text-overflow: ellipsis; white-space: nowrap; }
.session-name time { color: #9AA7B8; font-size: 11px; }
.last-message { overflow: hidden; color: var(--hda-ink-soft); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.session-item.pending .last-message { color: #B7791F; font-weight: 700; }
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
.session-result { margin: 24px auto 2px; width: min(760px, 100%); padding: 18px; border: 1px solid var(--hda-line); background: rgba(255,255,255,.96); box-shadow: 0 8px 22px rgba(31,66,118,.08); }
.waiting-summary p { margin: 0 0 14px; color: var(--hda-ink-soft); line-height: 1.7; }
.result-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 13px; }
.result-head strong { color: var(--hda-ink); font-size: 18px; }
.result-copy { display: grid; gap: 12px; }
.result-copy section { padding: 13px 14px; border: 1px solid rgba(223,231,242,.88); background: #F8FAFD; }
.result-copy span, .patient-feedback > span { display: block; margin-bottom: 5px; color: var(--hda-ink-soft); font-size: 13px; }
.result-copy p, .patient-feedback p { margin: 0; color: var(--hda-ink); line-height: 1.7; white-space: pre-wrap; }
.patient-feedback { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--hda-line); }
.rating-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.rating-row time { color: #9AA7B8; font-size: 12px; }
.feedback-tags { display: flex; flex-wrap: wrap; gap: 7px; margin: 8px 0; }
.medication-panel { margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--hda-line); }
.medication-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.medication-head span { color: var(--hda-ink-soft); font-size: 13px; }
.advice-status { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.advice-status time { color: #8B9AAF; font-size: 12px; }
.doctor-note { margin: 0 0 10px; color: var(--hda-ink); line-height: 1.7; white-space: pre-wrap; }
.medicine-list { display: grid; gap: 10px; }
.medicine-list section { padding: 12px; border: 1px solid rgba(223,231,242,.88); background: #fff; }
.medicine-list b { color: var(--hda-ink); }
.medicine-list small { margin-left: 8px; color: #8B9AAF; }
.medicine-list p { margin: 6px 0 0; color: var(--hda-ink); }
.medicine-list em { display: block; margin-top: 6px; color: var(--hda-ink-soft); font-style: normal; line-height: 1.6; }
.empty-tip { margin: 0; color: var(--hda-ink-soft); }
.medicine-editor { display: grid; gap: 12px; margin-bottom: 12px; }
.medicine-editor-item { padding: 14px; border: 1px solid var(--hda-line); background: #F8FAFD; }
.editor-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; color: var(--hda-ink); }
.editor-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.el-select-dropdown__item small { margin-left: 8px; color: #8B9AAF; }
.composer { min-height: 76px; padding: 13px 18px; border-top: 1px solid var(--hda-line); display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,.88); }
.composer .el-input { flex: 1; }
.icon-button { width: 48px; padding: 0; font-size: 20px; }
.chat-empty { margin: auto; padding: 30px; text-align: center; }
.chat-empty > span { width: 78px; height: 78px; margin: 0 auto 18px; display: grid; place-items: center; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 36px; }
.chat-empty h2 { margin: 0; color: var(--hda-ink); font-size: 23px; }
.chat-empty p { margin: 7px 0 0; color: var(--hda-ink-soft); }
.history-list { display: grid; gap: 10px; max-height: min(60dvh, 560px); overflow-y: auto; padding-right: 4px; }
.history-list button { width: 100%; padding: 14px 15px; border: 1px solid var(--hda-line); background: #fff; text-align: left; cursor: pointer; transition: border-color .2s, background-color .2s, transform .2s; }
.history-list button:hover, .history-list button.on { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); transform: translateY(-1px); }
.history-main { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.history-main b { color: var(--hda-ink); font-size: 16px; }
.history-main small { flex: 0 0 auto; color: #8B9AAF; font-size: 12px; }
.history-sub { display: block; margin-top: 6px; color: var(--hda-ink-soft); overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.history-meta { display: flex; align-items: center; gap: 10px; margin-top: 10px; color: #8B9AAF; }
.history-meta em { font-style: normal; font-size: 12px; }
@media (max-width: 820px) { .chat-shell { height: auto; min-height: 700px; } .chat-shell :deep(.el-card__body) { grid-template-columns: 1fr; grid-template-rows: 230px 1fr; } .session-list { border-right: 0; border-bottom: 1px solid var(--hda-line); } .message-wrap { max-width: 82%; } }
@media (max-width: 560px) { .chat-head { padding: 0 14px; } .chat-head > .el-button { width: 44px; padding: 0; font-size: 0; } .chat-head > .el-button .el-icon { margin: 0; font-size: 17px; } .messages { padding: 16px 12px; } .composer { padding: 10px; } .composer > .el-button { width: 48px; padding: 0; font-size: 0; } .composer > .el-button .el-icon { margin: 0; font-size: 18px; } .editor-grid { grid-template-columns: 1fr; } }
</style>
