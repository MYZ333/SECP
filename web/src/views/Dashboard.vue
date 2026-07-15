<template>
  <div class="dash" ref="root">
    <!-- ============ 全局固定背景：浅蓝→白渐变 + 粒子星座 + 视差光斑 ============ -->
    <div class="bg-fixed" aria-hidden="true">
      <ParticleNet :count="70" :repel="120" />
      <div class="blob-w b1" ref="b1"><div class="blob"></div></div>
      <div class="blob-w b2" ref="b2"><div class="blob"></div></div>
    </div>

    <!-- ============ 右侧滚动进度条（蓝紫渐变） ============ -->
    <div class="sprog" aria-hidden="true"><div class="sprog-bar" ref="progBar"></div></div>

    <div class="dash-body">
      <!-- ============ 欢迎区 Hero ============ -->
      <section class="hero glass" ref="heroEl">
        <div class="hero-text">
          <p class="hi">
            <span v-for="(c, i) in hiChars" :key="'h' + i" class="ch">{{ c }}</span>
          </p>
          <h1>
            <span v-for="(c, i) in titleChars" :key="'t' + i" class="ch"
                  :class="c === '健' ? 'hl hl1' : c === '康' ? 'hl hl2' : ''">{{ c }}</span>
          </h1>
          <p class="sub" data-hero>今天是 {{ today }} · 已累计记录 <b>{{ raw.metric }}</b> 条体征数据</p>
          <div class="acts" data-hero>
            <button class="btn-main" @mousemove="magMove" @mouseleave="magLeave" @click="onMainClick">
              <span class="b-ic" v-html="ICONS.plus"></span>记录体征
            </button>
            <button class="btn-ghost" @click="go('/consult')">
              <span class="b-ic" v-html="ICONS.message"></span>向 AI 咨询
            </button>
          </div>
        </div>
        <div class="hero-ecg" data-hero @mouseenter="ecgEnter" @mouseleave="ecgLeave">
          <svg viewBox="0 0 400 140" preserveAspectRatio="none">
            <defs>
              <linearGradient id="dashEcg" x1="0" y1="0" x2="1" y2="0">
                <stop offset="0" stop-color="#37B6D9" stop-opacity="0" />
                <stop offset=".5" stop-color="#2E6FE0" stop-opacity=".9" />
                <stop offset="1" stop-color="#7B5BE6" stop-opacity="0" />
              </linearGradient>
            </defs>
            <g ref="ecgGroup">
              <path ref="ecgPath"
                    d="M0,70 L104,70 L120,70 L134,36 L148,104 L161,16 L175,96 L188,70 L242,70 L256,70 L270,42 L283,100 L296,24 L309,92 L322,70 L400,70"
                    fill="none" stroke="url(#dashEcg)" stroke-width="2.5" stroke-linecap="round" />
            </g>
          </svg>
        </div>
      </section>

      <!-- ============ 健康态势球 ============ -->
      <section class="situation glass" ref="situationEl" :class="healthSituation.tone">
        <div class="sphere-wrap" aria-hidden="true">
          <div class="sphere">
            <span class="orbit o1"></span>
            <span class="orbit o2"></span>
            <span class="orbit o3"></span>
            <span class="pulse-dot d1" :style="{ opacity: 0.38 + healthSituation.rings.metric * 0.62 }"></span>
            <span class="pulse-dot d2" :style="{ opacity: 0.28 + healthSituation.rings.alert * 0.72 }"></span>
            <span class="pulse-dot d3" :style="{ opacity: 0.28 + healthSituation.rings.consult * 0.72 }"></span>
            <span class="pulse-dot d4" :style="{ opacity: 0.28 + healthSituation.rings.medicine * 0.72 }"></span>
            <div class="sphere-core">
              <strong>{{ situationLoading ? '--' : healthSituation.score }}</strong>
              <small>{{ healthSituation.level }}</small>
            </div>
          </div>
        </div>
        <div class="situation-copy">
          <p class="sec-kicker">HEALTH SITUATION</p>
          <h3>健康态势球</h3>
          <p class="situation-summary">{{ healthSituation.summary }}</p>
          <div class="deductions">
            <span v-if="!healthSituation.deductions.length">暂无明显扣分项，继续保持记录习惯。</span>
            <span v-for="item in healthSituation.deductions.slice(0, 3)" :key="item.key">
              {{ item.label }} <b>-{{ item.points }}</b>
            </span>
          </div>
          <div class="situation-actions">
            <button class="btn-main" @click="go('/health/timeline')">查看健康时间轴</button>
            <button class="btn-ghost" @click="go('/alert')">处理预警</button>
          </div>
        </div>
        <div class="mini-events">
          <button
            v-for="event in healthSituation.events.slice(0, 3)"
            :key="event.id"
            type="button"
            class="mini-event"
            @click="go(event.actionPath)"
          >
            <span>{{ typeMeta(event.type).label }}</span>
            <b>{{ event.title }}</b>
            <small>{{ formatTimelineTime(event.time) }}</small>
          </button>
        </div>
      </section>

      <!-- ============ 统计卡片区 ============ -->
      <section class="stats" ref="statsEl">
        <div v-for="s in stats" :key="s.label" class="stat glass" role="button" tabindex="0"
             @mousemove="tilt" @mouseleave="untilt" @click="go(s.path)" @keyup.enter="go(s.path)">
          <div class="glare" aria-hidden="true"></div>
          <div class="stat-top">
            <span class="s-ic" :style="{ color: s.color }" v-html="s.icon"></span>
            <span v-if="s.alert && s.value > 0" class="dot" aria-label="有未处理预警"></span>
          </div>
          <div class="num" v-countup="s.value">0</div>
          <div class="lb">{{ s.label }}<span class="wk">7 日 +{{ s.week }}</span></div>
          <svg class="spark" viewBox="0 0 120 36" aria-hidden="true">
            <path :ref="el => sparkEls[s.key] = el" :d="s.spark"
                  fill="none" :stroke="s.color" stroke-width="1.5"
                  stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </section>

      <!-- ============ 快捷入口 bento ============ -->
      <section class="quick" ref="bentoEl">
        <h3 class="sec-title">快捷入口</h3>
        <div class="bento">
          <div v-for="q in quicks" :key="q.path" class="tile glass" :class="q.span" role="button" tabindex="0"
               @click="go(q.path)" @keyup.enter="go(q.path)">
            <div class="bd" aria-hidden="true"></div>
            <span class="t-ic" :class="q.anim" :style="{ color: q.color }" v-html="q.icon"></span>
            <div class="t-txt">
              <div class="t-name">{{ q.name }}</div>
              <div class="t-desc">{{ q.desc }}</div>
            </div>
            <span class="t-arr" v-html="ICONS.arrow"></span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { CustomEase } from 'gsap/CustomEase'
