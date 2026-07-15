<template>
  <div class="chat-shell">
    <aside class="session-panel">
      <div class="assistant-brand">
        <div class="brand-mark"><el-icon><ChatDotRound /></el-icon></div>
        <div><strong>健康助手</strong><span>Health Copilot</span></div>
      </div>
      <button class="new-session" @click="newConversation">
        <el-icon><Plus /></el-icon><span>开启新对话</span><kbd>⌘ K</kbd>
      </button>
      <div class="history-label">历史对话</div>
      <div class="session-list">
        <button v-for="item in sessions" :key="item.id" class="session-item"
                :class="{ active: item.id === sessionId }" @click="selectSession(item.id)">
          <span class="session-title">{{ item.title }}</span>
          <span class="session-time">{{ item.time || '最近' }}</span>
        </button>
        <div v-if="!sessions.length" class="empty-session">暂无历史对话</div>
      </div>
    </aside>

    <section class="chat-main">
      <div class="chat-head">
        <div class="bot"><el-icon :size="20"><ChatDotRound /></el-icon></div>
        <div>
          <div class="bot-name">健康助手</div>
          <div class="bot-sub"><span class="live"></span>在线 · 回答仅供健康参考</div>
        </div>
        <div class="profile-consent" :class="{ enabled: useHealthProfile }">
          <div><strong>结合我的健康档案</strong><span>仅本次咨询按最小范围读取</span></div>
          <el-switch v-model="useHealthProfile" :disabled="loading" @change="saveProfilePreference" />
        </div>
      </div>

      <div class="messages" ref="msgBox" @scroll="handleMessagesScroll">
        <transition-group name="msg" tag="div">
          <div v-for="(message, index) in messages" :key="index" :class="['msg', message.role]">
            <div v-if="message.role === 'assistant'" class="face" :class="{ working: isCoordinating(message, index) }">
              <span v-if="isCoordinating(message, index)" class="agent-loader" aria-label="智能体正在协作"></span>
              <el-icon v-else><Avatar /></el-icon>
            </div>
            <div class="assistant-response">
              <section v-if="message.role === 'assistant' && message.stages?.length" class="agent-progress"
                       :class="{ collapsed: message.content && !message.progressExpanded }">
                <button class="progress-summary" type="button" :aria-expanded="String(!message.content || message.progressExpanded)"
                        @click="message.progressExpanded = !message.progressExpanded">
                  <span class="progress-indicator" :class="progressState(message)"></span>
                  <span>{{ progressSummary(message) }}</span>
                  <span class="progress-toggle" aria-hidden="true">{{ !message.content || message.progressExpanded ? '收起' : '查看' }}</span>
                </button>
                <transition name="progress-details">
                  <div v-if="!message.content || message.progressExpanded" class="progress-details">
                    <div v-for="stage in message.stages" :key="stage.stage" class="progress-row" :class="stage.status?.toLowerCase()">
                      <span class="progress-dot"></span><strong>{{ stageName(stage.stage) }}</strong><span>{{ stage.content }}</span>
                    </div>
                  </div>
                </transition>
              </section>
              <div class="bubble" :class="{ 'is-streaming': streaming && loading && index === messages.length - 1 && message.role === 'assistant' }"
                   v-html="message.role === 'assistant' ? renderMarkdown(message.content) : escapeHtml(message.content)"></div>
              <section v-if="message.role === 'assistant' && message.intakeQuestion" class="intake-card"
                       :class="{ answered: message.intakeAnswered }">
                <div class="intake-card-title">问诊补充</div>
                <strong>{{ message.intakeQuestion.prompt }}</strong>
                <div class="intake-options">
                  <button v-for="option in message.intakeQuestion.options" :key="option" type="button"
                          :disabled="loading || message.intakeAnswered" @click="answerIntake(message, option)">
                    {{ option }}
                  </button>
                </div>
                <div v-if="message.intakeQuestion.allowFreeText" class="intake-free">
                  <el-input v-model="message.intakeDraft" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }"
                            resize="none" placeholder="以上选项不合适？请自由填写…"
                            :disabled="loading || message.intakeAnswered"
                            @keydown.enter.exact.prevent="answerIntake(message, message.intakeDraft)" />
                  <button type="button" :disabled="loading || message.intakeAnswered || !message.intakeDraft?.trim()"
                          @click="answerIntake(message, message.intakeDraft)">提交回答</button>
                </div>
                <div v-if="message.intakeAnswered" class="intake-selected">已回答：{{ message.selectedIntakeAnswer }}</div>
              </section>
              <section v-if="message.role === 'assistant' && message.recommendedDoctors?.length" class="doctor-recommendations">
                <div class="doctor-recommendations-head">
                  <div><strong>为你匹配的医生</strong><span>匹配度仅表示资料与当前诉求的相关程度</span></div>
                  <button type="button" @click="viewAllDoctors">查看全部医生</button>
                </div>
                <article v-for="doctor in message.recommendedDoctors" :key="doctor.doctorId" class="recommended-doctor">
                  <div class="recommended-doctor-main">
                    <el-avatar :size="48" :src="resolveServerUrl(doctor.avatar)">{{ doctor.name?.charAt(0) || '医' }}</el-avatar>
                    <div class="recommended-doctor-info">
                      <div class="recommended-doctor-title"><strong>{{ doctor.name }}</strong><span>{{ doctor.title }}</span></div>
                      <p>{{ doctor.hospital }} · {{ doctor.department }}</p>
                    </div>
                    <div class="match-score"><strong>{{ doctor.matchScore }}</strong><span>匹配度</span></div>
                  </div>
                  <p class="recommended-speciality"><b>擅长</b>{{ doctor.speciality || '暂无详细说明' }}</p>
                  <div class="recommend-reasons"><span v-for="reason in doctor.reasons" :key="reason">{{ reason }}</span></div>
                  <button class="consult-doctor-button" type="button" @click="startDoctorConsult(doctor)">向 TA 咨询</button>
                </article>
              </section>
              <div v-if="message.role === 'assistant' && (message.riskLevel || message.usedProfileCategories?.length)" class="answer-meta">
                <span v-if="message.riskLevel" class="risk-pill" :class="String(message.riskLevel).toLowerCase()">
                  <span class="risk-dot" aria-hidden="true"></span><span class="risk-prefix">风险评估</span><span>{{ riskLabel(message.riskLevel) }}</span>
                </span>
                <span v-if="message.usedProfileCategories?.length" class="profile-used">已参考：{{ message.usedProfileCategories.join('、') }}</span>
              </div>
            </div>
          </div>
        </transition-group>

        <div v-if="!messages.length && !loading" class="hello">
          <div class="hello-orb"><el-icon :size="40"><ChatDotRound /></el-icon></div>
          <h3>您好，有什么健康问题想咨询？</h3>
          <div class="samples"><span v-for="sample in samples" :key="sample" @click="askSample(sample)">{{ sample }}</span></div>
        </div>
      </div>

      <div class="composer-wrap">
        <div class="input-row">
          <el-input v-model="text" type="textarea" :autosize="{ minRows: 1, maxRows: 5 }"
                    resize="none" placeholder="向健康助手提问…" @keydown.enter.exact.prevent="send" :disabled="loading" />
          <button class="send-button" :class="{ ready: text.trim() }" :disabled="loading || !text.trim()" @click="send">
            <el-icon v-if="!loading"><Promotion /></el-icon>
            <span v-else class="send-loading"></span>
          </button>
        </div>
        <div class="composer-tip">Enter 发送 · Shift + Enter 换行 · 健康助手不替代医生诊断</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Avatar, Promotion, Plus } from '@element-plus/icons-vue'
