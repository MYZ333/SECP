<template>
  <div class="metric-page">
    <!-- 趋势图 -->
    <el-card class="trend-card">
      <div class="trend-head">
        <h3>体征趋势</h3>
        <el-radio-group v-model="trendType" @change="buildTrend">
          <el-radio-button v-for="t in types" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="chartBox" class="chart-box">
        <svg v-if="trend.points.length >= 2" :viewBox="`0 0 ${W} ${H}`" class="trend-svg">
          <defs>
            <linearGradient id="areaGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stop-color="#2E6FE0" stop-opacity="0.16" />
              <stop offset="1" stop-color="#2E6FE0" stop-opacity="0" />
            </linearGradient>
          </defs>

          <!-- 横向网格 + Y 轴刻度 -->
          <g v-for="tk in trend.ticks" :key="'g' + tk.v">
            <line :x1="padL" :x2="W - padR" :y1="tk.y" :y2="tk.y" stroke="#E8EFF8" stroke-width="1" />
            <text :x="padL - 8" :y="tk.y + 4" text-anchor="end" class="ax">{{ tk.v }}</text>
          </g>

          <!-- X 轴日期 -->
          <text v-for="xl in trend.xLabels" :key="'x' + xl.x" :x="xl.x" :y="H - 10"
                text-anchor="middle" class="ax">{{ xl.label }}</text>

          <!-- 渐变面积 -->
          <path ref="areaRef" :d="trend.area" fill="url(#areaGrad)" stroke="none" />

          <!-- 主线（血压=收缩压），平滑曲线 -->
          <path ref="lineRef" :d="trend.path1" fill="none" stroke="#2E6FE0" stroke-width="2.5"
                stroke-linecap="round" stroke-linejoin="round" />
          <!-- 血压第二条线（舒张压） -->
          <path v-if="trend.path2" ref="line2Ref" :d="trend.path2" fill="none" stroke="#37B6D9"
                stroke-width="2" stroke-dasharray="6 5" stroke-linecap="round" />

          <!-- 数据点：正常=白底蓝描边；异常=实心红点+脉冲光环 -->
          <g v-for="(p, i) in trend.points" :key="i">
            <circle v-if="p.abnormal" :cx="p.x" :cy="p.y" r="8" class="dot-halo"
                    fill="none" stroke="#E5654B" stroke-width="1.5" />
            <circle class="dot-pt" :class="{ 'dot-abn': p.abnormal }"
                    :cx="p.x" :cy="p.y" r="4"
                    :fill="p.abnormal ? '#E5654B' : '#fff'"
                    :stroke="p.abnormal ? '#fff' : '#2E6FE0'" stroke-width="2">
              <title>{{ p.tip }}</title>
            </circle>
          </g>

          <!-- 最新值标注 -->
          <g v-if="trend.last" ref="lastRef" class="last-tag">
            <rect :x="trend.last.bx" :y="trend.last.by" :width="trend.last.bw" height="22" rx="6"
                  fill="#2E6FE0" opacity="0.92" />
            <text :x="trend.last.bx + trend.last.bw / 2" :y="trend.last.by + 15"
                  text-anchor="middle" class="last-txt">{{ trend.last.text }}</text>
          </g>
        </svg>
        <el-empty v-else description="该指标记录不足 2 条，暂无法绘制趋势" :image-size="60" />
      </div>
      <p v-if="trend.points.length >= 2" class="trend-note">
        近 {{ trend.points.length }} 次记录
        <template v-if="trend.path2">（实线收缩压 / 虚线舒张压）</template>
        · 红点为异常值 · 悬停数据点可查看详情
      </p>
    </el-card>

    <!-- 记录表格 -->
    <el-card>
      <el-button type="primary" @click="openDialog()">新增体征记录</el-button>
      <span class="auto-hint">保存后系统按医学阈值自动判断是否异常</span>
      <el-table :data="list" style="margin-top:15px" border>
        <el-table-column label="指标类型"><template #default="{ row }">{{ typeName(row.metricType) }}</template></el-table-column>
        <el-table-column prop="metricValue" label="数值" />
        <el-table-column prop="metricValue2" label="第二值" />
        <el-table-column prop="unit" label="单位" />
        <el-table-column label="测量时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.measureTime) }}</template>
        </el-table-column>
        <el-table-column label="是否异常">
          <template #default="{ row }">
            <el-tag :type="row.abnormal ? 'danger' : 'success'">{{ row.abnormal ? '异常' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                     :page-size="query.pageSize" @current-change="onPage" />

      <el-dialog v-model="dialog" :title="form.id ? '编辑' : '新增'" width="500px" append-to-body>
        <el-form :model="form" label-width="100px">
          <el-form-item label="指标类型">
            <el-select v-model="form.metricType" @change="fillUnit">
              <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="form.metricType === 'BLOOD_PRESSURE' ? '收缩压' : '数值'">
            <el-input-number v-model="form.metricValue" :precision="1" style="width: 100%"
                             controls-position="right" :placeholder="form.metricType === 'BLOOD_PRESSURE' ? '如 120' : '请输入数值'" />
          </el-form-item>
          <el-form-item v-if="form.metricType === 'BLOOD_PRESSURE'" label="舒张压">
            <el-input-number v-model="form.metricValue2" :precision="1" style="width: 100%"
                             controls-position="right" placeholder="如 80" />
          </el-form-item>
          <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
          <el-form-item label="测量时间">
            <el-date-picker v-model="form.measureTime" type="datetime" style="width: 100%"
                            value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不填默认当前时间" />
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
        </el-form>
        <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import gsap from 'gsap'
import { pageMetric, createMetric, updateMetric, deleteMetric } from '@/api'

const H = 260, padL = 44, padR = 16, padT = 24, padB = 34
const W = ref(760)
const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches

const types = [
  { label: '血压', value: 'BLOOD_PRESSURE', unit: 'mmHg' },
  { label: '血糖', value: 'BLOOD_SUGAR', unit: 'mmol/L' },
  { label: '心率', value: 'HEART_RATE', unit: '次/分' },
  { label: '体温', value: 'TEMPERATURE', unit: '℃' },
  { label: '体重', value: 'WEIGHT', unit: 'kg' },
]
const typeName = (v) => types.find(t => t.value === v)?.label || v
const fmtTime = (t) => String(t || '').replace('T', ' ').slice(0, 19)

const list = ref([]), total = ref(0), dialog = ref(false), form = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })
const trendType = ref('BLOOD_PRESSURE')
const allRecords = ref([])
const trend = ref({ points: [], path1: '', path2: null, area: '', ticks: [], xLabels: [], last: null })
const chartBox = ref(null), lineRef = ref(null), line2Ref = ref(null), areaRef = ref(null), lastRef = ref(null)