import ParticleNet from '@/components/ParticleNet.vue'
import { getPointBalance, pageMetric, pageReport, pageAlerts, pagePointRecords } from '@/api'
import { useUserStore } from '@/store/user'
import { emptyHealthSituation, formatTimelineTime, loadHealthSituation, typeMeta } from '@/utils/healthSituation'

gsap.registerPlugin(ScrollTrigger, CustomEase)
CustomEase.create('hda', 'M0,0 C0.22,1 0.36,1 1,1') // cubic-bezier(0.22,1,0.36,1)

const router = useRouter()
const userStore = useUserStore()
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/* ---------- lucide 线性图标（1.5px 描边，仅蓝/橙/灰着色） ---------- */
const svg = (inner) =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">${inner}</svg>`
const ICONS = {
  plus: svg('<path d="M5 12h14"/><path d="M12 5v14"/>'),
  activity: svg('<path class="act-line" d="M22 12h-4l-3 9L9 3l-3 9H2"/>'),
  bell: svg('<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>'),
  fileText: svg('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/>'),
  coins: svg('<circle cx="8" cy="8" r="6"/><path d="M18.09 10.37A6 6 0 1 1 10.34 18"/><path d="M7 6h1v4"/><path d="m16.71 13.88.7.71-2.82 2.82"/>'),
  book: svg('<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path class="pg-r" d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>'),
  gift: svg('<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13"/><path d="M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7"/><path d="M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5"/>'),
  stetho: svg('<path d="M4.8 2.3A.3.3 0 1 0 5 2H4a2 2 0 0 0-2 2v5a6 6 0 0 0 6 6 6 6 0 0 0 6-6V4a2 2 0 0 0-2-2h-1a.2.2 0 1 0 .3.3"/><path d="M8 15v1a6 6 0 0 0 6 6 6 6 0 0 0 6-6v-4"/><circle cx="20" cy="10" r="2"/>'),
  message: svg('<path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/>'),
  arrow: svg('<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>'),
}