import { consultChatStream, consultHistory, consultSessions, startAlertHandling } from '@/api'
import { resolveServerUrl } from '@/config/server'

const route = useRoute()
const router = useRouter()
const messages = ref([])
const sessions = ref([])
const text = ref('')
const loading = ref(false)
const streaming = ref(false)
const useHealthProfile = ref(localStorage.getItem('health-assistant-use-profile') === 'true')
const sessionId = ref('')
const msgBox = ref(null)
let abortController = null
let scrollFrame = 0
let stageQueueTimer = 0
let alertLinked = false
const stageQueue = []
const shouldFollowLatest = ref(true)
const samples = ['最近血压有点高怎么办？', '老年人补钙吃什么好？', '血糖偏高饮食注意什么？']

function sessionStorageKey() {
  const user = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return `health-assistant-session:${user.userId || user.username || 'guest'}`
}

function saveSession(id) {
  if (!id) return
  sessionId.value = id
  localStorage.setItem(sessionStorageKey(), id)
}

async function refreshSessions() {
  const response = await consultSessions()
  sessions.value = (response.data || []).map(item => ({
    id: item.sessionId,
    title: item.title || '新对话',
    time: item.updateTime ? String(item.updateTime).replace('T', ' ').slice(0, 16) : ''
  }))
}

