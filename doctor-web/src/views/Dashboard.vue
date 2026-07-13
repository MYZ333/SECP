<template>
  <div class="dashboard">
    <section ref="hero" class="hero glass-panel">
      <ParticleNet :count="34" :link="120" :repel="90" />
      <div class="hero-copy">
        <p class="welcome">{{ greeting }}，{{ doctorName }}</p>
        <h1>今日诊疗工作台</h1>
        <p class="hero-sub">集中查看患者档案与咨询进度，及时回应新的健康问题。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/sessions')">
            <el-icon><ChatDotRound /></el-icon>进入咨询会话
          </el-button>
          <el-button size="large" @click="$router.push('/patients')">
            <el-icon><User /></el-icon>查看患者
          </el-button>
        </div>
      </div>
      <div class="hero-ecg" aria-hidden="true">
        <svg viewBox="0 0 360 140">
          <defs>
            <linearGradient id="dashboardEcg" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0" stop-color="#37B6D9" stop-opacity="0" />
              <stop offset=".5" stop-color="#2E6FE0" />
              <stop offset="1" stop-color="#9B6AD8" stop-opacity="0" />
            </linearGradient>
          </defs>
          <path class="ecg-base" d="M0 76 H72 L91 76 L105 46 L122 110 L142 20 L160 96 L176 76 H360" />
          <path class="ecg-live" d="M0 76 H72 L91 76 L105 46 L122 110 L142 20 L160 96 L176 76 H360" />
        </svg>
      </div>
    </section>

    <section class="stats" aria-label="工作统计">
      <el-skeleton v-if="loading" v-for="index in 4" :key="index" animated class="stat-skeleton">
        <template #template><el-skeleton-item variant="rect" class="skeleton-block" /></template>
      </el-skeleton>
      <article
        v-else
        v-for="card in cards"
        :key="card.label"
        class="stat glass-panel"
        tabindex="0"
        @click="$router.push(card.path)"
        @keyup.enter="$router.push(card.path)"
        @pointermove="tilt"
        @pointerleave="resetTilt"
      >
        <div class="stat-glare"></div>
        <div class="stat-top">
          <span class="stat-icon"><el-icon><component :is="card.icon" /></el-icon></span>
          <span v-if="card.alert && card.value > 0" class="live-dot"></span>
        </div>
        <strong class="num">{{ Math.round(card.value) }}</strong>
        <span>{{ card.label }}</span>
        <small>{{ card.note }}</small>
      </article>
    </section>

    <section class="quick-section">
      <div class="section-heading">
        <h2>快捷入口</h2>
        <span>按优先级处理今日工作</span>
      </div>
      <div class="quick-grid">
        <button
          v-for="item in quickActions"
          :key="item.title"
          class="quick glass-panel"
          @click="$router.push(item.path)"
          @pointermove="tilt"
          @pointerleave="resetTilt"
        >
          <span class="quick-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="quick-copy"><b>{{ item.title }}</b><small>{{ item.description }}</small></span>
          <el-icon class="quick-arrow"><ArrowRight /></el-icon>
          <span class="quick-glare"></span>
          <span class="quick-border" aria-hidden="true"></span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref } from 'vue'
import { ArrowRight, ChatDotRound, ChatLineRound, DataAnalysis, Message, User, UserFilled } from '@element-plus/icons-vue'
import gsap from 'gsap'
import ParticleNet from '@/components/ParticleNet.vue'
import { getMe, getStats } from '@/api'

const loading = ref(true)
const doctorName = ref('医生')
const values = reactive({ patientCount: 0, openSessionCount: 0, unreadCount: 0, todayMessageCount: 0 })
const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 11 ? '上午好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
})
const cards = computed(() => [
  { label: '平台患者', value: values.patientCount, note: '可查看健康档案', path: '/patients', icon: markRaw(UserFilled) },
  { label: '进行中会话', value: values.openSessionCount, note: '当前咨询关系', path: '/sessions', icon: markRaw(ChatLineRound) },
  { label: '未读消息', value: values.unreadCount, note: '建议优先处理', path: '/sessions', icon: markRaw(Message), alert: true },
  { label: '今日消息', value: values.todayMessageCount, note: '今日医患沟通', path: '/sessions', icon: markRaw(DataAnalysis) }
])
const quickActions = [
  { title: '咨询会话', description: '回复患者的新消息与附件', path: '/sessions', icon: markRaw(ChatDotRound) },
  { title: '患者档案', description: '查看健康档案和近期体征', path: '/patients', icon: markRaw(User) },
  { title: '个人资料', description: '维护出诊信息与专业介绍', path: '/profile', icon: markRaw(DataAnalysis) }
]