/* ---------- 文案与数据 ---------- */
const nickname = computed(() => userStore.userInfo.nickname || userStore.userInfo.username || '您')
const hour = new Date().getHours()
const greeting = hour < 6 ? '凌晨好' : hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const hiChars = computed(() => `${greeting}，${nickname.value}`.split(''))
const titleChars = '祝您健康每一天'.split('')

const raw = reactive({ balance: 0, metric: 0, report: 0, alert: 0 })
const week = reactive({ balance: [0,0,0,0,0,0,0], metric: [0,0,0,0,0,0,0], report: [0,0,0,0,0,0,0], alert: [0,0,0,0,0,0,0] })
const situationLoading = ref(true)
const healthSituation = ref(emptyHealthSituation())

/* 近 7 日（含今日）日期串 yyyy-MM-dd */
const days = [...Array(7)].map((_, i) => {
  const d = new Date(Date.now() - (6 - i) * 864e5)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
})
const bucket = (records) => days.map(day =>
  (records || []).filter(r => String(r.measureTime || r.createTime || r.updateTime || '').slice(0, 10) === day).length)
const sparkPath = (vals) => {
  const max = Math.max(...vals, 1)
  return vals.map((v, i) => `${i ? 'L' : 'M'}${8 + i * 17},${30 - (v / max) * 22}`).join(' ')
}
const sum = (a) => a.reduce((x, y) => x + y, 0)

const stats = computed(() => [
  { key: 'balance', label: '我的积分', value: raw.balance, week: sum(week.balance), spark: sparkPath(week.balance), icon: ICONS.coins,    color: '#FF6A00', path: '/point/record' },
  { key: 'metric',  label: '体征记录', value: raw.metric,  week: sum(week.metric),  spark: sparkPath(week.metric),  icon: ICONS.activity, color: '#2E6FE0', path: '/health/metric' },
  { key: 'report',  label: '健康报告', value: raw.report,  week: sum(week.report),  spark: sparkPath(week.report),  icon: ICONS.fileText, color: '#2E6FE0', path: '/health/report' },
  { key: 'alert',   label: '健康预警', value: raw.alert,   week: sum(week.alert),   spark: sparkPath(week.alert),   icon: ICONS.bell,     color: '#5D7189', path: '/alert', alert: true },
])

const quicks = computed(() => [
  { name: '健康档案', path: '/health/profile', icon: ICONS.book,    color: '#2E6FE0', span: 'c2', anim: 'a-book',
    desc: '基本信息、既往病史与健康标签集中归档' },
  { name: '健康时间轴', path: '/health/timeline', icon: ICONS.activity, color: '#37B6D9', span: 'c1', anim: 'a-line',
    desc: '按时间串联体征、预警、报告和咨询闭环' },
  { name: '体征数据', path: '/health/metric',  icon: ICONS.activity, color: '#2E6FE0', span: 'c1', anim: 'a-line',
    desc: `已记录 ${raw.metric} 条，近 7 日新增 ${sum(week.metric)} 条` },
  { name: '积分商城', path: '/point/mall',     icon: ICONS.gift,    color: '#FF6A00', span: 'c1', anim: '',
    desc: `当前 ${raw.balance} 积分可直接兑换好礼` },
  { name: '医生专家库', path: '/doctor',       icon: ICONS.stetho,  color: '#2E6FE0', span: 'c1', anim: '',
    desc: '按科室与职称检索签约医生专家' },
  { name: '健康咨询', path: '/consult',        icon: ICONS.message, color: '#2E6FE0', span: 'c1', anim: '',
    desc: 'AI 健康助手 7×24 小时即时应答' },
  { name: '健康预警', path: '/alert',          icon: ICONS.bell,    color: '#5D7189', span: 'c2', anim: 'a-bell',
    desc: raw.alert > 0 ? `${raw.alert} 条预警待处理，建议尽快查看` : '暂无未处理预警，各项指标平稳' },
])

function go(p) { router.push(p) }

