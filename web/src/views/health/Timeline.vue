<template>
  <div class="timeline-page hda-enter">
    <section class="timeline-hero">
      <div class="hero-copy">
        <p class="eyebrow">HEALTH ORBIT</p>
        <h1>患者健康时间轴</h1>
        <p>{{ situation.summary }}</p>
      </div>
      <div class="score-orbit" :class="situation.tone">
        <div class="orbit-ring r1"></div>
        <div class="orbit-ring r2"></div>
        <div class="orbit-ring r3"></div>
        <div class="score-core">
          <span>{{ loading ? '--' : situation.score }}</span>
          <small>{{ situation.level }}</small>
        </div>
      </div>
    </section>

    <section class="insight-grid">
      <article v-for="item in insightCards" :key="item.key" class="insight-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="timeline-shell">
      <div class="timeline-toolbar">
        <div>
          <h2>健康事件流</h2>
          <p>自动聚合体征、预警、报告、就诊、咨询和用药建议</p>
        </div>
        <div class="filter-tabs">
          <button
            v-for="item in filters"
            :key="item.value"
            :class="{ on: activeFilter === item.value }"
            type="button"
            @click="activeFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <el-skeleton v-if="loading" :rows="7" animated />
      <div v-else-if="filteredEvents.length" class="event-list">
        <article
          v-for="(event, index) in filteredEvents"
          :key="event.id"
          class="event-item"
          :class="typeMeta(event.type).tone"
          :style="{ '--index': index }"
        >
          <div class="event-time">
            <strong>{{ datePart(event.time) }}</strong>
            <span>{{ clockPart(event.time) }}</span>
          </div>
          <div class="event-node"></div>
          <div class="event-card">
            <div class="event-head">
              <span>{{ typeMeta(event.type).label }}</span>
              <el-tag size="small" :type="tagType(event)">{{ event.status }}</el-tag>
            </div>
            <h3>{{ event.title }}</h3>
            <p>{{ event.desc }}</p>
            <el-button link type="primary" @click="go(event.actionPath)">{{ actionLabel(event) }}</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无可展示的健康事件" :image-size="86" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  emptyHealthSituation,
  formatTimelineTime,
  loadHealthSituation,
  typeMeta,
} from '@/utils/healthSituation'

const router = useRouter()
const loading = ref(true)
const situation = ref(emptyHealthSituation())
const activeFilter = ref('all')

const filters = [
  { label: '全部', value: 'all' },
  { label: '体征', value: 'metric' },
  { label: '预警', value: 'alert' },
  { label: '报告', value: 'report' },
  { label: '就诊', value: 'medical' },
  { label: '咨询', value: 'consult' },
  { label: '用药', value: 'medicine' },
]

const insightCards = computed(() => [
  {
    key: 'metric',
    label: '体征异常',
    value: situation.value.metrics.filter(item => Number(item.abnormal) === 1).length,
    desc: '来自最近体征数据和系统阈值判断',
  },
  {
    key: 'alert',
    label: '未读预警',
    value: situation.value.alerts.filter(item => Number(item.readFlag) !== 1).length,
    desc: '建议优先处理高危和中度预警',
  },
  {
    key: 'consult',
    label: '咨询闭环',
    value: situation.value.sessions.filter(item => (item.session || item).status === 'CLOSED').length,
    desc: '统计已结束的医生咨询及报告状态',
  },
  {
    key: 'medicine',
    label: '待确认用药',
    value: situation.value.medicationAdvices.filter(item => item.status === 'PENDING_CONFIRM').length,
    desc: '医生给出的用药建议需要患者确认',
  },
])

const filteredEvents = computed(() => {
  if (activeFilter.value === 'all') return situation.value.events
  return situation.value.events.filter(item => item.type === activeFilter.value)
})

function datePart(value) {
  const text = formatTimelineTime(value)
  return text.includes(' ') ? text.split(' ')[0].slice(5) : text.slice(5, 10) || '-'
}

function clockPart(value) {
  const text = formatTimelineTime(value)
  return text.includes(' ') ? text.split(' ')[1] : '全天'
}

function tagType(event) {
  if (['异常', '未处理', '待确认', '待报告'].includes(event.status)) return 'danger'
  if (['进行中', '含用药'].includes(event.status)) return 'warning'
  if (['正常', '已确认', '已出报告', '已归档'].includes(event.status)) return 'success'
  return 'info'
}