async function selectSession(id) {
  if (!id || loading.value) return
  const response = await consultHistory({ sessionId: id, pageNum: 1, pageSize: 100 })
  messages.value = (response.data.records || []).map(record => ({
    role: record.role,
    content: record.content,
    riskLevel: record.riskLevel,
    usedProfileCategories: record.usedProfileCategories || [],
    recommendedDoctors: record.recommendedDoctors || [],
    traceId: record.traceId
  }))
  saveSession(id)
  scroll(true)
}

async function loadHistory() {
  await refreshSessions()
  const saved = localStorage.getItem(sessionStorageKey())
  const target = sessions.value.some(item => item.id === saved) ? saved : sessions.value[0]?.id
  if (target) await selectSession(target)
}

function newConversation() {
  if (loading.value) return
  sessionId.value = ''
  messages.value = []
  localStorage.removeItem(sessionStorageKey())
  text.value = ''
}

function askSample(sample) {
  text.value = sample
  send()
}

onMounted(async () => {
  try { await loadHistory() } catch (error) { ElMessage.error('历史对话加载失败') }
  if (typeof route.query.q === 'string' && route.query.q.trim()) {
    if (route.query.alertId) newConversation()
    text.value = route.query.q.trim()
  }
})

async function send(overrideMessage = '') {
  const supplied = typeof overrideMessage === 'string' ? overrideMessage.trim() : ''
  if ((!supplied && !text.value.trim()) || loading.value) return false
  const question = supplied || text.value.trim()
  if (!supplied) text.value = ''
  const pendingIntake = [...messages.value].reverse().find(item => item.role === 'assistant' && item.intakeQuestion && !item.intakeAnswered)
  if (pendingIntake) {
    pendingIntake.intakeAnswered = true
    pendingIntake.selectedIntakeAnswer = question
  }
  messages.value.push({ role: 'user', content: question })
  const assistantIndex = messages.value.length
  messages.value.push({
    role: 'assistant', content: '', usedProfileCategories: [], recommendedDoctors: [], riskLevel: '',
    stages: [{ stage: 'SAFETY_CHECK', status: 'RUNNING', content: '正在检查紧急风险信号' }], progressExpanded: true
  })
  loading.value = true
  streaming.value = false
  abortController = new AbortController()
  scroll(true)
  let succeeded = false

  try {
    await consultChatStream(
      { message: question, sessionId: sessionId.value || null, useHealthProfile: useHealthProfile.value },
      {
        signal: abortController.signal,
        onMeta: event => {
          saveSession(event.sessionId)
          if (route.query.alertId && !alertLinked) {
            alertLinked = true
            startAlertHandling(route.query.alertId, { channel: 'HEALTH_ASSISTANT', sessionId: event.sessionId })
              .catch(() => { alertLinked = false })
          }
          messages.value[assistantIndex].traceId = event.traceId
          messages.value[assistantIndex].usedProfileCategories = event.usedProfileCategories || []
        },
        onStage: event => {
          queueStage(assistantIndex, event)
        },
        onRisk: event => { messages.value[assistantIndex].riskLevel = event.riskLevel },
        onIntake: intakeQuestion => {
          const assistant = messages.value[assistantIndex]
          if (!assistant || !intakeQuestion) return
          assistant.intakeQuestion = intakeQuestion
          assistant.intakeDraft = ''
          assistant.intakeAnswered = false
        },
        onDoctorRecommendations: doctors => {
          const assistant = messages.value[assistantIndex]
          if (assistant) assistant.recommendedDoctors = doctors || []
        },
        onDelta: content => {
          const assistant = messages.value[assistantIndex]
          if (!assistant) return
          streaming.value = true
          if (!assistant.content && !stageQueueTimer && !hasQueuedStages(assistantIndex)) assistant.progressExpanded = false
          assistant.content += content
          scroll()
        },
        onDone: () => { streaming.value = false }
      }
    )
    if (!messages.value[assistantIndex]?.content) throw new Error('健康助手没有返回内容')
    await refreshSessions()
    succeeded = true
  } catch (error) {
    const partial = messages.value[assistantIndex]?.content
    if (partial) {
      messages.value[assistantIndex].content += '\n\n本次生成已中断，以上为已接收内容。'
      ElMessage.warning(error.name === 'AbortError' ? '已停止生成，已保留现有回答' : '连接中断，已保留现有回答')
    } else {
      messages.value.splice(assistantIndex, 1)
      ElMessage.error(error.name === 'AbortError' ? '已停止生成' : (error.message || '发送失败'))
    }
  } finally {
    abortController = null
    streaming.value = false
    loading.value = false
    scroll()
  }
  if (!succeeded && pendingIntake) {
    pendingIntake.intakeAnswered = false
    pendingIntake.selectedIntakeAnswer = ''
  }
  return succeeded
}

