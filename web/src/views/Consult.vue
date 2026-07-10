<template>
  <el-card class="chat-card">
    <div class="chat-head">
      <div class="bot"><el-icon :size="22"><ChatDotRound /></el-icon></div>
      <div>
        <div class="bot-name">健康助手</div>
        <div class="bot-sub"><span class="live"></span>在线 · 有问必答</div>
      </div>
    </div>

    <div class="messages" ref="msgBox">
      <transition-group name="msg" tag="div">
        <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
          <div v-if="m.role==='assistant'" class="face"><el-icon><Avatar /></el-icon></div>
          <div class="bubble">{{ m.content }}</div>
        </div>
      </transition-group>

      <!-- 打字指示器 -->
      <div v-if="loading" class="msg assistant">
        <div class="face"><el-icon><Avatar /></el-icon></div>
        <div class="bubble typing"><span></span><span></span><span></span></div>
      </div>

      <div v-if="!messages.length && !loading" class="hello">
        <div class="hello-orb"><el-icon :size="40"><ChatDotRound /></el-icon></div>
        <h3>您好，有什么健康问题想咨询？</h3>
        <div class="samples">
          <span v-for="s in samples" :key="s" @click="text = s; send()">{{ s }}</span>
        </div>
      </div>
    </div>

    <div class="input-row">
      <el-input v-model="text" size="large" placeholder="请输入您的健康问题…" @keyup.enter="send" :disabled="loading" />
      <el-button type="primary" size="large" :loading="loading" @click="send">
        <el-icon v-if="!loading"><Promotion /></el-icon>&nbsp;发送
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Avatar, Promotion } from '@element-plus/icons-vue'
import { consultChat } from '@/api'
const messages = ref([]), text = ref(''), loading = ref(false), sessionId = ref(''), msgBox = ref(null)
const samples = ['最近血压有点高怎么办？', '老年人补钙吃什么好？', '血糖偏高饮食注意什么？']

async function send() {
  if (!text.value.trim() || loading.value) return
  const q = text.value
  messages.value.push({ role: 'user', content: q }); text.value = ''; scroll()
  loading.value = true
  try {
    const res = await consultChat({ message: q, sessionId: sessionId.value })
    sessionId.value = res.data.sessionId
    messages.value.push({ role: 'assistant', content: res.data.content }); scroll()
  } catch (e) { ElMessage.error('发送失败') } finally { loading.value = false; scroll() }
}
function scroll() { nextTick(() => { if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight }) }
</script>

<style scoped>
.chat-card { height: calc(100vh - 152px); display: flex; flex-direction: column; padding: 0; }
:deep(.el-card__body) { height: 100%; display: flex; flex-direction: column; padding: 0; }

.chat-head {
  display: flex; align-items: center; gap: 12px; padding: 18px 24px;
  border-bottom: 1px solid var(--hda-line);
}
.bot { width: 46px; height: 46px; border-radius: 14px; display: grid; place-items: center; color: #fff;
  background: linear-gradient(135deg, #37B389, #279470); animation: hdaFloat 5s ease-in-out infinite; }
.bot-name { font-weight: 700; font-size: 18px; color: var(--hda-ink); font-family: var(--hda-font-display); }
.bot-sub { font-size: 14px; color: var(--hda-ink-soft); display: flex; align-items: center; gap: 6px; }
.live { width: 8px; height: 8px; border-radius: 50%; background: #37A86B; box-shadow: 0 0 0 0 rgba(55,168,107,.5); animation: pulse 1.8s infinite; }
@keyframes pulse { 0%{ box-shadow:0 0 0 0 rgba(55,168,107,.5) } 70%{ box-shadow:0 0 0 8px rgba(55,168,107,0) } 100%{ box-shadow:0 0 0 0 rgba(55,168,107,0) } }

.messages { flex: 1; overflow-y: auto; padding: 22px 24px; }
.msg { display: flex; align-items: flex-end; gap: 10px; margin: 14px 0; }
.msg.user { justify-content: flex-end; }
.face { width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; flex-shrink: 0;
  background: var(--el-color-primary-light-8); color: var(--el-color-primary); }
.bubble {
  max-width: 68%; padding: 14px 18px; border-radius: 18px; font-size: 17px; line-height: 1.65;
  background: #F3F1EA; color: var(--hda-ink); border-bottom-left-radius: 6px;
  box-shadow: var(--hda-shadow-sm);
}
.msg.user .bubble {
  background: linear-gradient(135deg, #37B389, #2FA37C); color: #fff;
  border-bottom-left-radius: 18px; border-bottom-right-radius: 6px;
}

/* 打字指示器 */
.typing { display: flex; gap: 6px; align-items: center; }
.typing span { width: 9px; height: 9px; border-radius: 50%; background: var(--el-color-primary-light-3); animation: bounce 1.2s infinite; }
.typing span:nth-child(2){ animation-delay: .18s } .typing span:nth-child(3){ animation-delay: .36s }
@keyframes bounce { 0%,60%,100%{ transform: translateY(0); opacity:.6 } 30%{ transform: translateY(-7px); opacity:1 } }

/* 消息进入动画 */
.msg-enter-active { transition: all .4s var(--ease-spring); }
.msg-enter-from { opacity: 0; transform: translateY(14px) scale(.96); }

/* 空状态 */
.hello { text-align: center; padding: 8% 0; }
.hello-orb { width: 88px; height: 88px; border-radius: 50%; display: grid; place-items: center; margin: 0 auto 18px;
  color: #fff; background: linear-gradient(135deg, #37B389, #279470); box-shadow: 0 16px 34px rgba(39,148,112,.4);
  animation: hdaFloat 5s ease-in-out infinite; }
.hello h3 { font-family: var(--hda-font-display); font-size: 24px; color: var(--hda-ink); margin: 0 0 22px; }
.samples { display: flex; flex-wrap: wrap; gap: 12px; justify-content: center; }
.samples span {
  padding: 12px 20px; border-radius: 999px; background: #fff; cursor: pointer; font-size: 16px;
  box-shadow: inset 0 0 0 1.5px var(--el-color-primary-light-7); color: var(--hda-ink);
  transition: transform .22s var(--ease-spring), box-shadow .25s, color .25s;
}
.samples span:hover { transform: translateY(-3px); color: var(--el-color-primary); box-shadow: inset 0 0 0 2px var(--el-color-primary); }

.input-row { display: flex; gap: 12px; padding: 16px 24px; border-top: 1px solid var(--hda-line); }
.input-row .el-input { flex: 1; }
</style>
