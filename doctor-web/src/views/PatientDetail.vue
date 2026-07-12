<template>
  <div class="detail-page">
    <div class="page-heading">
      <div>
        <el-button text class="back" @click="$router.push('/patients')"><el-icon><ArrowLeft /></el-icon>返回患者列表</el-button>
        <h1>患者详情</h1>
        <p>健康资料仅用于当前医疗服务场景。</p>
      </div>
    </div>

    <el-skeleton v-if="loading" animated :rows="8" class="detail-skeleton" />
    <template v-else-if="detail.patient">
      <section class="patient-summary glass-panel">
        <el-avatar :size="72" :src="detail.patient.avatar">{{ patientInitial }}</el-avatar>
        <div class="patient-main">
          <div class="name-line"><h2>{{ detail.patient.nickname || detail.patient.username }}</h2><el-tag type="success">账户正常</el-tag></div>
          <p>{{ detail.patient.username }} · {{ genderText(detail.patient.gender) }} · {{ ageText }}</p>
        </div>
        <div class="summary-meta">
          <span>最近健康资料</span>
          <b>{{ latestDataTime }}</b>
        </div>
      </section>

      <div class="detail-grid">
        <el-card class="profile-card">
          <template #header><div class="card-title"><el-icon><Postcard /></el-icon><span>健康档案</span></div></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="身高">{{ valueWithUnit(detail.profile?.height, 'cm') }}</el-descriptions-item>
            <el-descriptions-item label="体重">{{ valueWithUnit(detail.profile?.weight, 'kg') }}</el-descriptions-item>
            <el-descriptions-item label="血型">{{ detail.profile?.bloodType || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="过敏史">{{ detail.profile?.allergyHistory || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="既往史">{{ detail.profile?.pastHistory || '未填写' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card>
          <template #header><div class="card-title"><el-icon><DataLine /></el-icon><span>近期体征</span><small>最近 10 条</small></div></template>
          <el-table :data="detail.metrics || []" height="286">
            <el-table-column prop="metricType" label="类型" min-width="110" />
            <el-table-column label="数值" min-width="110"><template #default="{ row }"><b class="metric-value">{{ row.metricValue }} {{ row.unit || '' }}</b></template></el-table-column>
            <el-table-column prop="measureTime" label="测量时间" min-width="170" />
            <template #empty><el-empty description="暂无体征数据" :image-size="64" /></template>
          </el-table>
        </el-card>

        <el-card>
          <template #header><div class="card-title"><el-icon><FirstAidKit /></el-icon><span>就诊记录</span><small>最近 10 条</small></div></template>
          <el-table :data="detail.medicalRecords || []" height="286">
            <el-table-column prop="visitDate" label="日期" min-width="115" />
            <el-table-column prop="hospital" label="医院" min-width="145" show-overflow-tooltip />
            <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
            <template #empty><el-empty description="暂无就诊记录" :image-size="64" /></template>
          </el-table>
        </el-card>

        <el-card>
          <template #header><div class="card-title"><el-icon><Document /></el-icon><span>健康报告</span><small>最近 10 条</small></div></template>
          <el-table :data="detail.reports || []" height="286">
            <el-table-column prop="title" label="报告标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reportDate" label="报告日期" min-width="130" />
            <template #empty><el-empty description="暂无健康报告" :image-size="64" /></template>
          </el-table>
        </el-card>

        <el-card class="wide alerts-card">
          <template #header><div class="card-title"><el-icon><Bell /></el-icon><span>预警记录</span><small>最近 10 条</small></div></template>
          <el-table :data="detail.alerts || []">
            <el-table-column label="级别" width="110"><template #default="{ row }"><el-tag :type="alertType(row.level)">{{ levelText(row.level) }}</el-tag></template></el-table-column>
            <el-table-column prop="alertType" label="预警类型" min-width="150" />
            <el-table-column prop="content" label="预警内容" min-width="320" />
            <el-table-column prop="createTime" label="时间" min-width="180" />
            <template #empty><el-empty description="暂无预警记录" :image-size="64" /></template>
          </el-table>
        </el-card>
      </div>
    </template>
    <el-empty v-else description="未找到患者资料" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Bell, DataLine, Document, FirstAidKit, Postcard } from '@element-plus/icons-vue'
import { getPatientDetail } from '@/api'

const route = useRoute()
const detail = ref({})
const loading = ref(true)
const patientInitial = computed(() => (detail.value.patient?.nickname || detail.value.patient?.username || '患').charAt(0))
const ageText = computed(() => detail.value.age === null || detail.value.age === undefined ? '年龄未知' : `${detail.value.age} 岁`)
const latestDataTime = computed(() => {
  const times = [
    detail.value.metrics?.[0]?.measureTime,
    detail.value.medicalRecords?.[0]?.visitDate,
    detail.value.reports?.[0]?.reportDate,
    detail.value.alerts?.[0]?.createTime
  ].filter(Boolean).sort().reverse()
  return times[0] || '暂无记录'
})

function genderText(value) { return value === 1 ? '男' : value === 2 ? '女' : '保密' }
function valueWithUnit(value, unit) { return value === null || value === undefined ? '未填写' : `${value} ${unit}` }
function alertType(level) { return level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'info' }
function levelText(level) { return { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[level] || level || '未知' }

onMounted(async () => {
  try { detail.value = (await getPatientDetail(route.params.id)).data || {} }
  finally { loading.value = false }
})
</script>

<style scoped>
.back { margin: 0 0 8px -12px; color: var(--el-color-primary); }
.detail-skeleton { padding: 28px; background: rgba(255,255,255,.7); }
.patient-summary { min-height: 126px; margin-bottom: 18px; padding: 24px 28px; display: flex; align-items: center; gap: 18px; }
.patient-summary .el-avatar { flex: 0 0 auto; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 25px; font-weight: 700; }
.patient-main { min-width: 0; }
.name-line { display: flex; align-items: center; gap: 12px; }
.name-line h2 { margin: 0; color: var(--hda-ink); font-size: 25px; }
.patient-main p { margin: 5px 0 0; color: var(--hda-ink-soft); }
.summary-meta { margin-left: auto; padding-left: 28px; border-left: 1px solid var(--hda-line); display: flex; flex-direction: column; align-items: flex-end; }
.summary-meta span { color: #8B9AAF; font-size: 13px; }
.summary-meta b { color: var(--hda-ink); font-size: 15px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1.4fr; gap: 16px; }
.wide { grid-column: 1 / -1; }
.card-title { display: flex; align-items: center; gap: 9px; color: var(--hda-ink); font-size: 17px; font-weight: 700; }
.card-title .el-icon { color: var(--el-color-primary); font-size: 20px; }
.card-title small { margin-left: auto; color: #93A2B5; font-size: 12px; font-weight: 400; }
.metric-value { color: var(--el-color-primary); font-family: var(--hda-font-display); }
.profile-card :deep(.el-descriptions__label) { width: 100px; color: #718198; }
.profile-card :deep(.el-descriptions__content) { color: var(--hda-ink); }
.detail-grid :deep(.el-card__body) { padding: 18px 20px; }
.detail-grid :deep(.el-table .el-empty) { padding: 20px 0; }
@media (max-width: 960px) { .detail-grid { grid-template-columns: 1fr; } .wide { grid-column: auto; } }
@media (max-width: 620px) { .patient-summary { align-items: flex-start; padding: 20px; } .summary-meta { display: none; } .name-line { align-items: flex-start; flex-direction: column; gap: 5px; } }
</style>
