<template>
  <div class="overview-page hda-enter">
    <OverviewHero
      variant="health"
      eyebrow="HEALTH RECORDS"
      title="健康档案"
      description="集中查看个人基础资料、体征趋势、就诊记录与健康报告，形成连续的健康数据时间线。"
      cta-label="完善档案"
      @action="go('/health/profile')"
    />

    <section class="summary-strip">
      <div v-for="(item, index) in summaryItems" :key="item.label" class="summary-item" :style="{ '--index': index }">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="module-grid">
      <article
        v-for="(item, index) in modules"
        :key="item.path"
        class="module-card"
        role="button"
        tabindex="0"
        :style="{ '--index': index }"
        @click="go(item.path)"
        @keyup.enter="go(item.path)"
        @keyup.space.prevent="go(item.path)"
      >
        <span class="module-accent" aria-hidden="true"></span>
        <div class="module-icon"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="module-copy">
          <h2>{{ item.title }}</h2>
          <p>{{ item.desc }}</p>
        </div>
        <span class="module-action">进入<el-icon><ArrowRight /></el-icon></span>
      </article>
    </section>

    <section class="preview-grid">
      <article class="preview-panel" style="--index: 0">
        <div class="panel-head"><h3>基础信息</h3><el-button link type="primary" @click="go('/health/profile')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <dl v-else class="profile-list">
          <div><dt>身高</dt><dd>{{ profile.height || '-' }} cm</dd></div>
          <div><dt>体重</dt><dd>{{ profile.weight || '-' }} kg</dd></div>
          <div><dt>BMI</dt><dd>{{ bmi }}</dd></div>
          <div><dt>血型</dt><dd>{{ profile.bloodType || '-' }}</dd></div>
        </dl>
      </article>

      <article class="preview-panel" style="--index: 1">
        <div class="panel-head"><h3>最近体征</h3><el-button link type="primary" @click="go('/health/metric')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <div v-else-if="latestMetric" class="latest-line">
          <strong>{{ metricName(latestMetric.metricType) }}</strong>
          <span>{{ metricValue(latestMetric) }}</span>
          <small>{{ formatTime(latestMetric.measureTime) }}</small>
        </div>
        <el-empty v-else description="暂无体征记录" :image-size="70" />
      </article>

      <article class="preview-panel" style="--index: 2">
        <div class="panel-head"><h3>最近就诊</h3><el-button link type="primary" @click="go('/health/medical')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <div v-else-if="latestMedical" class="latest-line">
          <strong>{{ latestMedical.hospital || '未记录医院' }}</strong>
          <span>{{ latestMedical.diagnosis || '未填写诊断' }}</span>
          <small>{{ latestMedical.visitDate || '-' }}</small>
        </div>
        <el-empty v-else description="暂无就诊记录" :image-size="70" />
      </article>

      <article class="preview-panel" style="--index: 3">
        <div class="panel-head"><h3>健康报告</h3><el-button link type="primary" @click="go('/health/report')">查看</el-button></div>
        <el-skeleton v-if="loading" :rows="3" animated />
        <div v-else-if="latestReport" class="latest-line">
          <strong>{{ latestReport.title || '健康报告' }}</strong>
          <span>{{ latestReport.summary || latestReport.content || '点击查看报告详情' }}</span>
          <small>{{ formatTime(latestReport.createTime || latestReport.reportTime) }}</small>
        </div>
        <el-empty v-else description="暂无健康报告" :image-size="70" />
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, DataLine, Document, Tickets, User } from '@element-plus/icons-vue'
import { getHealthProfile, pageMedical, pageMetric, pageReport } from '@/api'
import OverviewHero from '@/components/OverviewHero.vue'

const router = useRouter()
const profile = ref({})
const totals = ref({ metric: 0, medical: 0, report: 0 })
const latestMetric = ref(null)
const latestMedical = ref(null)
const latestReport = ref(null)
const loading = ref(true)

const modules = [
  { title: '基本信息', desc: '维护身高、体重、血型、过敏史与紧急联系人。', path: '/health/profile', icon: User },
  { title: '体征数据', desc: '记录血压、血糖、心率、体温等日常指标。', path: '/health/metric', icon: DataLine },
  { title: '就诊记录', desc: '保存医院、科室、诊断与处方用药信息。', path: '/health/medical', icon: Tickets },
  { title: '健康报告', desc: '查看周期性健康分析和系统生成报告。', path: '/health/report', icon: Document },
]

const completeness = computed(() => {
  const keys = ['height', 'weight', 'bloodType', 'allergyHistory', 'familyHistory', 'pastHistory', 'emergencyContact', 'emergencyPhone']
  const done = keys.filter(key => profile.value[key]).length
  return Math.round(done / keys.length * 100)
})

const summaryItems = computed(() => [
  { label: '档案完整度', value: `${completeness.value}%` },
  { label: '体征记录', value: totals.value.metric },
  { label: '就诊记录', value: totals.value.medical },
  { label: '健康报告', value: totals.value.report },
])

const bmi = computed(() => {
  if (!profile.value.height || !profile.value.weight) return '-'
  const h = profile.value.height / 100
  return (profile.value.weight / (h * h)).toFixed(1)
})

function go(path) { router.push(path) }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function metricName(type) {
  return ({ BLOOD_PRESSURE: '血压', BLOOD_SUGAR: '血糖', HEART_RATE: '心率', TEMPERATURE: '体温', WEIGHT: '体重' })[type] || type || '体征'
}
function metricValue(row) {
  if (!row) return '-'
  return row.metricValue2 ? `${row.metricValue}/${row.metricValue2} ${row.unit || ''}` : `${row.metricValue ?? '-'} ${row.unit || ''}`
}