/* ---------- refs ---------- */
const root = ref(null), heroEl = ref(null), situationEl = ref(null), statsEl = ref(null), bentoEl = ref(null)
const b1 = ref(null), b2 = ref(null), progBar = ref(null)
const ecgGroup = ref(null), ecgPath = ref(null)
const sparkEls = reactive({})

/* ---------- 磁吸按钮 + 波纹 ---------- */
function magMove(e) {
  if (reduced) return
  const el = e.currentTarget, r = el.getBoundingClientRect()
  const dx = (e.clientX - (r.left + r.width / 2)) / (r.width / 2)
  const dy = (e.clientY - (r.top + r.height / 2)) / (r.height / 2)
  gsap.to(el, { x: dx * 6, y: dy * 6, duration: 0.4, ease: 'hda' })
}
function magLeave(e) { gsap.to(e.currentTarget, { x: 0, y: 0, duration: 0.5, ease: 'hda' }) }
function onMainClick(e) {
  if (!reduced) {
    const el = e.currentTarget, r = el.getBoundingClientRect()
    const s = document.createElement('span')
    s.className = 'ripple'
    s.style.left = e.clientX - r.left + 'px'
    s.style.top = e.clientY - r.top + 'px'
    el.appendChild(s)
    s.addEventListener('animationend', () => s.remove())
  }
  go('/health/metric')
}

/* ---------- 心电图 hover 振幅 ---------- */
function ecgEnter() { if (!reduced) gsap.to(ecgGroup.value, { scaleY: 1.55, transformOrigin: '50% 50%', duration: 0.5, ease: 'hda' }) }
function ecgLeave() { if (!reduced) gsap.to(ecgGroup.value, { scaleY: 1, transformOrigin: '50% 50%', duration: 0.5, ease: 'hda' }) }

/* ---------- 统计卡 3D 倾斜 + 高光跟随 ---------- */
function tilt(e) {
  if (reduced) return
  const el = e.currentTarget, r = el.getBoundingClientRect()
  const px = (e.clientX - r.left) / r.width, py = (e.clientY - r.top) / r.height
  el.style.setProperty('--rx', (0.5 - py) * 8 + 'deg')
  el.style.setProperty('--ry', (px - 0.5) * 8 + 'deg')
  el.style.setProperty('--mx', px * 100 + '%')
  el.style.setProperty('--my', py * 100 + '%')
}
function untilt(e) {
  const el = e.currentTarget
  el.style.setProperty('--rx', '0deg')
  el.style.setProperty('--ry', '0deg')
}

/* ---------- 滚动进度条 ---------- */
let onScroll = null

/* ---------- 数据拉取（接口与原页面一致，仅加大分页以计算 7 日趋势） ---------- */
async function fetchData() {
  const [bal, met, rep, al, pt] = await Promise.allSettled([
    getPointBalance(),
    pageMetric({ pageNum: 1, pageSize: 50 }),
    pageReport({ pageNum: 1, pageSize: 50 }),
    pageAlerts({ pageNum: 1, pageSize: 50, status: 'ACTIVE' }),
    pagePointRecords({ pageNum: 1, pageSize: 50 }),
  ])
  if (bal.status === 'fulfilled') raw.balance = bal.value.data ?? 0
  if (met.status === 'fulfilled') { raw.metric = met.value.data.total; week.metric = bucket(met.value.data.records) }
  if (rep.status === 'fulfilled') { raw.report = rep.value.data.total; week.report = bucket(rep.value.data.records) }
  if (al.status === 'fulfilled')  { raw.alert = al.value.data.total;  week.alert = bucket(al.value.data.records) }
  if (pt.status === 'fulfilled')  week.balance = bucket(pt.value.data.records)
}

async function fetchSituation() {
  situationLoading.value = true
  try {
    healthSituation.value = await loadHealthSituation({ metricSize: 80, sessionSize: 30, medicationLimit: 0 })
  } finally {
    situationLoading.value = false
  }
}