async function answerIntake(message, rawAnswer) {
  if (loading.value || message.intakeAnswered) return
  const answer = String(rawAnswer || '').trim()
  if (!answer) return
  message.intakeAnswered = true
  message.selectedIntakeAnswer = answer
  const succeeded = await send(answer)
  if (!succeeded) {
    message.intakeAnswered = false
    message.selectedIntakeAnswer = ''
  }
}

function saveProfilePreference(value) {
  localStorage.setItem('health-assistant-use-profile', String(Boolean(value)))
}

function startDoctorConsult(doctor) {
  if (!doctor?.doctorId) return
  router.push({
    path: doctor.action?.route || '/doctor-consult',
    query: { doctorId: doctor.doctorId, healthAssistantSessionId: sessionId.value }
  })
}

function viewAllDoctors() {
  router.push({ path: '/doctor', query: { healthAssistantSessionId: sessionId.value } })
}

function stageName(stage) {
  return ({ SAFETY_CHECK: '安全分诊', CLARIFYING: '问诊补充', ROUTING: '任务调度', CONSULTING: '咨询分析', RETRIEVING: '权威检索', MATCHING_DOCTORS: '医生匹配', SYNTHESIZING: '整合回答' })[stage] || stage
}

function progressState(message) {
  const stages = message.stages || []
  if (stages.some(stage => stage.status === 'RUNNING')) return 'running'
  if (stages.some(stage => stage.status === 'DEGRADED')) return 'degraded'
  return 'completed'
}

function progressSummary(message) {
  const stages = message.stages || []
  const active = stages.find(stage => stage.status === 'RUNNING')
  if (active) return `正在${stageName(active.stage)}`
  return `已完成 ${stages.length} 个协作步骤`
}

function isCoordinating(message, index) {
  return loading.value && index === messages.value.length - 1 && !message.content
}

function hasQueuedStages(assistantIndex) {
  return stageQueue.some(item => item.assistantIndex === assistantIndex)
}

function queueStage(assistantIndex, event) {
  stageQueue.push({ assistantIndex, event })
  if (!stageQueueTimer) revealNextStage()
}

function revealNextStage() {
  const next = stageQueue.shift()
  if (!next) {
    stageQueueTimer = 0
    return
  }
  const assistant = messages.value[next.assistantIndex]
  if (assistant) {
    const existing = assistant.stages.findIndex(item => item.stage === next.event.stage)
    if (existing >= 0) assistant.stages[existing] = next.event
    else assistant.stages.push(next.event)
    if (assistant.content) assistant.progressExpanded = true
    scroll()
  }
  stageQueueTimer = window.setTimeout(() => {
    if (!hasQueuedStages(next.assistantIndex)) {
      const completed = messages.value[next.assistantIndex]
      if (completed?.content) completed.progressExpanded = false
    }
    revealNextStage()
  }, 440)
}

function riskLabel(level) {
  return ({ LOW: '一般咨询', MEDIUM: '建议关注', HIGH: '尽快就医', EMERGENCY: '立即就医' })[level] || level
}

function escapeHtml(value) {
  return String(value || '').replace(/[&<>'"]/g, char => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[char])
}

function renderInline(value) {
  return escapeHtml(value)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\[资料\d+]/g, '')
    // 流式片段可能暂时只收到一侧星号，不把 Markdown 符号直接暴露给用户。
    .replace(/\*\*/g, '')
}

function renderMarkdown(content) {
  const lines = String(content || '').replace(/\r\n?/g, '\n').split('\n')
  let html = ''
  let listType = ''
  const closeList = () => {
    if (listType) html += `</${listType}>`
    listType = ''
  }

  lines.forEach(line => {
    const boldHeading = line.match(/^\s*\*\*(.+?)\*\*\s*$/)
    const hashHeading = line.match(/^\s{0,3}#{1,6}\s+(.+?)\s*#*\s*$/)
    const heading = boldHeading || hashHeading
    const ordered = line.match(/^\s*\d+\.\s+(.+)$/)
    const bullet = line.match(/^\s*[-*]\s+(.+)$/)
    if (heading && heading[1].trim() === '资料依据') {
      closeList()
    } else if (heading) {
      closeList()
      html += `<h3>${renderInline(heading[1])}</h3>`
    } else if (ordered || bullet) {
      const type = ordered ? 'ol' : 'ul'
      if (type !== listType) {
        closeList()
        listType = type
        html += `<${type}>`
      }
      html += `<li>${renderInline((ordered || bullet)[1])}</li>`
    } else if (!line.trim()) {
      closeList()
      html += '<div class="md-spacer"></div>'
    } else if (/^\s*\[资料\d+]/.test(line)) {
      closeList()
    } else {
      closeList()
      html += `<p>${renderInline(line)}</p>`
    }
  })
  closeList()
  return html
}

function handleMessagesScroll() {
  const container = msgBox.value
  if (!container) return
  // 仅在接近底部时跟随流式内容，用户向上浏览时保留当前位置。
  shouldFollowLatest.value = container.scrollHeight - container.scrollTop - container.clientHeight < 32
}

function scroll(force = false) {
  if (force) shouldFollowLatest.value = true
  if (!shouldFollowLatest.value || scrollFrame) return
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = 0
    nextTick(() => {
      const container = msgBox.value
      if (container && shouldFollowLatest.value) container.scrollTop = container.scrollHeight
    })
  })
}