async function load() {
  const res = await pageMetric(query.value)
  list.value = res.data.records
  total.value = res.data.total
  const all = await pageMetric({ pageNum: 1, pageSize: 100 })
  allRecords.value = all.data.records || []
  buildTrend()
}

/** Catmull-Rom 平滑曲线 */
function smoothPath(pts) {
  if (pts.length < 2) return ''
  let d = `M${pts[0].x.toFixed(1)},${pts[0].y.toFixed(1)}`
  for (let i = 0; i < pts.length - 1; i++) {
    const p0 = pts[i - 1] || pts[i], p1 = pts[i], p2 = pts[i + 1], p3 = pts[i + 2] || p2
    const c1x = p1.x + (p2.x - p0.x) / 6, c1y = p1.y + (p2.y - p0.y) / 6
    const c2x = p2.x - (p3.x - p1.x) / 6, c2y = p2.y - (p3.y - p1.y) / 6
    d += ` C${c1x.toFixed(1)},${c1y.toFixed(1)} ${c2x.toFixed(1)},${c2y.toFixed(1)} ${p2.x.toFixed(1)},${p2.y.toFixed(1)}`
  }
  return d
}

/** 构建趋势数据并触发入场动画 */
function buildTrend() {
  const rows = allRecords.value
    .filter(r => r.metricType === trendType.value && r.metricValue != null)
    .sort((a, b) => String(a.measureTime).localeCompare(String(b.measureTime)))
    .slice(-30)
  const unit = types.find(t => t.value === trendType.value)?.unit || ''
  if (rows.length < 2) {
    trend.value = { points: [], path1: '', path2: null, area: '', ticks: [], xLabels: [], last: null }
    return
  }

  const isBP = trendType.value === 'BLOOD_PRESSURE'
  const vals = rows.flatMap(r => isBP && r.metricValue2 != null ? [r.metricValue, r.metricValue2] : [r.metricValue])
  const span = Math.max(Math.max(...vals) - Math.min(...vals), 1)
  const min = Math.floor(Math.min(...vals) - span * 0.12)
  const max = Math.ceil(Math.max(...vals) + span * 0.12)

  const x = (i) => padL + i * (W.value - padL - padR) / (rows.length - 1)
  const y = (v) => padT + (1 - (v - min) / Math.max(max - min, 1)) * (H - padT - padB)

  const p1 = rows.map((r, i) => ({ x: x(i), y: y(r.metricValue) }))
  const points = rows.map((r, i) => ({
    x: x(i), y: y(r.metricValue), abnormal: r.abnormal === 1,
    tip: `${fmtTime(r.measureTime).slice(0, 16)}  ${r.metricValue}${isBP && r.metricValue2 != null ? '/' + r.metricValue2 : ''} ${unit}${r.abnormal === 1 ? '（异常）' : ''}`,
  }))
  const path1 = smoothPath(p1)
  const area = `${path1} L${p1[p1.length - 1].x.toFixed(1)},${H - padB} L${p1[0].x.toFixed(1)},${H - padB} Z`
  const path2 = isBP && rows.every(r => r.metricValue2 != null)
    ? smoothPath(rows.map((r, i) => ({ x: x(i), y: y(r.metricValue2) })))
    : null

  // Y 轴 4 等分刻度
  const ticks = [...Array(5)].map((_, i) => {
    const v = Math.round(min + (max - min) * i / 4)
    return { v, y: y(v) }
  })
  // X 轴日期标签（最多 6 个，均匀取样）
  const step = Math.max(1, Math.ceil(rows.length / 6))
  const xLabels = rows.map((r, i) => ({ i, x: x(i), label: fmtTime(r.measureTime).slice(5, 10) }))
    .filter((_, i) => i % step === 0 || i === rows.length - 1)

  // 最新值标签（贴在最后一个点上方）
  const lastRow = rows[rows.length - 1]
  const text = isBP && lastRow.metricValue2 != null
    ? `${lastRow.metricValue}/${lastRow.metricValue2} ${unit}`
    : `${lastRow.metricValue} ${unit}`
  const bw = text.length * 7.2 + 16
  const lp = p1[p1.length - 1]
  const last = { text, bw, bx: Math.min(lp.x - bw / 2, W.value - padR - bw), by: Math.max(lp.y - 34, 2) }

  trend.value = { points, path1, path2, area, ticks, xLabels, last }
  animateChart()
}