/* ---------- GSAP 编排 ---------- */
let ctx = null
function initFx() {
  ctx = gsap.context(() => {
    /* Hero：问候语逐字 + 附属元素 */
    gsap.from('.hero .ch', { y: 16, opacity: 0, duration: 0.5, ease: 'hda', stagger: 0.03 })
    gsap.from('[data-hero]', { y: 20, opacity: 0, duration: 0.5, ease: 'hda', stagger: 0.08, delay: 0.35, clearProps: 'all' })

    /* 光斑视差（系数 0.2），呼吸缩放由 CSS 负责（分离 transform 层避免冲突） */
    const max = () => ScrollTrigger.maxScroll(window)
    gsap.to(b1.value, { y: () => max() * 0.2,  ease: 'none', scrollTrigger: { start: 0, end: 'max', scrub: true, invalidateOnRefresh: true } })
    gsap.to(b2.value, { y: () => -max() * 0.2, ease: 'none', scrollTrigger: { start: 0, end: 'max', scrub: true, invalidateOnRefresh: true } })

    /* 心电图循环描边 */
    const p = ecgPath.value, len = p.getTotalLength()
    gsap.set(p, { strokeDasharray: `${len * 0.22} ${len}`, strokeDashoffset: len * 1.22 })
    gsap.to(p, { strokeDashoffset: -len * 0.22, duration: 6, repeat: -1, ease: 'none' })

    /* 主要内容默认保持可见，只保留 hover、描边、图表线条这类不控制显隐的动效。 */
  }, root.value)
}

/* sparkline 生长绘制（数据就绪后再挂 ScrollTrigger，只播一次） */
let sparkTweens = []
function drawSparks() {
  const paths = Object.values(sparkEls).filter(Boolean)
  if (!paths.length) return
  if (reduced) return // 直接完整显示
  paths.forEach((p, i) => {
    const len = p.getTotalLength()
    gsap.set(p, { strokeDasharray: len, strokeDashoffset: len })
    const tween = gsap.to(p, {
      strokeDashoffset: 0, duration: 0.6, ease: 'hda', delay: i * 0.1,
      scrollTrigger: { trigger: statsEl.value, start: 'top 88%', once: true },
    })
    sparkTweens.push(tween)
  })
}

onMounted(async () => {
  /* 滚动进度条（transform-only） */
  const bar = progBar.value
  let ticking = false
  onScroll = () => {
    if (ticking) return
    ticking = true
    requestAnimationFrame(() => {
      const h = document.documentElement
      const pgs = h.scrollTop / Math.max(h.scrollHeight - h.clientHeight, 1)
      bar.style.transform = `scaleY(${pgs})`
      ticking = false
    })
  }
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()

  if (!reduced) initFx()
  await Promise.allSettled([fetchData(), fetchSituation()])
  await nextTick()
  drawSparks()
  ScrollTrigger.refresh()
})

