<template>
  <div class="assistant-entry">
    <button class="assistant-trigger" type="button" :aria-expanded="open"
            aria-controls="application-assistant-panel" @click="togglePanel">
      <el-icon aria-hidden="true"><MagicStick /></el-icon>
      <span>AI 助手</span>
    </button>

    <Teleport to="body">
      <transition name="assistant-panel">
        <section v-if="open" id="application-assistant-panel" class="assistant-panel"
                 role="dialog" aria-modal="false" aria-labelledby="application-assistant-title">
          <header class="assistant-head">
            <h2 id="application-assistant-title">SECP-AI助手</h2>
            <button class="assistant-close" type="button" aria-label="关闭应用使用助手" @click="closePanel">
              <el-icon aria-hidden="true"><Close /></el-icon>
            </button>
          </header>

          <div ref="messageList" class="assistant-messages" aria-live="polite">
            <div v-if="messages.length === 0" class="assistant-empty">
              <div class="welcome-content">
                <span class="empty-mark" aria-hidden="true">
                  <el-icon><MagicStick /></el-icon>
                </span>
                <h3>你好，我是 <span>SECP-AI助手</span></h3>

                <div class="suggestion-carousel" aria-label="快捷问题分类">
                  <Transition name="suggestion-slide" mode="out-in">
                    <div :key="carouselIndex" class="suggestion-grid">
                      <article v-for="group in visibleSuggestionGroups" :key="group.title" class="suggestion-card">
                        <div class="suggestion-title">
                          <h4>{{ group.title }}</h4>
                          <el-icon aria-hidden="true"><component :is="group.icon" /></el-icon>
                        </div>
                        <button v-for="question in group.questions" :key="question" type="button"
                                @click="askSuggestion(question)">
                          <span>{{ question }}</span>
                          <el-icon aria-hidden="true"><ArrowRight /></el-icon>
                        </button>
                      </article>
                    </div>
                  </Transition>
                  <div class="carousel-controls">
                    <button type="button" aria-label="查看上一组问题" @click="moveCarousel(-1)">
                      <el-icon aria-hidden="true"><ArrowLeft /></el-icon>
                    </button>
                    <button type="button" aria-label="查看下一组问题" @click="moveCarousel(1)">
                      <el-icon aria-hidden="true"><ArrowRight /></el-icon>
                    </button>
                    <span>{{ carouselIndex + 1 }} / {{ suggestionGroups.length }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-for="message in messages" :key="message.id" class="message-row" :class="message.role">
              <div class="message-bubble">
                <span v-if="message.pending && !message.content" class="typing" aria-label="正在生成回答">
                  <i></i><i></i><i></i>
                </span>
                <div v-else-if="message.role === 'assistant'" class="markdown-content"
                     v-html="renderMarkdown(message.content)" @click="onMarkdownClick"></div>
                <span v-else>{{ message.content }}</span>
              </div>
            </div>
          </div>

          <form class="assistant-composer" @submit.prevent="sendMessage">
            <textarea ref="inputEl" v-model="input" rows="2" maxlength="1000"
                      placeholder="请将您遇到的问题告诉我，使用 Shift + Enter 换行" aria-label="向应用使用助手提问"
                      :disabled="loading" @keydown.enter.exact.prevent="onEnter"></textarea>
            <div class="composer-foot">
              <button class="thinking-button" type="button" :class="{ active: deepThinking }"
                      :aria-pressed="deepThinking" @click="deepThinking = !deepThinking">
                <el-icon aria-hidden="true"><Opportunity /></el-icon>
                <span>深度思考</span>
              </button>
              <button class="send-button" type="submit" :disabled="loading || !input.trim()" aria-label="发送问题">
                <el-icon aria-hidden="true"><Top /></el-icon>
              </button>
            </div>
          </form>
        </section>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  ArrowLeft, ArrowRight, Bell, ChatDotRound, Close, DataAnalysis, Document,
  Goods, MagicStick, Opportunity, QuestionFilled, Top
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { applicationAssistantChatStream } from '@/api'
import { useUserStore } from '@/store/user'
import {
  renderApplicationAssistantLinks,
  resolveApplicationAssistantRoute
} from '@/utils/applicationAssistantLinks'

const router = useRouter()
const userStore = useUserStore()
const open = ref(false)
const input = ref('')
const loading = ref(false)
const deepThinking = ref(false)
const carouselIndex = ref(0)
const messages = ref([])
const inputEl = ref(null)
const messageList = ref(null)
let requestController = null
let messageId = 0

const suggestionGroups = [
  {
    title: '平台使用指南',
    icon: Document,
    questions: ['第一次使用应该从哪里开始？', '如何修改个人账户信息？', '找不到功能入口怎么办？']
  },
  {
    title: '健康档案',
    icon: QuestionFilled,
    questions: ['如何完善我的健康档案？', '在哪里查看健康报告？', '就诊记录如何新增？']
  },
  {
    title: '体征数据',
    icon: DataAnalysis,
    questions: ['如何记录血压和血糖？', '体征数据填错了怎么修改？', '历史健康趋势在哪里看？']
  },
  {
    title: '健康预警',
    icon: Bell,
    questions: ['健康预警在哪里查看？', '收到异常提醒后应该怎么做？', '如何查看预警处理记录？']
  },
  {
    title: '积分与兑换',
    icon: Goods,
    questions: ['积分商城如何兑换商品？', '我的积分明细在哪里？', '兑换记录怎么查询？']
  },
  {
    title: '医生服务',
    icon: ChatDotRound,
    questions: ['怎样发起医生咨询？', '如何查找合适的医生？', '咨询记录在哪里查看？']
  }
]

const visibleSuggestionGroups = computed(() => [
  suggestionGroups[carouselIndex.value],
  suggestionGroups[(carouselIndex.value + 1) % suggestionGroups.length]
])

function moveCarousel(step) {
  carouselIndex.value = (carouselIndex.value + step + suggestionGroups.length) % suggestionGroups.length
}

function togglePanel() {
  if (open.value) closePanel()
  else {
    open.value = true
    nextTick(() => inputEl.value?.focus())
  }
}

function closePanel() {
  requestController?.abort()
  requestController = null
  loading.value = false
  open.value = false
  input.value = ''
  messages.value = []
}

async function scrollToLatest() {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}

async function sendMessage() {
  const content = input.value.trim()
  if (!content || loading.value) return

  input.value = ''
  loading.value = true
  messages.value.push({ id: ++messageId, role: 'user', content })
  const assistant = reactive({ id: ++messageId, role: 'assistant', content: '', pending: true })
  messages.value.push(assistant)
  await scrollToLatest()

  requestController = new AbortController()
  try {
    await applicationAssistantChatStream({ message: content }, {
      signal: requestController.signal,
      onDelta: async (delta) => {
        assistant.content += delta
        await scrollToLatest()
      },
      onDone: () => { assistant.pending = false }
    })
    if (!assistant.content) assistant.content = '应用助手暂时没有返回内容，请稍后再试。'
  } catch (error) {
    if (error.name !== 'AbortError') assistant.content = error.message || '应用助手暂时不可用，请稍后再试。'
  } finally {
    assistant.pending = false
    loading.value = false
    requestController = null
    await scrollToLatest()
    if (open.value) inputEl.value?.focus()
  }
}

async function askSuggestion(question) {
  if (loading.value) return
  input.value = question
  await sendMessage()
}

function onEnter(event) {
  if (!event.isComposing) sendMessage()
}

function renderInline(value) {
  return renderApplicationAssistantLinks(value, userStore.roles)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*/g, '')
}