function actionLabel(event) {
  return event.type === 'consult' || event.type === 'medicine' ? '进入对话' : '查看来源'
}

function go(path) {
  router.push(path)
}

async function load() {
  loading.value = true
  try {
    situation.value = await loadHealthSituation({ metricSize: 100, sessionSize: 40 })
  } catch (error) {
    ElMessage.error(error.message || '健康时间轴加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.timeline-page {
  --tl-ease: cubic-bezier(.22, 1, .36, 1);
  max-width: 1360px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.timeline-hero {
  position: relative;
  min-height: 300px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: center;
  gap: 28px;
  padding: 42px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255,255,255,.92), rgba(235,246,255,.82)),
    repeating-linear-gradient(90deg, rgba(46,111,224,.05) 0 1px, transparent 1px 48px),
    repeating-linear-gradient(0deg, rgba(55,182,217,.05) 0 1px, transparent 1px 48px);
  border: 1px solid rgba(210,224,243,.9);
  box-shadow: var(--hda-shadow-sm);
}
.hero-copy { position: relative; z-index: 1; }
.eyebrow {
  margin: 0 0 10px;
  color: #2e6fe0;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: .16em;
}
.hero-copy h1 {
  margin: 0;
  font-size: clamp(32px, 5vw, 58px);
  line-height: 1.05;
  color: var(--hda-ink);
}
.hero-copy p:last-child {
  max-width: 620px;
  margin: 16px 0 0;
  color: var(--hda-ink-soft);
  font-size: 17px;
  line-height: 1.8;
}
.score-orbit {
  position: relative;
  width: 260px;
  aspect-ratio: 1;
  justify-self: center;
  display: grid;
  place-items: center;
  color: #2e6fe0;
}
.orbit-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid rgba(46,111,224,.22);
  background:
    conic-gradient(from 120deg, transparent 0 20%, currentColor 38%, transparent 50% 100%);
  -webkit-mask: radial-gradient(circle, transparent 59%, #000 60%);
  mask: radial-gradient(circle, transparent 59%, #000 60%);
  animation: orbitSpin 9s linear infinite;
}
.r2 {
  inset: 22px;
  opacity: .62;
  animation-duration: 13s;
  animation-direction: reverse;
}
.r3 {
  inset: 44px;
  opacity: .52;
  animation-duration: 7s;
}
.score-core {
  position: relative;
  z-index: 1;
  width: 142px;
  aspect-ratio: 1;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: radial-gradient(circle at 38% 30%, rgba(255,255,255,.96), rgba(232,244,255,.88));
  box-shadow: inset 0 0 34px rgba(46,111,224,.12), 0 18px 46px rgba(46,111,224,.16);
}
.score-core span {
  color: var(--hda-ink);
  font-size: 48px;
  font-weight: 900;
  line-height: 1;
}
.score-core small {
  margin-top: 8px;
  color: var(--hda-ink-soft);
  font-size: 14px;
}
.score-orbit.excellent { color: #23a889; }
.score-orbit.stable { color: #2e6fe0; }
.score-orbit.watch { color: #ff8a00; }
.score-orbit.risk { color: #e5654b; }
@keyframes orbitSpin { to { transform: rotate(360deg); } }

.insight-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.insight-card {
  padding: 22px;
  background: rgba(255,255,255,.82);
  border: 1px solid var(--hda-line);
  box-shadow: var(--hda-shadow-sm);
  transition: transform .35s var(--tl-ease), box-shadow .35s ease;
}
.insight-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--hda-shadow);
}
.insight-card span {
  color: var(--hda-ink-soft);
  font-size: 14px;
}
.insight-card strong {
  display: block;
  margin-top: 8px;
  color: var(--hda-ink);
  font-size: 34px;
  line-height: 1;
}
.insight-card p {
  margin: 10px 0 0;
  color: #7a8ca5;
  line-height: 1.65;
}

.timeline-shell {
  padding: 26px;
  background: rgba(255,255,255,.84);
  border: 1px solid var(--hda-line);
  box-shadow: var(--hda-shadow-sm);
}
.timeline-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}
.timeline-toolbar h2 {
  margin: 0;
  color: var(--hda-ink);
  font-size: 24px;
}
.timeline-toolbar p {
  margin: 6px 0 0;
  color: var(--hda-ink-soft);
}
.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}
.filter-tabs button {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #d7e4f4;
  background: #fff;
  color: #58708e;
  cursor: pointer;
  font-weight: 700;
  transition: transform .25s var(--tl-ease), color .25s ease, background-color .25s ease, border-color .25s ease;
}
.filter-tabs button:hover,
.filter-tabs button.on {
  color: #fff;
  background: #2e6fe0;
  border-color: #2e6fe0;
  transform: translateY(-2px);
}
.filter-tabs button:active { transform: translateY(0) scale(.98); }
.event-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.event-list::before {
  content: "";
  position: absolute;
  left: 120px;
  top: 12px;
  bottom: 12px;
  width: 1px;
  background: linear-gradient(180deg, transparent, #b7d2f6 8%, #b7d2f6 92%, transparent);
}
.event-item {
  position: relative;
  display: grid;
  grid-template-columns: 94px 52px minmax(0, 1fr);
  gap: 0;
  padding: 8px 0;
}
.event-time {
  padding-top: 20px;
  text-align: right;
  color: var(--hda-ink-soft);
  font-variant-numeric: tabular-nums;
}
.event-time strong {
  display: block;
  color: var(--hda-ink);
}
.event-time span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
}
.event-node {
  position: relative;
  z-index: 1;
  justify-self: center;
  margin-top: 25px;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: #2e6fe0;
  box-shadow: 0 0 0 7px rgba(46,111,224,.12);
}
.event-card {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #dce7f4;
  box-shadow: 0 12px 26px rgba(46,111,224,.06);
  transition: transform .35s var(--tl-ease), border-color .35s ease, box-shadow .35s ease;
}
.event-card:hover {
  transform: translateX(6px);
  border-color: #9fc2f5;
  box-shadow: var(--hda-shadow);
}
.event-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.event-head span {
  color: #5d7189;
  font-size: 13px;
  font-weight: 800;
}
.event-card h3 {
  margin: 8px 0 6px;
  color: var(--hda-ink);
  font-size: 19px;
}
.event-card p {
  margin: 0 0 8px;
  color: var(--hda-ink-soft);
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.event-item.red .event-node { background: #e5654b; box-shadow: 0 0 0 7px rgba(229,101,75,.14); }
.event-item.cyan .event-node { background: #37b6d9; box-shadow: 0 0 0 7px rgba(55,182,217,.14); }
.event-item.green .event-node { background: #23a889; box-shadow: 0 0 0 7px rgba(35,168,137,.14); }
.event-item.violet .event-node { background: #7b5be6; box-shadow: 0 0 0 7px rgba(123,91,230,.14); }
.event-item.orange .event-node { background: #ff8a00; box-shadow: 0 0 0 7px rgba(255,138,0,.14); }
@media (prefers-reduced-motion: no-preference) {
  .event-item {
    animation: eventRise .5s var(--tl-ease) both;
    animation-delay: calc(var(--index) * 45ms);
  }
}
@keyframes eventRise {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (max-width: 1100px) {
  .timeline-hero { grid-template-columns: 1fr; }
  .score-orbit { width: 220px; }
  .insight-grid { grid-template-columns: repeat(2, 1fr); }
  .timeline-toolbar { flex-direction: column; }
  .filter-tabs { justify-content: flex-start; }
}
@media (max-width: 720px) {
  .timeline-hero, .timeline-shell { padding: 22px; }
  .insight-grid { grid-template-columns: 1fr; }
  .event-list::before { left: 8px; }
  .event-item { grid-template-columns: 20px minmax(0, 1fr); gap: 12px; }
  .event-time { grid-column: 2; text-align: left; padding-top: 0; }
  .event-node { grid-column: 1; grid-row: 1 / span 2; margin-top: 7px; }
  .event-card { grid-column: 2; }
}
@media (prefers-reduced-motion: reduce) {
  .timeline-page *, .timeline-page *::before, .timeline-page *::after {
    animation: none !important;
    transition: none !important;
  }
  .event-card:hover, .insight-card:hover, .filter-tabs button:hover { transform: none; }
}
</style>