async function load() {
  try {
    const [profileRes, metricRes, medicalRes, reportRes] = await Promise.allSettled([
      getHealthProfile(),
      pageMetric({ pageNum: 1, pageSize: 1 }),
      pageMedical({ pageNum: 1, pageSize: 1 }),
      pageReport({ pageNum: 1, pageSize: 1 }),
    ])
    if (profileRes.status === 'fulfilled') profile.value = profileRes.value.data || {}
    if (metricRes.status === 'fulfilled') {
      totals.value.metric = metricRes.value.data.total || 0
      latestMetric.value = metricRes.value.data.records?.[0] || null
    }
    if (medicalRes.status === 'fulfilled') {
      totals.value.medical = medicalRes.value.data.total || 0
      latestMedical.value = medicalRes.value.data.records?.[0] || null
    }
    if (reportRes.status === 'fulfilled') {
      totals.value.report = reportRes.value.data.total || 0
      latestReport.value = reportRes.value.data.records?.[0] || null
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.overview-page {
  --overview-ease: cubic-bezier(.22, 1, .36, 1);
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1380px;
  margin: 0 auto;
}
.summary-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; overflow: hidden; border: 1px solid var(--hda-line); background: var(--hda-line); }
.summary-strip div { display: flex; align-items: baseline; justify-content: space-between; padding: 20px 24px; background: rgba(255,255,255,.86); }
.summary-item { position: relative; transition: background-color .35s ease, transform .35s var(--overview-ease); }
.summary-item::after { content: ""; position: absolute; left: 24px; right: 24px; bottom: 0; height: 2px; background: linear-gradient(90deg, #2e6fe0, #37b6d9); transform: scaleX(0); transform-origin: left; transition: transform .45s var(--overview-ease); }
.summary-item:hover { z-index: 1; background: #fff; transform: translateY(-2px); }
.summary-item:hover::after { transform: scaleX(1); }
.summary-strip span { color: var(--hda-ink-soft); font-size: 14px; }
.summary-strip strong { color: var(--hda-ink); font-size: 30px; font-variant-numeric: tabular-nums; }
.module-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.module-card { position: relative; display: grid; grid-template-columns: 46px 1fr auto; align-items: center; gap: 14px; min-height: 140px; padding: 22px; overflow: hidden; cursor: pointer; background: rgba(255,255,255,.78); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .4s var(--overview-ease), box-shadow .4s ease, border-color .4s ease, background-color .4s ease; }
.module-accent { position: absolute; inset: 0 auto 0 0; width: 3px; background: linear-gradient(180deg, #2e6fe0, #37b6d9); transform: scaleY(0); transform-origin: center; transition: transform .45s var(--overview-ease); }
.module-card:hover { transform: translateY(-6px); border-color: var(--el-color-primary-light-5); background: #fff; box-shadow: var(--hda-shadow); }
.module-card:hover .module-accent { transform: scaleY(1); }
.module-card:active { transform: translateY(-1px) scale(.98); transition-duration: .12s; }
.module-card:focus-visible { outline: 3px solid var(--el-color-primary-light-7); outline-offset: 2px; }
.module-icon { width: 46px; height: 46px; display: grid; place-items: center; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 24px; transition: transform .45s var(--ease-spring), color .35s ease, background-color .35s ease; }
.module-card:hover .module-icon { transform: translateY(-3px) scale(1.08); color: #fff; background: linear-gradient(145deg, #2e6fe0, #37a9d5); }
.module-copy { min-width: 0; }
.module-card h2 { margin: 0 0 6px; font-size: 20px; letter-spacing: 0; }
.module-card p { margin: 0; color: var(--hda-ink-soft); font-size: 14px; line-height: 1.55; }
.module-action { display: inline-flex; align-items: center; gap: 5px; color: var(--el-color-primary); font-size: 15px; font-weight: 700; white-space: nowrap; }
.module-action .el-icon { transition: transform .4s var(--overview-ease); }
.module-card:hover .module-action .el-icon { transform: translateX(5px); }
.preview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.preview-panel { min-height: 210px; padding: 22px; background: rgba(255,255,255,.78); border: 1px solid var(--hda-line); box-shadow: var(--hda-shadow-sm); transition: transform .38s var(--overview-ease), box-shadow .38s ease, background-color .38s ease; }
.preview-panel:hover { transform: translateY(-3px); background: rgba(255,255,255,.96); box-shadow: var(--hda-shadow); }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.panel-head h3 { margin: 0; font-size: 18px; }
.profile-list { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 0; }
.profile-list div { padding: 12px; background: #f6f9fd; transition: transform .3s var(--overview-ease), background-color .3s ease; }
.profile-list div:hover { transform: translateY(-2px); background: var(--el-color-primary-light-9); }
.profile-list dt { color: var(--hda-ink-soft); font-size: 13px; }
.profile-list dd { margin: 3px 0 0; font-size: 18px; font-weight: 700; }
.latest-line strong, .latest-line span, .latest-line small { display: block; }
.latest-line strong { font-size: 19px; }
.latest-line span { height: 52px; margin: 10px 0; overflow: hidden; color: var(--hda-ink-soft); line-height: 1.65; }
.latest-line small { color: #90a2b8; }
@media (prefers-reduced-motion: no-preference) {
  .summary-item, .module-card, .preview-panel { animation: sectionRise .58s var(--overview-ease) both; animation-delay: calc(var(--index) * 70ms + 100ms); }
}
@keyframes sectionRise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 1100px) {
  .module-grid, .preview-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 760px) {
  .summary-strip, .module-grid, .preview-grid { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .overview-page *, .overview-page *::before, .overview-page *::after { animation: none !important; transition: none !important; }
  .module-card:hover, .preview-panel:hover, .summary-item:hover { transform: none; }
}
</style>