onMounted(async () => {
  const [statsResult, meResult] = await Promise.all([getStats(), getMe()])
  doctorName.value = meResult.data?.name || '医生'
  loading.value = false
  const target = statsResult.data || {}
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) Object.assign(values, target)
  else gsap.to(values, { ...target, duration: .9, ease: 'power3.out' })
})

function tilt(event) {
  const card = event.currentTarget
  const rect = card.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width - .5
  const y = (event.clientY - rect.top) / rect.height - .5
  card.style.setProperty('--rotate-x', `${-y * 5}deg`)
  card.style.setProperty('--rotate-y', `${x * 7}deg`)
  card.style.setProperty('--pointer-x', `${(x + .5) * 100}%`)
  card.style.setProperty('--pointer-y', `${(y + .5) * 100}%`)
}
function resetTilt(event) {
  event.currentTarget.style.setProperty('--rotate-x', '0deg')
  event.currentTarget.style.setProperty('--rotate-y', '0deg')
}
</script>

<style scoped>
.dashboard { display: grid; gap: 22px; }
.hero { position: relative; min-height: 246px; overflow: hidden; padding: 34px 40px; display: flex; align-items: center; justify-content: space-between; }
.hero :deep(.particle-net) { z-index: -1; opacity: .55; }
.hero::after { content: ""; position: absolute; right: 0; bottom: 0; left: 0; height: 3px; background: linear-gradient(90deg, #2E6FE0, #37B6D9, #9B6AD8); transform-origin: left; animation: hero-line 1s var(--ease-out); }
@keyframes hero-line { from { transform: scaleX(0); } }
.hero-copy { position: relative; z-index: 1; }
.welcome { margin: 0 0 5px; color: var(--el-color-primary); font-size: 15px; font-weight: 700; }
.hero h1 { margin: 0; color: var(--hda-ink); font-size: clamp(32px, 4vw, 48px); font-weight: 800; }
.hero-sub { margin: 10px 0 22px; color: var(--hda-ink-soft); font-size: 16px; }
.hero-actions { display: flex; gap: 12px; }
.hero-ecg { flex: 0 0 360px; height: 140px; }
.hero-ecg svg { width: 100%; height: 100%; filter: drop-shadow(0 0 6px rgba(46,111,224,.28)); }
.hero-ecg path { fill: none; stroke-linecap: round; stroke-linejoin: round; }
.ecg-base { stroke: rgba(46,111,224,.12); stroke-width: 2; }
.ecg-live { stroke: url(#dashboardEcg); stroke-width: 3; stroke-dasharray: 92 420; animation: ecg-run 4.5s linear infinite; }
@keyframes ecg-run { from { stroke-dashoffset: 512; } to { stroke-dashoffset: 0; } }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; perspective: 900px; }
.stat { --rotate-x: 0deg; --rotate-y: 0deg; position: relative; min-height: 174px; overflow: hidden; padding: 24px; display: flex; flex-direction: column; cursor: pointer; transform: perspective(900px) rotateX(var(--rotate-x)) rotateY(var(--rotate-y)); transition: transform .4s var(--ease-out), box-shadow .4s var(--ease-out); }
.stat:hover { transform: perspective(900px) rotateX(var(--rotate-x)) rotateY(var(--rotate-y)) translateY(-3px); }
.stat:focus-visible { outline: 3px solid var(--el-color-primary-light-7); outline-offset: 2px; }
.stat-glare { position: absolute; inset: 0; pointer-events: none; opacity: 0; background: radial-gradient(220px circle at var(--pointer-x, 50%) var(--pointer-y, 50%), rgba(46,111,224,.12), transparent 64%); transition: opacity .3s; }
.stat:hover .stat-glare { opacity: 1; }
.stat-top { height: 34px; display: flex; align-items: flex-start; justify-content: space-between; }
.stat-icon { color: var(--el-color-primary); font-size: 27px; }
.live-dot { width: 9px; height: 9px; border-radius: 50%; background: var(--el-color-danger); animation: pulse 1.6s ease-in-out infinite; }
@keyframes pulse { 50% { box-shadow: 0 0 0 7px rgba(229,101,75,0); } 0%,100% { box-shadow: 0 0 0 0 rgba(229,101,75,.42); } }
.stat strong { margin-top: 10px; color: var(--hda-ink); font-size: 38px; line-height: 1; }
.stat > span:not(.stat-icon) { margin-top: 7px; color: var(--hda-ink); font-size: 16px; font-weight: 700; }
.stat small { color: #8B9AAF; font-size: 13px; }
.stat-skeleton { min-height: 174px; }
.skeleton-block { width: 100%; height: 174px; border-radius: 0; }
.section-heading { margin-bottom: 14px; display: flex; align-items: baseline; gap: 14px; }
.section-heading h2 { margin: 0; font-size: 22px; }
.section-heading span { color: var(--hda-ink-soft); font-size: 14px; }
.quick-grid { display: grid; grid-template-columns: 1.25fr 1fr 1fr; gap: 16px; }
.quick { --rotate-x: 0deg; --rotate-y: 0deg; position: relative; min-height: 104px; overflow: hidden; padding: 22px 24px; border: 0; display: flex; align-items: center; gap: 16px; color: inherit; text-align: left; cursor: pointer; transform: perspective(900px) rotateX(var(--rotate-x)) rotateY(var(--rotate-y)); transition: transform .4s var(--ease-out), box-shadow .4s var(--ease-out); }
.quick:hover { box-shadow: var(--hda-shadow), var(--hda-glass-inner); transform: perspective(900px) rotateX(var(--rotate-x)) rotateY(var(--rotate-y)) translateY(-3px); }
.quick:active { transform: perspective(900px) rotateX(var(--rotate-x)) rotateY(var(--rotate-y)) translateY(0) scale(.985); transition-duration: .12s; }
.quick:focus-visible { outline: 3px solid var(--el-color-primary-light-7); outline-offset: 2px; }
.quick-glare { position: absolute; inset: 0; pointer-events: none; opacity: 0; background: radial-gradient(260px circle at var(--pointer-x, 50%) var(--pointer-y, 50%), rgba(46,111,224,.12), transparent 64%); transition: opacity .3s; }
.quick:hover .quick-glare { opacity: 1; }
.quick-border { position: absolute; inset: 0; border: 1.5px solid transparent; pointer-events: none; transition: border-color .3s; }
.quick:hover .quick-border { border-color: var(--el-color-primary-light-5); }
.quick-icon { position: relative; z-index: 1; flex: 0 0 42px; width: 42px; height: 42px; display: grid; place-items: center; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 24px; transition: transform .35s var(--ease-spring); }
.quick:hover .quick-icon { transform: scale(1.08) rotate(-4deg); }
.quick-copy { position: relative; z-index: 1; min-width: 0; display: flex; flex-direction: column; }
.quick-copy b { color: var(--hda-ink); font-size: 17px; }
.quick-copy small { color: var(--hda-ink-soft); font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.quick-arrow { position: relative; z-index: 1; margin-left: auto; color: #AAB7C8; transition: transform .3s var(--ease-out), color .3s; }
.quick:hover .quick-arrow { color: var(--el-color-primary); transform: translateX(4px); }
@media (max-width: 1050px) { .hero-ecg { display: none; } .stats { grid-template-columns: repeat(2, 1fr); } .quick-grid { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .hero { min-height: 0; padding: 28px 22px; } .hero h1 { font-size: 31px; } .hero-actions { align-items: stretch; flex-direction: column; } .stats { grid-template-columns: 1fr 1fr; } .stat { min-height: 148px; padding: 19px; } .stat strong { font-size: 32px; } }
</style>
