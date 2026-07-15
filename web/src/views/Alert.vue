<template>
  <div class="alert-page">
    <section class="alert-head" aria-labelledby="alert-title">
      <div class="alert-head-copy">
        <div class="section-label"><el-icon><CircleCheckFilled /></el-icon><span>自动监测已开启</span></div>
        <h1 id="alert-title">健康预警</h1>
        <p>每次保存血压、血糖、心率或体温后，系统都会自动检测并合并同类未解决问题，无需手动生成。</p>
      </div>
      <div class="monitor-visual" aria-hidden="true">
        <span class="visual-line line-one"></span>
        <span class="visual-line line-two"></span>
        <span class="visual-pulse"><el-icon><DataLine /></el-icon></span>
      </div>
      <div class="monitor-state"><span aria-hidden="true"></span><strong>正在监听体征变化</strong></div>
    </section>

    <section class="summary-band" aria-label="预警处理概况">
      <div class="summary-intro">
        <span class="summary-icon"><el-icon><BellFilled /></el-icon></span>
        <div><strong>处理概况</strong><span>{{ summary.latestCreateTime ? `最近触发于 ${fmtTime(summary.latestCreateTime)}` : '目前没有新的预警' }}</span></div>
      </div>
      <dl class="summary-values">
        <div class="active"><dt>待处理</dt><dd>{{ summary.active }}</dd><span>需要关注</span></div>
        <div class="danger"><dt>高危待处理</dt><dd>{{ summary.highActive }}</dd><span>优先处理</span></div>
        <div class="warning"><dt>处理中</dt><dd>{{ summary.inProgress }}</dd><span>持续跟进</span></div>
        <div class="resolved"><dt>已解决</dt><dd>{{ summary.resolved }}</dd><span>处理完成</span></div>
      </dl>
    </section>

    <section class="records-panel" aria-labelledby="records-title">
      <div class="records-head">
        <div><h2 id="records-title">预警事项</h2><p>选择咨询渠道开始处理，确认问题处理完成后再标记为已解决。</p></div>
        <div class="filters" aria-label="筛选预警事项">
          <el-select v-model="filters.status" placeholder="全部状态" clearable @change="applyFilters">
            <el-option label="待处理与处理中" value="ACTIVE" />
            <el-option label="新预警" value="OPEN" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-select v-model="filters.level" placeholder="全部级别" clearable @change="applyFilters">
            <el-option label="高危" value="HIGH" /><el-option label="中度" value="MEDIUM" /><el-option label="提示" value="LOW" />
          </el-select>
          <el-button v-if="hasActiveFilters" class="reset-filter" text @click="resetFilters">重置筛选</el-button>
        </div>
      </div>

      <el-table v-loading="listLoading" :data="list" class="desktop-table" :row-class-name="rowClass">
        <el-table-column label="级别" width="90"><template #default="{ row }"><el-tag :type="levelTag(row.level)" :effect="isActive(row) ? 'dark' : 'light'">{{ levelName(row.level) }}</el-tag></template></el-table-column>
        <el-table-column prop="alertType" label="类型" width="116" />
        <el-table-column label="预警内容" min-width="370"><template #default="{ row }"><AlertContent :content="row.content" /><span v-if="row.triggerCount > 1" class="trigger-count">本轮已更新 {{ row.triggerCount }} 次</span></template></el-table-column>
        <el-table-column label="最近触发" width="158"><template #default="{ row }">{{ fmtTime(row.lastTriggerTime || row.createTime) }}</template></el-table-column>
        <el-table-column label="处理状态" width="104"><template #default="{ row }"><span :class="['status-pill', statusOf(row).toLowerCase()]">{{ statusName(statusOf(row)) }}</span></template></el-table-column>
        <el-table-column label="处理" width="296"><template #default="{ row }"><div v-if="isActive(row)" class="row-actions"><el-button link type="primary" @click="consultHealth(row)">问健康助手</el-button><el-button link type="primary" @click="consultDoctor(row)">咨询医生</el-button><el-button link type="success" @click="resolve(row)">标记解决</el-button><el-button link type="info" @click="ignore(row)">误录</el-button></div><span v-else class="finished-note">{{ row.resolutionNote || '处理已结束' }}</span></template></el-table-column>
        <template #empty><div class="empty-state"><span class="empty-icon"><el-icon><CircleCheckFilled /></el-icon></span><strong>{{ hasActiveFilters ? '没有符合筛选条件的预警' : '目前没有待处理预警' }}</strong><span>{{ hasActiveFilters ? '调整筛选条件，查看其他预警记录。' : '系统正在持续监测，新异常会自动出现在这里。' }}</span><el-button v-if="hasActiveFilters" type="primary" plain @click="resetFilters">查看全部待处理预警</el-button><el-button v-else type="primary" plain @click="$router.push('/health/metric')">记录新的体征</el-button></div></template>
      </el-table>

      <div v-loading="listLoading" class="mobile-list">
        <article v-for="row in list" :key="row.id" :class="['alert-card', { active: isActive(row) }]">
          <div class="card-top"><div><el-tag :type="levelTag(row.level)" :effect="isActive(row) ? 'dark' : 'light'">{{ levelName(row.level) }}</el-tag><strong>{{ row.alertType }}</strong></div><span :class="['status-pill', statusOf(row).toLowerCase()]">{{ statusName(statusOf(row)) }}</span></div>
          <AlertContent :content="row.content" />
          <div class="card-meta"><time>{{ fmtTime(row.lastTriggerTime || row.createTime) }}</time><span v-if="row.triggerCount > 1">已更新 {{ row.triggerCount }} 次</span></div>
          <div v-if="isActive(row)" class="mobile-actions"><el-button type="primary" plain @click="consultHealth(row)">问健康助手</el-button><el-button type="primary" plain @click="consultDoctor(row)">咨询医生</el-button><el-button type="success" plain @click="resolve(row)">标记解决</el-button><el-button @click="ignore(row)">误录</el-button></div>
        </article>
        <div v-if="!list.length && !listLoading" class="empty-state"><span class="empty-icon"><el-icon><CircleCheckFilled /></el-icon></span><strong>{{ hasActiveFilters ? '没有符合筛选条件的预警' : '目前没有待处理预警' }}</strong><span>{{ hasActiveFilters ? '调整筛选条件，查看其他预警记录。' : '系统正在持续监测，新异常会自动出现在这里。' }}</span><el-button v-if="hasActiveFilters" type="primary" plain @click="resetFilters">查看全部待处理预警</el-button><el-button v-else type="primary" plain @click="$router.push('/health/metric')">记录新的体征</el-button></div>
      </div>
      <el-pagination v-if="total > query.pageSize" v-model:current-page="query.pageNum" class="pager" layout="prev, pager, next" :total="total" :page-size="query.pageSize" @current-change="loadList" />
    </section>

    <section class="history-check">
      <span class="history-icon"><el-icon><Refresh /></el-icon></span>
      <div><strong>历史数据补偿检测</strong><span>仅用于规则升级或旧体征未生成预警时，日常记录无需操作。</span></div>
      <el-button :loading="previewLoading" :icon="Refresh" @click="historyOpen ? historyOpen = false : loadPreview()">{{ historyOpen ? '收起检测结果' : '重新检测历史体征' }}</el-button>
    </section>

    <section v-if="historyOpen" class="preview-panel" aria-live="polite">
      <el-skeleton v-if="previewLoading" :rows="3" animated />
      <template v-else-if="preview">
        <div class="preview-title"><div><span class="state-mark" :class="{ warning: preview.abnormalCount }"><el-icon><WarningFilled v-if="preview.abnormalCount" /><CircleCheckFilled v-else /></el-icon></span><div><h2>历史检测结果</h2><p>{{ preview.conclusion }}</p></div></div></div>
        <div class="preview-facts"><span>范围：{{ fmtTime(preview.windowStart) }} 至 {{ fmtTime(preview.windowEnd) }}</span><span>可分析 {{ preview.analyzableMetricCount }} 条</span><span>异常 {{ preview.abnormalCount }} 条</span></div>
        <div v-if="preview.abnormalTypes.length" class="risk-list"><article v-for="item in preview.abnormalTypes" :key="item.metricType" class="risk-row"><el-tag :type="levelTag(item.level)" effect="dark">{{ levelName(item.level) }}</el-tag><div><strong>{{ item.typeName }}异常，共 {{ item.abnormalCount }} 次</strong><span>{{ item.latestMessage }}</span></div></article></div>
        <div class="preview-actions"><p>{{ preview.abnormalTypes.length ? '系统会合并同类未解决预警，不会重复创建。' : '没有需要补偿的异常预警。' }}</p><el-button v-if="preview.abnormalTypes.length" type="primary" :loading="generating" @click="generate">同步历史异常</el-button><el-button v-else @click="$router.push('/health/metric')">查看体征记录</el-button></div>
      </template>
    </section>

    <aside class="medical-note"><span class="note-icon"><el-icon><FirstAidKit /></el-icon></span><div><strong>重要提示</strong><p>预警由固定阈值规则生成，不构成诊断。若出现持续胸痛、严重呼吸困难、意识异常等紧急情况，请立即联系急救服务。</p></div></aside>

    <el-dialog v-model="resultVisible" title="历史检测完成" width="min(520px, 92vw)" append-to-body>
      <div v-if="generationResult" class="result-dialog"><span class="result-icon"><el-icon><CircleCheckFilled /></el-icon></span><h3>{{ generationResult.conclusion }}</h3><p>分析 {{ generationResult.analyzableMetricCount }} 条体征，发现 {{ generationResult.abnormalCount }} 条异常。</p><dl><div><dt>新增</dt><dd>{{ generationResult.generatedCount }}</dd></div><div><dt>更新</dt><dd>{{ generationResult.updatedCount }}</dd></div><div><dt>已去重</dt><dd>{{ generationResult.duplicateCount }}</dd></div></dl></div>
      <template #footer><el-button type="primary" @click="resultVisible = false">查看预警事项</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellFilled, CircleCheckFilled, DataLine, FirstAidKit, Refresh, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateAlert, getAlertPreview, getAlertSummary, ignoreAlert, pageAlerts, resolveAlert } from '@/api'