async function onMarkdownClick(event) {
  const link = event.target.closest?.('a[data-app-route]')
  if (!link || !event.currentTarget.contains(link)) return

  event.preventDefault()
  const path = resolveApplicationAssistantRoute(link.dataset.appRoute, userStore.roles)
  if (!path) return

  requestController?.abort()
  await router.push(path)
  closePanel()
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
    const ordered = line.match(/^\s*\d+\.\s+(.+)$/)
    const bullet = line.match(/^\s*[-*]\s+(.+)$/)
    if (boldHeading || hashHeading) {
      closeList()
      html += `<h3>${renderInline((boldHeading || hashHeading)[1])}</h3>`
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
    } else if (/^\s*---+\s*$/.test(line)) {
      closeList()
      html += '<hr>'
    } else {
      closeList()
      html += `<p>${renderInline(line)}</p>`
    }
  })
  closeList()
  return html
}

function onKeydown(event) {
  if (event.key === 'Escape' && open.value) closePanel()
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  requestController?.abort()
})
</script>

<style scoped>
.assistant-entry { flex: 0 0 auto; }

.assistant-trigger {
  height: 40px;
  padding: 0 15px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #536fe8;
  background: #fff;
  border: 1px solid #d9def8;
  border-radius: 8px;
  font: inherit;
  font-size: 15px;
  font-weight: 650;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease, border-color .2s ease, transform .2s ease;
}
.assistant-trigger .el-icon { width: 20px; height: 20px; font-size: 20px; }
.assistant-trigger:hover,
.assistant-trigger[aria-expanded="true"] { color: #fff; background: #536fe8; border-color: #536fe8; }
.assistant-trigger:active { transform: scale(.98); }

.assistant-panel {
  --assistant-blue: #526ee8;
  --assistant-violet: #756bd8;
  --assistant-rose: #d67eae;
  position: fixed;
  z-index: 310;
  top: 76px;
  right: 24px;
  width: min(690px, calc(100vw - 48px));
  height: min(720px, calc(100dvh - 100px));
  min-height: 540px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #16181d;
  background: #fff;
  border: 1px solid #eef0f7;
  border-radius: 14px;
  box-shadow: 0 22px 60px rgba(62, 71, 126, .16);
}

.assistant-head {
  min-height: 60px;
  padding: 0 14px 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, .96);
  border-bottom: 1px solid #f0f1f6;
}
.assistant-head h2 { margin: 0; font: 700 15px/1.35 inherit; letter-spacing: 0; }
.assistant-close {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  color: #60646f;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease, transform .2s ease;
}
.assistant-close .el-icon { font-size: 20px; }
.assistant-close:hover { color: #22242a; background: #f4f5f8; }
.assistant-close:active { transform: scale(.96); }

.assistant-messages {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 22px 24px;
  overflow-y: auto;
  background:
    radial-gradient(circle at 48% 52%, rgba(91, 119, 239, .11), transparent 27%),
    radial-gradient(circle at 67% 61%, rgba(207, 119, 180, .10), transparent 24%),
    linear-gradient(180deg, #fff 0%, #fdfdff 100%);
  scroll-behavior: smooth;
}
.assistant-empty { min-height: 100%; display: flex; align-items: center; }
.welcome-content { width: 100%; max-width: 630px; margin: 0 auto; padding: 2px 0 40px; }
.empty-mark {
  width: 50px;
  height: 50px;
  display: grid;
  place-items: center;
  margin-bottom: 18px;
  color: #fff;
  background: linear-gradient(145deg, #526ee8 4%, #7767d5 54%, #c56ca7 100%);
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(91, 92, 200, .22);
}
.empty-mark .el-icon { font-size: 27px; }
.assistant-empty h3 { margin: 0 0 32px; color: #0f1115; font-size: clamp(29px, 3.3vw, 36px); font-weight: 750; line-height: 1.18; letter-spacing: -.03em; }
.assistant-empty h3 span {
  color: #596fe4;
  background: linear-gradient(90deg, #4d6ee9 0%, #796bd6 58%, #c677ad 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.suggestion-carousel { min-width: 0; }
.suggestion-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.suggestion-card {
  min-width: 0;
  padding: 18px 19px;
  background: rgba(255, 255, 255, .9);
  border: 1px solid rgba(235, 237, 247, .95);
  border-radius: 14px;
  box-shadow: 0 18px 40px rgba(88, 96, 159, .08);
}
.suggestion-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.suggestion-title h4 {
  margin: 0;
  color: #596fe4;
  background: linear-gradient(90deg, #536fe8, #ce7caf);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  font-size: 18px;
  font-weight: 750;
}
.suggestion-title .el-icon { color: #7c73d8; font-size: 21px; }
.suggestion-card > button {
  width: 100%;
  min-height: 34px;
  padding: 4px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #23252b;
  background: transparent;
  border: 0;
  border-radius: 8px;
  font: 500 14px/1.4 inherit;
  text-align: left;
  cursor: pointer;
  transition: color .2s ease, padding .2s ease, background-color .2s ease;
}
.suggestion-card > button span { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.suggestion-card > button .el-icon { flex: 0 0 auto; color: #afb4c4; opacity: 0; transform: translateX(-5px); transition: opacity .2s ease, transform .2s ease; }
.suggestion-card > button:hover { padding-inline: 9px; color: var(--assistant-blue); background: #f4f6ff; }
.suggestion-card > button:hover .el-icon { opacity: 1; transform: translateX(0); }
.carousel-controls { min-height: 42px; padding-top: 12px; display: flex; align-items: center; gap: 8px; }
.carousel-controls button {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: #6e7280;
  background: rgba(255,255,255,.82);
  border: 1px solid #e1e4ed;
  border-radius: 50%;
  cursor: pointer;
  transition: color .2s ease, border-color .2s ease, background-color .2s ease, transform .2s ease;
}
.carousel-controls button:hover { color: #536fe8; background: #fff; border-color: #cbd2f4; }
.carousel-controls button:active { transform: scale(.95); }
.carousel-controls span { margin-left: 3px; color: #9296a3; font-size: 12px; font-variant-numeric: tabular-nums; }
.suggestion-slide-enter-active,
.suggestion-slide-leave-active { transition: opacity .18s ease, transform .24s cubic-bezier(.22, 1, .36, 1); }
.suggestion-slide-enter-from { opacity: 0; transform: translateX(12px); }
.suggestion-slide-leave-to { opacity: 0; transform: translateX(-12px); }

.message-row { display: flex; margin-bottom: 18px; }
.message-row.user { justify-content: flex-end; }
.message-row.assistant { justify-content: flex-start; }
.message-bubble { max-width: 82%; padding: 12px 15px; border-radius: 12px; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 15px; line-height: 1.72; }
.message-row.user .message-bubble { color: #fff; background: linear-gradient(135deg, #526ee8, #756bd8); border-bottom-right-radius: 4px; }
.message-row.assistant .message-bubble { color: #202229; background: rgba(255,255,255,.94); border: 1px solid #e8eaf3; border-bottom-left-radius: 4px; box-shadow: 0 8px 22px rgba(77, 87, 145, .07); }
.markdown-content :deep(p) { margin: 0; }
.markdown-content :deep(.md-spacer) { height: 10px; }
.markdown-content :deep(h3) { margin: 14px 0 5px; color: #17191f; font: 700 15px/1.55 inherit; }
.markdown-content :deep(h3:first-child) { margin-top: 0; }
.markdown-content :deep(ol), .markdown-content :deep(ul) { margin: 5px 0; padding-left: 1.45em; }
.markdown-content :deep(li) { margin: 3px 0; padding-left: 2px; }
.markdown-content :deep(strong) { color: #17191f; font-weight: 700; }
.markdown-content :deep(code) { padding: 1px 5px; color: #455ab7; background: #eef1ff; border-radius: 4px; font: 13px/1.5 Consolas, monospace; }
.markdown-content :deep(hr) { height: 1px; margin: 10px 0; border: 0; background: #e5e8f2; }
.markdown-content :deep(.app-route-link) {
  margin-inline: 1px;
  padding-inline: 2px;
  color: #3156c8;
  border-radius: 3px;
  font-weight: 650;
  text-decoration-line: underline;
  text-decoration-thickness: .08em;
  text-underline-offset: 3px;
  cursor: pointer;
  transition: color .18s ease, background-color .18s ease;
}
.markdown-content :deep(.app-route-link:hover) { color: #1f3f9f; background: #eef3ff; }
.markdown-content :deep(.app-route-link:focus-visible) {
  color: #1f3f9f;
  outline: 3px solid rgba(49, 86, 200, .24);
  outline-offset: 2px;
}
.typing { display: inline-flex; align-items: center; gap: 4px; min-width: 38px; height: 20px; }
.typing i { width: 5px; height: 5px; border-radius: 50%; background: var(--assistant-blue); animation: typingPulse 1s ease-in-out infinite; }
.typing i:nth-child(2) { animation-delay: .14s; }
.typing i:nth-child(3) { animation-delay: .28s; }

.assistant-composer {
  margin: 0 24px 20px;
  padding: 12px;
  background: linear-gradient(#fff, #fff) padding-box, linear-gradient(100deg, #536fe8, #806dd3 55%, #d178aa) border-box;
  border: 1.5px solid transparent;
  border-radius: 14px;
  box-shadow: 0 10px 30px rgba(76, 86, 151, .08);
}
.assistant-composer textarea {
  width: 100%;
  min-height: 54px;
  max-height: 116px;
  padding: 2px 2px 8px;
  resize: none;
  display: block;
  color: #202229;
  background: #fff;
  border: 0;
  border-radius: 8px;
  font: inherit;
  font-size: 15px;
  line-height: 1.6;
  outline: none;
}
.assistant-composer textarea::placeholder { color: #9699a2; opacity: 1; }
.assistant-composer textarea:disabled { color: #777b87; background: #f8f8fb; cursor: not-allowed; }
.composer-foot { min-height: 38px; display: flex; align-items: center; justify-content: space-between; }
.thinking-button {
  height: 34px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #343740;
  background: #fff;
  border: 1px solid #e4e5eb;
  border-radius: 8px;
  font: 500 13px/1 inherit;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease, border-color .2s ease, transform .2s ease;
}
.thinking-button:hover,
.thinking-button.active { color: #5367cb; background: #f3f5ff; border-color: #cfd5f6; }
.thinking-button:active { transform: scale(.97); }
.send-button {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #526ee8, #756bd8);
  border: 0;
  border-radius: 9px;
  cursor: pointer;
  transition: opacity .2s ease, transform .2s ease;
}
.send-button:hover:not(:disabled) { transform: translateY(-1px); }
.send-button:active:not(:disabled) { transform: scale(.96); }
.send-button:disabled { color: #fff; background: #cfd6f5; cursor: not-allowed; opacity: .82; }
.send-button .el-icon { font-size: 18px; }

.assistant-trigger:focus-visible,
.assistant-close:focus-visible,
.suggestion-card button:focus-visible,
.carousel-controls button:focus-visible,
.thinking-button:focus-visible,
.send-button:focus-visible { outline: 3px solid rgba(82, 110, 232, .25); outline-offset: 2px; }

@keyframes typingPulse {
  0%, 70%, 100% { opacity: .3; transform: translateY(0); }
  35% { opacity: 1; transform: translateY(-3px); }
}
@keyframes assistantMarkSpin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.assistant-panel-enter-active,
.assistant-panel-leave-active { transition: opacity .22s ease, transform .3s cubic-bezier(.22, 1, .36, 1); }
.assistant-panel-enter-from,
.assistant-panel-leave-to { opacity: 0; transform: translateY(-10px) scale(.985); }

@media (prefers-reduced-motion: no-preference) {
  .empty-mark { animation: assistantMarkSpin 9s linear infinite; }
}
@media (max-width: 1180px) {
  .assistant-trigger { width: 42px; padding: 0; justify-content: center; }
  .assistant-trigger span { display: none; }
}
@media (max-width: 700px) {
  .assistant-panel { top: 64px; right: 0; width: 100vw; height: calc(100dvh - 64px); min-height: 0; border: 0; border-radius: 0; }
  .assistant-head { min-height: 60px; padding-inline: 18px 12px; }
  .assistant-messages { padding: 24px 18px; }
  .welcome-content { padding: 8px 0 30px; }
  .empty-mark { width: 46px; height: 46px; margin-bottom: 16px; border-radius: 13px; }
  .empty-mark .el-icon { font-size: 25px; }
  .assistant-empty h3 { margin-bottom: 30px; font-size: 29px; }
  .suggestion-grid { grid-template-columns: 1fr; gap: 12px; }
  .suggestion-card { padding: 19px 18px; }
  .suggestion-title { margin-bottom: 10px; }
  .suggestion-title h4 { font-size: 18px; }
  .assistant-composer { margin: 0 12px 12px; }
  .message-bubble { max-width: 90%; }
}
@media (prefers-reduced-motion: reduce) {
  .assistant-panel-enter-active,
  .assistant-panel-leave-active,
  .assistant-trigger,
  .assistant-close,
  .suggestion-card button,
  .carousel-controls button,
  .suggestion-slide-enter-active,
  .suggestion-slide-leave-active,
  .thinking-button,
  .send-button,
  .markdown-content :deep(.app-route-link) { transition: none; }
  .typing i,
  .empty-mark { animation: none; }
  .typing i { opacity: .7; }
  .assistant-messages { scroll-behavior: auto; }
}
</style>