onBeforeUnmount(() => {
  ctx && ctx.revert()
  sparkTweens.forEach(tween => tween.kill())
  sparkTweens = []
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
/* ============ 设计令牌（本页局部）：12px 圆角 / 单一阴影 / 统一缓动 ============ */
.dash {
  --ease: cubic-bezier(0.22, 1, 0.36, 1);
  --r: 12px;
  --shadow: 0 4px 16px rgba(64, 128, 255, 0.08);
  --blue: #2e6fe0;
  --violet: #7b5be6;
  --orange: #ff6a00;
  --gray: #5d7189;
  position: relative;
  min-height: 100%;
  max-width: 100%;
  overflow-x: clip;
}

/* ============ 固定背景层 ============ */
.bg-fixed {
  position: fixed; inset: 0; z-index: 0; pointer-events: none;
  background:
    radial-gradient(1100px 620px at 8% -4%, #dfeafb 0%, transparent 58%),
    radial-gradient(950px 640px at 102% 104%, #dcf2f8 0%, transparent 55%),
    linear-gradient(180deg, #eaf2fb 0%, #ffffff 70%);
}
.blob-w { position: absolute; will-change: transform; }
.b1 { top: 6%; left: -70px; }
.b2 { bottom: -60px; right: 8%; }
.blob {
  width: 320px; height: 320px; border-radius: 50%;
  background: #a9c6f5; filter: blur(64px); opacity: 0.45;
  animation: breathe 6s var(--ease) infinite;
}
.b2 .blob { width: 260px; height: 260px; background: #a5ddec; animation-delay: -3s; }
@keyframes breathe { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.12); } }

.dash-body {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; gap: 32px;
  width: min(100%, 1280px); max-width: 100%; margin: 0 auto;
}

/* ============ 毛玻璃基类 ============ */
.glass {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--r);
  box-shadow: var(--shadow);
}

/* ============ 滚动进度条 ============ */
.sprog {
  position: fixed; top: 0; right: 0; bottom: 0; width: 3px; z-index: 120;
  background: transparent;
}
.sprog-bar {
  width: 100%; height: 100%;
  background: linear-gradient(180deg, var(--blue), var(--violet));
  transform: scaleY(0); transform-origin: top; will-change: transform;
}

/* ============ Hero ============ */
.hero {
  display: flex; justify-content: space-between; align-items: center; gap: 32px;
  padding: 48px 48px; overflow: hidden; will-change: transform, opacity;
}
.hero-text { min-width: 0; }
.hi { margin: 0 0 8px; font-size: 18px; color: var(--gray); font-weight: 500; }
.hero h1 { margin: 0 0 8px; font-size: clamp(30px, 3.2vw, 44px); color: var(--hda-ink); }
.ch { display: inline-block; white-space: pre; will-change: transform, opacity; }
.hl {
  background: linear-gradient(90deg, var(--blue), var(--violet));
  background-size: 200% 100%;
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.hl1 { background-position: 0 0; }
.hl2 { background-position: 100% 0; }
.sub { margin: 0 0 24px; font-size: 16px; color: var(--gray); }
.sub b { color: var(--blue); font-weight: 700; }
.acts { display: flex; gap: 16px; }
.btn-main, .btn-ghost {
  position: relative; overflow: hidden; display: inline-flex; align-items: center; gap: 8px;
  min-height: 48px; padding: 0 24px; border-radius: var(--r); cursor: pointer;
  font-size: 17px; font-weight: 600; border: none;
  transition: box-shadow 0.4s var(--ease), background-color 0.4s var(--ease);
}
.btn-main { background: linear-gradient(135deg, var(--blue), #2458b8); color: #fff; box-shadow: var(--shadow); will-change: transform; }
.btn-ghost { background: rgba(255, 255, 255, 0.7); color: var(--blue); border: 1px solid var(--el-color-primary-light-7); }
.btn-ghost:hover { background: var(--el-color-primary-light-9); }
.b-ic { display: grid; place-items: center; width: 18px; height: 18px; }
.b-ic :deep(svg) { width: 18px; height: 18px; }
.btn-main :deep(.ripple) {
  position: absolute; width: 12px; height: 12px; border-radius: 50%;
  background: rgba(255, 255, 255, 0.45); transform: translate(-50%, -50%) scale(0);
  pointer-events: none; animation: rip 0.6s var(--ease) forwards;
}
@keyframes rip { to { transform: translate(-50%, -50%) scale(22); opacity: 0; } }

.hero-ecg { flex: 0 0 340px; height: 140px; cursor: crosshair; }
.hero-ecg svg { width: 100%; height: 100%; overflow: visible; filter: drop-shadow(0 0 6px rgba(46, 111, 224, 0.35)); }
.hero-ecg g { will-change: transform; }

/* ============ 健康态势球 ============ */
.situation {
  position: relative;
  display: grid;
  grid-template-columns: minmax(190px, 260px) minmax(0, 1fr) minmax(240px, 320px);
  align-items: center;
  gap: 28px;
  padding: 30px;
  overflow: hidden;
  max-width: 100%;
}
.situation::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(46, 111, 224, .08), transparent 42%),
    repeating-linear-gradient(90deg, rgba(46, 111, 224, .055) 0 1px, transparent 1px 42px);
  opacity: .8;
  pointer-events: none;
}
.sphere-wrap, .situation-copy, .mini-events { position: relative; z-index: 1; }
.sphere {
  position: relative;
  width: 230px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  color: #2e6fe0;
}
.situation.excellent .sphere { color: #23a889; }
.situation.stable .sphere { color: #2e6fe0; }
.situation.watch .sphere { color: #ff8a00; }
.situation.risk .sphere { color: #e5654b; }
.orbit {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(46,111,224,.18);
  background: conic-gradient(from 110deg, transparent 0 18%, currentColor 34%, transparent 48% 100%);
  -webkit-mask: radial-gradient(circle, transparent 61%, #000 62%);
  mask: radial-gradient(circle, transparent 61%, #000 62%);
  animation: sphereSpin 8s linear infinite;
}
.o2 { inset: 20px; opacity: .65; animation-duration: 12s; animation-direction: reverse; }
.o3 { inset: 40px; opacity: .5; animation-duration: 6s; }
.pulse-dot {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 8px color-mix(in srgb, currentColor 18%, transparent);
  animation: dotPulse 2.2s var(--ease) infinite;
}
.d1 { left: 18%; top: 22%; }
.d2 { right: 14%; top: 36%; animation-delay: -.7s; }
.d3 { left: 28%; bottom: 18%; animation-delay: -1.1s; }
.d4 { right: 28%; bottom: 15%; animation-delay: -1.5s; }
.sphere-core {
  position: relative;
  z-index: 1;
  width: 126px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: radial-gradient(circle at 38% 28%, rgba(255,255,255,.98), rgba(232,244,255,.86));
  box-shadow: inset 0 0 28px rgba(46,111,224,.12), 0 18px 38px rgba(46,111,224,.14);
}
.sphere-core strong {
  color: var(--hda-ink);
  font-size: 44px;
  line-height: 1;
  font-weight: 900;
}
.sphere-core small {
  margin-top: 8px;
  color: var(--gray);
  font-weight: 700;
}
.sec-kicker {
  margin: 0 0 8px;
  color: var(--blue);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: .14em;
}
.situation-copy h3 {
  margin: 0;
  color: var(--hda-ink);
  font-size: 30px;
}
.situation-summary {
  margin: 10px 0 16px;
  color: var(--gray);
  line-height: 1.8;
}
.deductions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}
.deductions span {
  padding: 8px 10px;
  color: #526b89;
  background: rgba(255,255,255,.82);
  border: 1px solid #dce8f6;
  font-size: 13px;
  font-weight: 700;
}
.deductions b { color: #e5654b; }
.situation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.situation-actions .btn-main,
.situation-actions .btn-ghost {
  min-height: 42px;
  font-size: 15px;
}
.mini-events {
  display: grid;
  gap: 10px;
}
.mini-event {
  width: 100%;
  text-align: left;
  padding: 14px;
  background: rgba(255,255,255,.78);
  border: 1px solid #dce8f6;
  cursor: pointer;
  transition: transform .35s var(--ease), background-color .35s ease;
}
.mini-event:hover {
  transform: translateX(5px);
  background: #fff;
}
.mini-event span,
.mini-event small {
  display: block;
  color: #8a9bb0;
  font-size: 12px;
}
.mini-event b {
  display: block;
  margin: 4px 0;
  color: var(--hda-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
@keyframes sphereSpin { to { transform: rotate(360deg); } }
@keyframes dotPulse {
  0%, 100% { transform: scale(.82); }
  50% { transform: scale(1.18); }
}

/* ============ 统计卡片 ============ */
.stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; perspective: 800px; }
.stat {
  position: relative; padding: 24px; cursor: pointer; overflow: hidden;
  transform: perspective(800px) rotateX(var(--rx, 0deg)) rotateY(var(--ry, 0deg));
  transition: transform 0.5s var(--ease);
  will-change: transform;
}
.stat:focus-visible { outline: 3px solid var(--el-color-primary-light-7); outline-offset: 2px; }
.glare {
  position: absolute; inset: 0; border-radius: var(--r); pointer-events: none;
  background: radial-gradient(220px circle at var(--mx, 50%) var(--my, 50%), rgba(46, 111, 224, 0.1), transparent 65%);
  opacity: 0; transition: opacity 0.4s var(--ease);
}
.stat:hover .glare { opacity: 1; }
.stat-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.s-ic { display: grid; place-items: center; width: 26px; height: 26px; }
.s-ic :deep(svg) { width: 26px; height: 26px; }
.dot {
  width: 10px; height: 10px; border-radius: 50%; background: #e5654b;
  animation: pulse 1.6s var(--ease) infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(229, 101, 75, 0.45); }
  50% { box-shadow: 0 0 0 7px rgba(229, 101, 75, 0); }
}
.num { font-size: 34px; font-weight: 800; color: var(--hda-ink); line-height: 1.1; }
.lb { display: flex; align-items: baseline; gap: 8px; margin-top: 4px; font-size: 16px; color: var(--gray); }
.wk { font-size: 13px; color: #93a2b5; }
.spark { width: 120px; height: 36px; margin-top: 8px; display: block; }

/* ============ bento 快捷入口 ============ */
.sec-title { margin: 0 0 16px; font-size: 22px; color: var(--hda-ink); }
.bento { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.tile {
  position: relative; display: flex; align-items: center; gap: 16px;
  min-height: 96px; padding: 24px; cursor: pointer;
  transition: transform 0.4s var(--ease), box-shadow 0.4s var(--ease);
  will-change: transform;
}
.tile.c2 { grid-column: span 2; }
.tile.c1 { grid-column: span 1; }
.tile:hover { transform: translateY(-4px); }
.tile:focus-visible { outline: 3px solid var(--el-color-primary-light-7); outline-offset: 2px; }

/* 描边渐变流动：蓝紫渐变沿边框转一圈 */
.bd {
  position: absolute; inset: 0; border-radius: var(--r); padding: 1.5px; overflow: hidden;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor; mask-composite: exclude;
  opacity: 0; transition: opacity 0.4s var(--ease); pointer-events: none;
}
.bd::before {
  content: ""; position: absolute; left: 50%; top: 50%; width: 220%; height: 0; padding-bottom: 220%;
  background: conic-gradient(from 0deg, transparent 0 10%, var(--blue) 32%, var(--violet) 52%, #37b6d9 72%, transparent 90% 100%);
  transform: translate(-50%, -50%) rotate(0deg);
}
.tile:hover .bd { opacity: 1; }
.tile:hover .bd::before { animation: bdspin 1.2s linear 1; }
@keyframes bdspin { to { transform: translate(-50%, -50%) rotate(360deg); } }

.t-ic { flex: 0 0 30px; display: grid; place-items: center; width: 30px; height: 30px; }
.t-ic :deep(svg) { width: 30px; height: 30px; overflow: visible; }
.t-txt { min-width: 0; }
.t-name { font-size: 18px; font-weight: 700; color: var(--hda-ink); }
.t-desc { margin-top: 4px; font-size: 15px; color: var(--gray); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.t-arr { margin-left: auto; flex: 0 0 18px; width: 18px; height: 18px; color: #b3c0d0; transition: transform 0.4s var(--ease), color 0.4s var(--ease); }
.t-arr :deep(svg) { width: 18px; height: 18px; }
.tile:hover .t-arr { transform: translateX(4px); color: var(--blue); }

/* 图标微动画（各自独立 keyframes） */
.tile:hover .a-bell :deep(svg) { animation: bellSway 0.6s var(--ease); transform-origin: 50% 12%; }
@keyframes bellSway {
  0%, 100% { transform: rotate(0); }
  30% { transform: rotate(12deg); }
  60% { transform: rotate(-9deg); }
  82% { transform: rotate(4deg); }
}
.tile:hover .a-line :deep(.act-line) { stroke-dasharray: 64; animation: lineGrow 0.6s var(--ease) forwards; }
@keyframes lineGrow { from { stroke-dashoffset: 64; } to { stroke-dashoffset: 0; } }
.tile:hover .a-book :deep(.pg-r) { animation: pageFlip 0.6s var(--ease); transform-box: fill-box; transform-origin: 0% 50%; }
@keyframes pageFlip {
  0% { transform: rotateY(0); }
  50% { transform: rotateY(-65deg); }
  100% { transform: rotateY(0); }
}

/* ============ 响应式 ============ */
@media (max-width: 1100px) {
  .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .bento { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .tile.c2 { grid-column: span 2; }
  .hero-ecg { display: none; }
  .hero { padding: 32px 24px; }
  .situation { grid-template-columns: 220px 1fr; }
  .mini-events { grid-column: 1 / -1; grid-template-columns: repeat(3, 1fr); }
  .sphere { width: 200px; }
}
@media (max-width: 640px) {
  .stats, .bento { grid-template-columns: 1fr; }
  .tile.c2 { grid-column: span 1; }
  .situation { grid-template-columns: 1fr; padding: 22px; }
  .sphere { width: 190px; justify-self: center; }
  .mini-events { grid-template-columns: 1fr; }
  .situation-actions .btn-main,
  .situation-actions .btn-ghost { width: 100%; justify-content: center; }
}

/* ============ 适老化 / 减少动效 ============ */
@media (prefers-reduced-motion: reduce) {
  .dash *, .dash *::before, .dash *::after { animation: none !important; transition: none !important; }
}
</style>