const router = useRouter()
const list = ref([]), total = ref(0), listLoading = ref(false)
const historyOpen = ref(false), previewLoading = ref(false), generating = ref(false), preview = ref(null)
const generationResult = ref(null), resultVisible = ref(false)
const summary = reactive({ total: 0, active: 0, highActive: 0, inProgress: 0, resolved: 0, latestCreateTime: null })
const filters = reactive({ status: 'ACTIVE', level: null })
const query = reactive({ pageNum: 1, pageSize: 10 })
const hasActiveFilters = computed(() => filters.status !== 'ACTIVE' || Boolean(filters.level))

const levelName = level => ({ LOW: '提示', MEDIUM: '中度', HIGH: '高危' }[level] || level)
const levelTag = level => ({ LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }[level] || 'info')
const statusName = status => ({ OPEN: '待处理', ACKNOWLEDGED: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已解决', IGNORED: '已忽略' }[status] || status)
const statusOf = row => row.status || (row.readFlag ? 'ACKNOWLEDGED' : 'OPEN')
const isActive = row => ['OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS'].includes(statusOf(row))
const fmtTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '暂无记录'

function contentParts(content) {
  const text = String(content || ''), latestAt = text.indexOf('最近一次：'), peakAt = text.indexOf('七日内最高风险记录：')
  if (latestAt < 0) return { summary: text, detail: '', peak: '' }
  return { summary: text.slice(0, latestAt).replace(/[。\s]+$/, ''), detail: text.slice(latestAt, peakAt < 0 ? undefined : peakAt).replace(/[。\s]+$/, ''), peak: peakAt < 0 ? '' : text.slice(peakAt).replace(/[。\s]+$/, '') }
}

const AlertContent = defineComponent({
  props: { content: { type: String, default: '' } },
  setup(props) { return () => { const parts = contentParts(props.content); return h('div', { class: 'alert-content' }, [h('strong', parts.summary), parts.detail && h('span', parts.detail), parts.peak && h('span', { class: 'peak-risk' }, parts.peak)]) } }
})

async function loadList() {
  listLoading.value = true
  try { const params = { ...query, ...(filters.status ? { status: filters.status } : {}), ...(filters.level ? { level: filters.level } : {}) }; const res = await pageAlerts(params); list.value = res.data.records || []; total.value = res.data.total || 0 } finally { listLoading.value = false }
}
async function loadSummary() { const res = await getAlertSummary(); Object.assign(summary, res.data || {}) }
async function loadPreview() { historyOpen.value = true; previewLoading.value = true; try { preview.value = (await getAlertPreview()).data } finally { previewLoading.value = false } }
async function generate() { generating.value = true; try { const res = await generateAlert(); generationResult.value = res.data; resultVisible.value = true; await Promise.all([loadList(), loadSummary(), loadPreview()]) } finally { generating.value = false } }
function applyFilters() { query.pageNum = 1; loadList() }
function resetFilters() { filters.status = 'ACTIVE'; filters.level = null; applyFilters() }
function buildQuestion(row) { return `我收到一条${levelName(row.level)}健康预警：${row.alertType}。${row.content} 请结合这条预警告诉我现在应该怎么处理、需要观察哪些危险信号，以及什么情况下应尽快线下就医。` }
async function consultHealth(row) { await router.push({ path: '/consult', query: { alertId: row.id, q: buildQuestion(row) } }) }
async function consultDoctor(row) { await router.push({ path: '/doctor', query: { alertId: row.id, q: buildQuestion(row) } }) }
async function resolve(row) {
  try { const { value } = await ElMessageBox.prompt('确认异常已经得到处理后再结束预警。可以补充复测结果或医生建议（选填）。', '标记为已解决', { confirmButtonText: '确认已解决', cancelButtonText: '取消', inputPlaceholder: '例如：复测恢复正常，已遵医嘱处理', inputValidator: value => !value || value.length <= 500 || '说明不能超过500字' }); await resolveAlert(row.id, { note: value }); ElMessage.success('预警已解决'); await Promise.all([loadList(), loadSummary()]) } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
async function ignore(row) {
  try { const { value } = await ElMessageBox.prompt('仅在数值误录、设备异常或记录无效时忽略。', '忽略预警', { confirmButtonText: '确认忽略', cancelButtonText: '取消', inputPlaceholder: '请填写忽略原因', inputValidator: value => String(value || '').trim() ? (value.length <= 500 || '原因不能超过500字') : '请填写忽略原因' }); await ignoreAlert(row.id, { note: value }); ElMessage.success('预警已忽略'); await Promise.all([loadList(), loadSummary()]) } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
function rowClass({ row }) { return isActive(row) ? `active-row ${String(row.level || '').toLowerCase()}-risk` : 'finished-row' }
onMounted(() => Promise.all([loadList(), loadSummary()]))
</script>

<style scoped>
.alert-page {
  --alert-radius: 12px;
  --alert-shadow: 0 6px 8px rgba(37, 86, 139, .08);
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1280px;
  margin: 0 auto;
  color: var(--hda-ink);
}
.alert-page :deep(.el-button:not(.is-link):not(.is-text)) { border-radius: 8px; }
.alert-page :deep(.el-tag) { border-radius: 6px; }
.alert-page :deep(.el-button:focus-visible),
.alert-page :deep(.el-select__wrapper.is-focused) {
  outline: 3px solid var(--el-color-primary-light-7);
  outline-offset: 2px;
}

.alert-head {
  position: relative;
  isolation: isolate;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px auto;
  align-items: center;
  gap: 32px;
  min-height: 236px;
  padding: 38px 42px;
  overflow: hidden;
  border: 1px solid rgba(111, 153, 194, .2);
  border-radius: var(--alert-radius);
  background: rgba(255, 255, 255, .92);
  box-shadow: var(--alert-shadow);
}
.alert-head::before {
  content: "";
  position: absolute;
  inset: 0 0 0 48%;
  z-index: -1;
  opacity: .65;
  background-image:
    linear-gradient(rgba(80, 139, 196, .07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(80, 139, 196, .07) 1px, transparent 1px);
  background-size: 42px 42px;
  -webkit-mask-image: linear-gradient(90deg, transparent, #000 34%);
  mask-image: linear-gradient(90deg, transparent, #000 34%);
}
.alert-head-copy { position: relative; z-index: 1; min-width: 0; }
.section-label {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 0 0 8px;
  color: #24795f;
  font-size: 14px;
  font-weight: 700;
}
.section-label .el-icon { font-size: 17px; }
.alert-head h1 { margin: 0; font-size: 42px; letter-spacing: -.025em; }
.alert-head-copy > p:last-child {
  max-width: 62ch;
  margin: 13px 0 0;
  color: var(--hda-ink-soft);
  line-height: 1.7;
  text-wrap: pretty;
}
.monitor-visual {
  position: relative;
  width: 230px;
  height: 90px;
  overflow: hidden;
  color: var(--el-color-primary);
  contain: paint;
}
.monitor-visual::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 -72px;
  width: 72px;
  opacity: 0;
  background: linear-gradient(90deg, transparent, rgba(46, 111, 224, .12), transparent);
  transform: translate3d(0, 0, 0);
  animation: monitorScan 3.2s cubic-bezier(.22, 1, .36, 1) infinite;
}
.visual-line { position: absolute; left: 0; right: 0; height: 1px; overflow: visible; background: #dbe8f7; }
.visual-line::after {
  content: "";
  position: absolute;
  top: -3px;
  left: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  opacity: 0;
  background: #5d91e8;
  box-shadow: 0 0 0 4px rgba(46, 111, 224, .1);
  transform: translate3d(0, 0, 0);
  animation: signalTravel 3.2s cubic-bezier(.22, 1, .36, 1) infinite;
}
.line-one { top: 28px; }
.line-two { top: 62px; left: 36px; }
.line-two::after { animation-delay: .46s; }
.visual-pulse {
  position: absolute;
  left: 62px;
  top: 9px;
  display: grid;
  place-items: center;
  width: 104px;
  height: 72px;
  font-size: 68px;
  color: var(--el-color-primary);
  filter: drop-shadow(0 5px 8px rgba(46, 111, 224, .18));
  transform-origin: 50% 54%;
  animation: chartHeartbeat 3.2s cubic-bezier(.22, 1, .36, 1) infinite;
}
.visual-pulse::after {
  content: "";
  position: absolute;
  inset: 7px 14px;
  z-index: -1;
  border: 1px solid rgba(46, 111, 224, .18);
  border-radius: 50%;
  opacity: 0;
  transform: scale(.8);
  animation: chartEcho 3.2s cubic-bezier(.22, 1, .36, 1) infinite;
}
.monitor-state {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
  min-height: 46px;
  padding: 0 16px;
  border-radius: 999px;
  background: #edf8f4;
  color: #24795f;
  font-size: 14px;
  white-space: nowrap;
}
.monitor-state span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #2fa37c;
  box-shadow: 0 0 0 5px rgba(47, 163, 124, .14);
  animation: monitorPulse 2.2s cubic-bezier(.22, 1, .36, 1) infinite;
}

.summary-band {
  display: grid;
  grid-template-columns: 278px minmax(0, 1fr);
  min-height: 132px;
  overflow: hidden;
  border-radius: var(--alert-radius);
  background: rgba(255, 255, 255, .9);
  box-shadow: var(--alert-shadow);
}
.summary-intro {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 26px 28px;
  color: #fff;
  background: linear-gradient(135deg, #356fd2, #2859b8);
}
.summary-icon {
  display: grid;
  place-items: center;
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .16);
  color: #fff;
  font-size: 23px;
}
.summary-intro div { display: flex; min-width: 0; flex-direction: column; }
.summary-intro strong { font-size: 17px; }
.summary-intro span { margin-top: 4px; color: rgba(255, 255, 255, .78); font-size: 13px; line-height: 1.5; }
.summary-values { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin: 0; }
.summary-values div {
  position: relative;
  display: grid;
  align-content: center;
  min-width: 0;
  padding: 22px 24px;
}
.summary-values div + div::before {
  content: "";
  position: absolute;
  top: 25px;
  bottom: 25px;
  left: 0;
  width: 1px;
  background: var(--hda-line);
}
.summary-values dt { color: var(--hda-ink-soft); font-size: 14px; }
.summary-values dd { margin: 2px 0 1px; font: 800 30px/1.15 var(--hda-font-display); color: var(--hda-ink); }
.summary-values span { color: #8a9bb0; font-size: 12px; }
.summary-values .active dd { color: var(--el-color-primary); }
.summary-values .danger dd { color: #d9543d; }
.summary-values .warning dd { color: #c87724; }
.summary-values .resolved dd { color: #288567; }

.records-panel,
.preview-panel {
  padding: 30px 32px;
  border-radius: var(--alert-radius);
  background: rgba(255, 255, 255, .94);
  box-shadow: var(--alert-shadow);
}
.records-head,
.preview-title,
.preview-title > div { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.records-head { align-items: flex-end; margin-bottom: 22px; }
.records-head h2,
.preview-title h2 { margin: 0; font-size: 23px; }
.records-head p,
.preview-title p { margin: 6px 0 0; color: var(--hda-ink-soft); font-size: 14px; }
.filters { display: flex; align-items: center; gap: 10px; }
.filters :deep(.el-select) { width: 172px; }
.filters :deep(.el-select__wrapper) { min-height: 42px; border-radius: 8px; }
.reset-filter { white-space: nowrap; }
.desktop-table { --el-table-header-bg-color: #f2f6fc; border-radius: 8px; }
.desktop-table :deep(th.el-table__cell) { height: 52px; }
.desktop-table :deep(td.el-table__cell) { padding: 14px 0; }
.desktop-table :deep(.active-row.high-risk td) { background: #fff7f5; }
.desktop-table :deep(.finished-row td) { color: #738296; background: #fafbfd; }
.alert-content { display: flex; flex-direction: column; gap: 4px; line-height: 1.58; }
.alert-content strong { font-weight: 650; }
.alert-content span { color: var(--hda-ink-soft); font-size: 14px; }
.alert-content .peak-risk { color: #b24430; }
.trigger-count { display: inline-block; margin-top: 7px; color: #486581; font-size: 13px; }
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f0f3f6;
  color: #66788c;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}
.status-pill.open,
.status-pill.acknowledged { background: #fff0ec; color: #b84430; }
.status-pill.in_progress { background: #edf3ff; color: #275fbd; }
.status-pill.resolved { background: #edf8f4; color: #24795f; }
.row-actions { display: flex; flex-wrap: wrap; gap: 2px 4px; }
.finished-note { color: #7b8999; font-size: 13px; }
.pager { justify-content: flex-end; margin-top: 20px; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  min-height: 250px;
  justify-content: center;
  padding: 34px 16px;
  color: var(--hda-ink-soft);
  text-align: center;
}
.empty-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  margin-bottom: 5px;
  border-radius: 50%;
  background: #edf8f4;
  color: #2b9472;
  font-size: 30px;
}
.empty-state strong { color: var(--hda-ink); font-size: 17px; }
.empty-state > span:not(.empty-icon) { max-width: 42ch; font-size: 14px; }
.empty-state .el-button { margin-top: 10px; }
.mobile-list { display: none; }

.history-check {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 22px 26px;
  border: 1px solid #dce8f6;
  border-radius: var(--alert-radius);
  background: rgba(247, 250, 253, .92);
  color: var(--hda-ink);
}
.history-icon,
.note-icon,
.state-mark {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #eaf1fc;
  color: var(--el-color-primary);
  font-size: 22px;
}
.history-check > div { display: flex; min-width: 0; flex-direction: column; }
.history-check span:not(.history-icon) { color: var(--hda-ink-soft); font-size: 14px; }
.preview-panel { border-top: 3px solid var(--el-color-primary); }
.state-mark { background: #edf8f4; color: #228765; font-size: 24px; }
.state-mark.warning { background: #fff0ec; color: #c94d35; }
.preview-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 22px;
  margin-top: 20px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #f3f7fb;
  color: #3f5470;
  font-size: 14px;
}
.risk-list { margin-top: 18px; }
.risk-row { display: flex; align-items: flex-start; gap: 14px; padding: 16px 0; border-bottom: 1px solid var(--hda-line); }
.risk-row div { display: flex; flex-direction: column; }
.risk-row span { color: var(--hda-ink-soft); font-size: 14px; }
.preview-actions { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--hda-line); }
.preview-actions p { margin: 0; color: var(--hda-ink-soft); }
.medical-note {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  border-radius: var(--alert-radius);
  background: #eaf1f8;
  color: #29435f;
}
.note-icon { width: 42px; height: 42px; background: rgba(255, 255, 255, .66); color: #476b92; font-size: 21px; }
.medical-note strong { display: block; margin-top: 1px; }
.medical-note p { margin: 3px 0 0; font-size: 14px; line-height: 1.65; }

.result-dialog { text-align: center; }
.result-icon { color: var(--el-color-success); font-size: 48px; }
.result-dialog h3 { margin: 8px 0 4px; font-size: 21px; }
.result-dialog > p { margin: 0; color: var(--hda-ink-soft); }
.result-dialog dl { display: grid; grid-template-columns: repeat(3, 1fr); margin: 22px 0 0; border-radius: 8px; background: #f3f7fb; }
.result-dialog dl div { padding: 14px; }
.result-dialog dt { color: var(--hda-ink-soft); font-size: 13px; }
.result-dialog dd { margin: 3px 0 0; font: 700 22px var(--hda-font-display); }

@keyframes monitorPulse {
  0%, 100% { transform: scale(.85); box-shadow: 0 0 0 4px rgba(47, 163, 124, .12); }
  50% { transform: scale(1); box-shadow: 0 0 0 7px rgba(47, 163, 124, .06); }
}
@keyframes monitorScan {
  0%, 12% { opacity: 0; transform: translate3d(0, 0, 0); }
  22% { opacity: 1; }
  68% { opacity: .8; }
  80%, 100% { opacity: 0; transform: translate3d(330px, 0, 0); }
}
@keyframes signalTravel {
  0%, 14% { opacity: 0; transform: translate3d(0, 0, 0) scale(.75); }
  22% { opacity: 1; }
  68% { opacity: 1; }
  82%, 100% { opacity: 0; transform: translate3d(220px, 0, 0) scale(1); }
}
@keyframes chartHeartbeat {
  0%, 18%, 100% { transform: translate3d(0, 0, 0) scale(1); filter: drop-shadow(0 5px 8px rgba(46, 111, 224, .18)); }
  28% { transform: translate3d(0, -2px, 0) scale(1.045); filter: drop-shadow(0 7px 10px rgba(46, 111, 224, .26)); }
  38% { transform: translate3d(0, 0, 0) scale(1); }
  48% { transform: translate3d(0, -1px, 0) scale(1.025); }
  58% { transform: translate3d(0, 0, 0) scale(1); }
}
@keyframes chartEcho {
  0%, 22% { opacity: 0; transform: scale(.82); }
  32% { opacity: .65; }
  56%, 100% { opacity: 0; transform: scale(1.22); }
}

@media (max-width: 1080px) {
  .alert-head { grid-template-columns: minmax(0, 1fr) auto; }
  .monitor-visual { display: none; }
  .summary-band { grid-template-columns: 240px minmax(0, 1fr); }
  .summary-values div { padding-inline: 18px; }
  .records-head { align-items: stretch; flex-direction: column; }
  .filters { align-self: flex-start; }
}
@media (max-width: 820px) {
  .alert-page { gap: 16px; }
  .alert-head { grid-template-columns: 1fr; min-height: 0; padding: 30px 26px; }
  .monitor-state { justify-self: start; }
  .summary-band { grid-template-columns: 1fr; }
  .summary-intro { padding-block: 20px; }
  .summary-values { min-height: 116px; }
  .history-check { grid-template-columns: auto minmax(0, 1fr); }
  .history-check > .el-button { grid-column: 2; justify-self: start; }
}
@media (max-width: 720px) {
  .alert-head { padding: 26px 20px; }
  .alert-head h1 { font-size: 34px; }
  .summary-values { grid-template-columns: repeat(2, 1fr); }
  .summary-values div:nth-child(3)::before { display: none; }
  .summary-values div:nth-child(n + 3) { border-top: 1px solid var(--hda-line); }
  .records-panel,
  .preview-panel { padding: 24px 18px; }
  .filters { width: 100%; flex-wrap: wrap; }
  .filters :deep(.el-select) { flex: 1 1 150px; width: auto; }
  .desktop-table { display: none; }
  .mobile-list { display: flex; flex-direction: column; gap: 10px; min-height: 100px; }
  .alert-card { padding: 17px; border: 1px solid #e3ebf5; border-radius: 10px; background: #f8fafc; }
  .alert-card.active { border-color: #efb4aa; background: #fff7f5; }
  .card-top,
  .card-top > div,
  .card-meta { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
  .card-top > div { justify-content: flex-start; }
  .alert-card .alert-content { margin: 14px 0; }
  .card-meta { color: var(--hda-ink-soft); font-size: 13px; }
  .mobile-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 14px; }
  .mobile-actions .el-button { min-width: 0; margin: 0; }
  .history-check { grid-template-columns: auto minmax(0, 1fr); padding: 20px; }
  .history-check > .el-button { grid-column: 1 / -1; width: 100%; }
  .preview-actions { align-items: stretch; flex-direction: column; }
  .medical-note { padding: 18px; }
}
@media (max-width: 460px) {
  .summary-values { grid-template-columns: 1fr 1fr; }
  .summary-values div { padding: 18px 15px; }
  .summary-values dd { font-size: 27px; }
  .monitor-state { width: 100%; justify-content: center; }
  .mobile-actions { grid-template-columns: 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .alert-page *,
  .alert-page *::before,
  .alert-page *::after { animation: none !important; transition: none !important; }
}
</style>