onBeforeUnmount(() => {
  abortController?.abort()
  if (scrollFrame) cancelAnimationFrame(scrollFrame)
  if (stageQueueTimer) clearTimeout(stageQueueTimer)
  stageQueue.splice(0)
})
</script>

<style scoped>
.chat-shell { height: calc(100vh - 120px); min-height: 620px; max-width: 1384px; margin: 2px auto; display: flex; overflow: hidden; border: 1px solid rgba(255,255,255,.8); border-radius: 14px; background: rgba(255,255,255,.82); box-shadow: 0 12px 36px rgba(46,111,224,.12); }
.session-panel { width: 274px; flex-shrink: 0; display: flex; flex-direction: column; padding: 22px 14px; background: linear-gradient(180deg, rgba(245,249,255,.96), rgba(238,246,255,.78)); border-right: 1px solid #e3edf9; }
.assistant-brand { display: flex; align-items: center; gap: 11px; padding: 1px 9px 22px; }.brand-mark { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); box-shadow: 0 8px 18px rgba(46,111,224,.25); }.assistant-brand strong, .assistant-brand span { display: block; }.assistant-brand strong, .bot-name { color: #21314d; font-family: var(--hda-font-display); font-weight: 700; }.assistant-brand strong { font-size: 15px; }.assistant-brand span { margin-top: 3px; color: #9aa9be; font-size: 10px; letter-spacing: .1em; }
.new-session { width: 100%; display: flex; align-items: center; gap: 9px; padding: 12px; border: 1px solid #cfe0fb; border-radius: 10px; color: #2e6fe0; background: #fff; cursor: pointer; transition: transform .2s ease, box-shadow .2s ease, background .2s ease; }.new-session:hover { background: #f4f8fe; box-shadow: 0 8px 18px rgba(46,111,224,.12); transform: translateY(-1px); }.new-session span { flex: 1; text-align: left; font-weight: 700; }.new-session kbd { color: #aebbd0; font: 10px/1 monospace; }
.history-label { padding: 27px 10px 9px; color: #9aa9be; font-size: 11px; font-weight: 700; letter-spacing: .1em; }.session-list { min-height: 0; overflow-y: auto; scrollbar-width: thin; }.session-item { position: relative; width: 100%; display: block; padding: 11px 12px; margin-bottom: 3px; text-align: left; border: 0; border-radius: 9px; color: #5c6e87; background: transparent; cursor: pointer; transition: background .18s ease, color .18s ease; }.session-item:hover { background: rgba(255,255,255,.82); color: #2e6fe0; }.session-item.active { color: #2e6fe0; background: #eaf2ff; }.session-item.active::before { content: ''; position: absolute; left: 0; top: 11px; bottom: 11px; width: 3px; border-radius: 4px; background: #2e6fe0; }.session-title { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 700; }.session-time, .empty-session { display: block; margin-top: 5px; color: #a5b2c6; font-size: 10px; }.empty-session { padding: 32px 8px; text-align: center; }
.chat-main { min-width: 0; flex: 1; display: flex; flex-direction: column; background: rgba(255,255,255,.9); }.chat-head { display: flex; align-items: center; gap: 11px; padding: 16px 26px; border-bottom: 1px solid #e7effa; background: rgba(255,255,255,.78); }.bot, .face { display: grid; place-items: center; border-radius: 11px; }.bot { width: 40px; height: 40px; color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); }.bot-name { font-size: 15px; }.bot-sub { display: flex; align-items: center; gap: 6px; margin-top: 2px; color: #8fa0b8; font-size: 11px; }.live { width: 6px; height: 6px; border-radius: 50%; background: #4bb78b; box-shadow: 0 0 0 3px #e5f6ef; }.profile-consent { margin-left: auto; display: flex; align-items: center; gap: 14px; padding: 8px 12px; border: 1px solid #e0eaf8; border-radius: 10px; background: #f7faff; transition: .2s ease; }.profile-consent.enabled { border-color: #bfd5f6; background: #edf4ff; }.profile-consent strong, .profile-consent span { display: block; }.profile-consent strong { color: #465a76; font-size: 12px; }.profile-consent span { margin-top: 2px; color: #9aa9be; font-size: 10px; }
.messages { flex: 1; overflow-y: auto; padding: 28px max(30px, calc((100% - 860px) / 2)); scroll-behavior: smooth; }.msg { display: flex; align-items: flex-start; gap: 11px; margin: 18px 0; }.msg.user { justify-content: flex-end; }.face { width: 32px; height: 32px; flex-shrink: 0; color: #2e6fe0; background: #eaf2ff; }.assistant-response { max-width: min(79%, 700px); }.bubble { max-width: min(79%, 700px); padding: 2px; color: #344862; font-size: 15px; line-height: 1.8; }.assistant-response .bubble { max-width: none; }.bubble :deep(p) { margin: 0; }.bubble :deep(.md-spacer) { height: 14px; }.bubble :deep(h3) { margin: 20px 0 7px; color: #263b59; font-size: 16px; font-weight: 700; line-height: 1.55; }.bubble :deep(h3:first-child) { margin-top: 0; }.bubble :deep(ol), .bubble :deep(ul) { margin: 7px 0 0; padding-left: 1.55em; }.bubble :deep(li) { margin: 4px 0; padding-left: 3px; }.bubble :deep(strong) { color: #263b59; font-weight: 700; }.bubble.is-streaming::after { content: ''; display: inline-block; width: 2px; height: 1.05em; margin-left: 4px; vertical-align: -.12em; background: #2e6fe0; animation: stream-caret .85s steps(1) infinite; }@keyframes stream-caret { 50% { opacity: 0; } }.msg.user .bubble { padding: 11px 15px; color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); border-radius: 12px 12px 3px 12px; box-shadow: 0 7px 17px rgba(46,111,224,.18); }.msg.user .bubble :deep(*) { color: inherit; }.msg.user .assistant-response { display: flex; justify-content: flex-end; }
.agent-progress { margin: 0 0 14px; border-bottom: 1px solid #dbe7f7; color: #46617f; }.progress-summary { width: 100%; display: flex; align-items: center; gap: 8px; padding: 0 0 10px; border: 0; color: #2e6fe0; background: transparent; cursor: pointer; font: 700 12px/1.4 var(--hda-font-display); text-align: left; }.progress-summary:focus-visible { outline: 2px solid #2e6fe0; outline-offset: 3px; }.progress-indicator { width: 8px; height: 8px; flex: 0 0 8px; border-radius: 50%; background: #4b7be0; }.progress-indicator.running, .progress-row.running .progress-dot { animation: progress-pulse 1.4s ease-in-out infinite; }.progress-indicator.degraded, .progress-row.degraded .progress-dot { background: #f08b3b; }.progress-indicator.completed { background: #4b7be0; }.progress-toggle { margin-left: auto; color: #8fa0b8; font-size: 11px; font-weight: 500; }.progress-details { display: grid; gap: 2px; padding: 1px 0 12px; }.progress-row { display: grid; grid-template-columns: 8px 72px minmax(0,1fr); align-items: start; gap: 8px; padding: 5px 0; color: #7890ac; font-size: 11px; line-height: 1.55; }.progress-row strong { color: #506887; font-size: 11px; }.progress-dot { width: 6px; height: 6px; margin-top: 5px; border-radius: 50%; background: #4b7be0; }@keyframes progress-pulse { 50% { opacity: .4; transform: scale(.72); } }.progress-details-enter-active, .progress-details-leave-active { transition: opacity .18s ease, transform .18s ease; }.progress-details-enter-from, .progress-details-leave-to { opacity: 0; transform: translateY(-4px); }.msg-enter-active { transition: opacity .22s ease, transform .22s ease; }.msg-enter-from { opacity: 0; transform: translateY(8px); }
.answer-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 10px; }.risk-pill { padding: 4px 8px; border-radius: 999px; color: #436998; background: #edf4ff; font-size: 11px; font-weight: 700; }.risk-pill.medium { color: #9a641d; background: #fff2d9; }.risk-pill.high, .risk-pill.emergency { color: #a63a31; background: #fde7e4; }.profile-used { color: #7b91ac; font-size: 11px; }.citations { margin-top: 14px; padding-top: 12px; border-top: 1px solid #e2ebf7; }.citation-title { margin-bottom: 7px; color: #7188a5; font-size: 11px; font-weight: 700; letter-spacing: .08em; }.citation-card { display: flex; align-items: flex-start; gap: 9px; padding: 9px 10px; margin-top: 6px; border-radius: 9px; color: #3b5f91; background: #f4f8fe; text-decoration: none; transition: background .18s ease, transform .18s ease; }.citation-card:hover { background: #eaf2ff; transform: translateX(2px); }.citation-index { width: 20px; height: 20px; display: grid; place-items: center; flex-shrink: 0; border-radius: 6px; color: #fff; background: #2e6fe0; font-size: 10px; }.citation-card strong, .citation-card small { display: block; }.citation-card strong { font-size: 12px; }.citation-card small { margin-top: 2px; color: #8ba0ba; font-size: 10px; }
.intake-card { margin-top: 14px; padding: 16px; border: 1px solid #b9d2f5; border-radius: 14px; background: linear-gradient(145deg, #f8fbff, #edf4ff); }.intake-card-title { margin-bottom: 7px; color: #245dbb; font-size: 12px; font-weight: 800; }.intake-card > strong { display: block; color: #294463; font-size: 14px; line-height: 1.55; }.intake-options { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 13px; }.intake-options button, .intake-free > button { min-height: 42px; padding: 9px 12px; border: 1px solid #9ebfea; border-radius: 9px; color: #2459a5; background: #fff; cursor: pointer; font-size: 13px; transition: background .18s ease, border-color .18s ease, transform .18s ease; }.intake-options button:hover:not(:disabled), .intake-free > button:hover:not(:disabled) { border-color: #5f92da; background: #eaf2ff; transform: translateY(-1px); }.intake-options button:focus-visible, .intake-free > button:focus-visible { outline: 2px solid #2e6fe0; outline-offset: 2px; }.intake-options button:disabled, .intake-free > button:disabled { opacity: .55; cursor: not-allowed; }.intake-free { display: grid; grid-template-columns: minmax(0,1fr) auto; align-items: end; gap: 8px; margin-top: 12px; }.intake-free :deep(.el-textarea__inner) { min-height: 64px !important; border-color: #afc9ed; color: #294463; box-shadow: none; }.intake-free :deep(.el-textarea__inner::placeholder) { color: #667b96; }.intake-selected { margin-top: 11px; color: #536b88; font-size: 13px; }.intake-card.answered .intake-options, .intake-card.answered .intake-free { display: none; }
.doctor-recommendations { display: grid; gap: 10px; margin-top: 16px; }.doctor-recommendations-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.doctor-recommendations-head strong, .doctor-recommendations-head span { display: block; }.doctor-recommendations-head strong { color: #263b59; font-size: 15px; }.doctor-recommendations-head span { margin-top: 3px; color: #7b91ac; font-size: 11px; }.doctor-recommendations-head button { border: 0; color: #2e6fe0; background: transparent; cursor: pointer; font-weight: 700; }.recommended-doctor { padding: 15px; border: 1px solid #d5e4f7; border-radius: 13px; background: #fff; box-shadow: 0 7px 18px rgba(46,111,224,.08); }.recommended-doctor-main { display: flex; align-items: center; gap: 11px; }.recommended-doctor-info { min-width: 0; flex: 1; }.recommended-doctor-title { display: flex; align-items: center; gap: 8px; }.recommended-doctor-title strong { color: #263b59; font-size: 16px; }.recommended-doctor-title span { padding: 2px 7px; border-radius: 6px; color: #245dbb; background: #edf4ff; font-size: 11px; }.recommended-doctor-info p { margin: 3px 0 0; overflow: hidden; color: #7188a5; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.match-score { min-width: 52px; text-align: center; }.match-score strong, .match-score span { display: block; }.match-score strong { color: #2e6fe0; font-size: 20px; }.match-score span { color: #8fa0b8; font-size: 10px; }.recommended-speciality { margin: 11px 0 8px; color: #536b88; font-size: 12px; line-height: 1.6; }.recommended-speciality b { margin-right: 7px; color: #294463; }.recommend-reasons { display: flex; flex-wrap: wrap; gap: 6px; }.recommend-reasons span { padding: 3px 7px; border-radius: 6px; color: #526f95; background: #f2f6fb; font-size: 10px; }.consult-doctor-button { width: 100%; min-height: 40px; margin-top: 12px; border: 0; border-radius: 9px; color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); cursor: pointer; font-weight: 700; }.consult-doctor-button:focus-visible { outline: 2px solid #2e6fe0; outline-offset: 2px; }
.hello { padding: 10vh 0 0; text-align: center; }.hello-orb { width: 72px; height: 72px; display: grid; place-items: center; margin: 0 auto 20px; border-radius: 16px; color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); box-shadow: 0 14px 30px rgba(46,111,224,.24); }.hello h3 { color: #263b59; font-family: var(--hda-font-display); font-size: 22px; }.samples { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }.samples span { padding: 9px 14px; border: 1px solid #d4e3f8; border-radius: 9px; color: #4672b9; background: #fff; cursor: pointer; transition: .2s; }.samples span:hover { border-color: #82aceb; background: #f4f8fe; transform: translateY(-1px); }
.composer-wrap { padding: 14px max(30px, calc((100% - 860px) / 2)) 16px; background: linear-gradient(180deg, rgba(255,255,255,.4), #fff 32%); }.input-row { display: flex; align-items: flex-end; gap: 8px; padding: 10px 10px 10px 15px; border: 1px solid #d8e5f7; border-radius: 12px; background: #fff; box-shadow: 0 10px 24px rgba(46,111,224,.1); transition: .2s; }.input-row:focus-within { border-color: #80abea; box-shadow: 0 12px 28px rgba(46,111,224,.16); }.input-row .el-textarea { flex: 1; }:deep(.el-textarea__inner) { padding: 6px 0; border: 0; box-shadow: none !important; background: transparent; line-height: 1.6; }.send-button { width: 38px; height: 38px; display: grid; place-items: center; flex-shrink: 0; border: 0; border-radius: 10px; color: #a8b7cb; background: #edf2f8; cursor: not-allowed; transition: .2s; }.send-button.ready { color: #fff; background: linear-gradient(135deg, #3e86ec, #2e6fe0); cursor: pointer; box-shadow: 0 6px 14px rgba(46,111,224,.26); }.send-button.ready:hover { transform: translateY(-1px); }.send-loading { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,.35); border-top-color: #fff; border-radius: 50%; animation: spin .7s linear infinite; }.composer-tip { padding-top: 7px; text-align: center; color: #9baabd; font-size: 10px; }@keyframes spin { to { transform: rotate(360deg); } }
.face.working { background: #edf4ff; }.agent-loader { width: 15px; height: 15px; border: 2px solid #c7daf8; border-top-color: #2e6fe0; border-radius: 50%; animation: agent-spin .72s linear infinite; }@keyframes agent-spin { to { transform: rotate(360deg); } }
@media (max-width: 768px) { .chat-shell { height: calc(100vh - 96px); min-height: 520px; margin: 0; border-radius: 12px; }.session-panel { width: 64px; padding: 14px 8px; }.assistant-brand { justify-content: center; padding: 2px 0 18px; }.assistant-brand > div:last-child, .new-session span, .new-session kbd, .history-label, .session-title, .session-time { display: none; }.new-session { justify-content: center; padding: 11px; }.session-item { height: 40px; }.messages, .composer-wrap { padding-left: 16px; padding-right: 16px; }.assistant-response, .bubble { max-width: 86%; }.profile-consent > div { display: none; }.profile-consent { padding: 7px 9px; }.progress-row { grid-template-columns: 8px 66px minmax(0,1fr); }.intake-free { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .progress-indicator.running, .progress-row.running .progress-dot, .bubble.is-streaming::after, .send-loading, .agent-loader { animation: none; }.progress-details-enter-active, .progress-details-leave-active, .msg-enter-active { transition-duration: .01ms; } }
.risk-pill { display: inline-flex; align-items: center; gap: 7px; min-height: 32px; padding: 6px 11px; border: 1px solid #bdd4f7; border-radius: 8px; color: #245dbb; background: #edf4ff; font-size: 12px; font-weight: 700; box-shadow: 0 3px 8px rgba(46,111,224,.08); }.risk-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }.risk-prefix { padding-right: 7px; border-right: 1px solid currentColor; opacity: .72; font-size: 11px; }.risk-pill.medium { border-color: #f0ca8b; color: #9a641d; background: #fff6e7; }.risk-pill.high, .risk-pill.emergency { border-color: #efb6b0; color: #a63a31; background: #fff0ee; }.risk-pill.emergency { color: #9c2922; background: #fde5e2; }
</style>