/** 入场动画：面积淡入 + 线条描边生长 + 数据点依次弹入 */
async function animateChart() {
  await nextTick()
  if (reduced || !lineRef.value) return
  const ease = 'power2.out'
  const l1 = lineRef.value, len1 = l1.getTotalLength()
  gsap.fromTo(l1, { strokeDasharray: len1, strokeDashoffset: len1 },
    { strokeDashoffset: 0, duration: 0.9, ease })
  if (line2Ref.value) {
    const l2 = line2Ref.value, len2 = l2.getTotalLength()
    gsap.fromTo(l2, { strokeDasharray: `${len2} ${len2}`, strokeDashoffset: len2 },
      { strokeDashoffset: 0, duration: 0.9, delay: 0.15, ease,
        onComplete: () => gsap.set(l2, { strokeDasharray: '6 5' }) })
  }
  if (areaRef.value) gsap.fromTo(areaRef.value, { opacity: 0 }, { opacity: 1, duration: 0.7, delay: 0.35, ease })
  const dots = chartBox.value?.querySelectorAll('.dot-pt') || []
  gsap.fromTo(dots, { attr: { r: 0 } }, { attr: { r: 4 }, duration: 0.35, stagger: 0.05, delay: 0.25, ease })
  if (lastRef.value) gsap.fromTo(lastRef.value, { opacity: 0, y: 6 }, { opacity: 1, y: 0, duration: 0.4, delay: 0.8, ease })
}

/** 图表宽度跟随容器（避免 SVG 拉伸变形） */
let onResize = null
function measure() {
  if (chartBox.value) W.value = Math.max(chartBox.value.clientWidth, 320)
}

function fillUnit() {
  form.value.unit = types.find(t => t.value === form.value.metricType)?.unit || ''
  if (form.value.metricType !== 'BLOOD_PRESSURE') form.value.metricValue2 = null
}
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) {
  form.value = row ? { ...row } : { metricType: 'BLOOD_PRESSURE', unit: 'mmHg' }
  dialog.value = true
}
async function save() {
  const res = form.value.id ? await updateMetric(form.value) : await createMetric(form.value)
  const msg = res.message || '保存成功'
  msg.includes('注意') ? ElMessage.warning(msg) : ElMessage.success(msg)
  dialog.value = false
  load()
}
async function remove(id) {
  await ElMessageBox.confirm('确认删除?', '提示'); await deleteMetric(id); ElMessage.success('已删除'); load()
}

onMounted(() => {
  measure()
  onResize = () => { measure(); buildTrend() }
  window.addEventListener('resize', onResize, { passive: true })
  load()
})
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<style scoped>
.metric-page { display: flex; flex-direction: column; gap: 16px; }
.trend-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.trend-head h3 { margin: 0; font-size: 18px; color: var(--hda-ink); }
.chart-box { margin-top: 12px; }
.trend-svg { width: 100%; height: 260px; display: block; }
.trend-svg .ax { font-size: 11px; fill: #93a2b5; }
.dot-pt { cursor: pointer; transition: stroke-width 0.2s cubic-bezier(0.22, 1, 0.36, 1); }
.dot-pt:hover { stroke-width: 4; }
/* 异常点：红色光晕 + 外圈脉冲扩散 */
.dot-abn { filter: drop-shadow(0 0 4px rgba(229, 101, 75, 0.55)); }
.dot-halo {
  pointer-events: none;
  transform-box: fill-box; transform-origin: center;
  animation: haloPulse 1.8s cubic-bezier(0.22, 1, 0.36, 1) infinite;
}
@keyframes haloPulse {
  0% { transform: scale(0.5); opacity: 0.9; }
  70% { transform: scale(1.9); opacity: 0; }
  100% { transform: scale(1.9); opacity: 0; }
}
.last-txt { font-size: 12px; fill: #fff; font-weight: 600; }
.trend-note { margin: 8px 0 0; font-size: 14px; color: var(--hda-ink-soft); }
.auto-hint { margin-left: 12px; font-size: 14px; color: var(--hda-ink-soft); }
@media (prefers-reduced-motion: reduce) {
  .metric-page * { animation: none !important; transition: none !important; }
}
</style>
