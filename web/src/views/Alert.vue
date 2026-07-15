<template>
  <div class="alert-page">
    <section class="alert-head" aria-labelledby="alert-title">
      <div>
        <p class="section-label">自动监测已开启</p>
        <h1 id="alert-title">健康预警</h1>
        <p>每次保存血压、血糖、心率或体温后，系统都会自动检测并合并同类未解决问题，无需手动生成。</p>
      </div>
      <div class="monitor-state"><span aria-hidden="true"></span><strong>正在监听体征变化</strong></div>
    </section>

    <section class="summary-band" aria-label="预警处理概况">
      <div class="summary-intro">
        <span class="summary-icon"><el-icon><BellFilled /></el-icon></span>
        <div><strong>处理概况</strong><span>{{ summary.latestCreateTime ? `最近触发于 ${fmtTime(summary.latestCreateTime)}` : '暂未触发预警' }}</span></div>
      </div>
      <dl class="summary-values">
        <div><dt>待处理</dt><dd>{{ summary.active }}</dd></div>
        <div class="danger"><dt>高危待处理</dt><dd>{{ summary.highActive }}</dd></div>
        <div class="warning"><dt>处理中</dt><dd>{{ summary.inProgress }}</dd></div>
        <div><dt>已解决</dt><dd>{{ summary.resolved }}</dd></div>
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
        </div>
      </div>

      <el-table v-loading="listLoading" :data="list" class="desktop-table" :row-class-name="rowClass">
        <el-table-column label="级别" width="90"><template #default="{ row }"><el-tag :type="levelTag(row.level)" :effect="isActive(row) ? 'dark' : 'light'">{{ levelName(row.level) }}</el-tag></template></el-table-column>
        <el-table-column prop="alertType" label="类型" width="116" />
        <el-table-column label="预警内容" min-width="370"><template #default="{ row }"><AlertContent :content="row.content" /><span v-if="row.triggerCount > 1" class="trigger-count">本轮已更新 {{ row.triggerCount }} 次</span></template></el-table-column>
        <el-table-column label="最近触发" width="158"><template #default="{ row }">{{ fmtTime(row.lastTriggerTime || row.createTime) }}</template></el-table-column>
        <el-table-column label="处理状态" width="104"><template #default="{ row }"><span :class="['status-pill', statusOf(row).toLowerCase()]">{{ statusName(statusOf(row)) }}</span></template></el-table-column>
        <el-table-column label="处理" width="296"><template #default="{ row }"><div v-if="isActive(row)" class="row-actions"><el-button link type="primary" @click="consultHealth(row)">问健康助手</el-button><el-button link type="primary" @click="consultDoctor(row)">咨询医生</el-button><el-button link type="success" @click="resolve(row)">标记解决</el-button><el-button link type="info" @click="ignore(row)">误录</el-button></div><span v-else class="finished-note">{{ row.resolutionNote || '处理已结束' }}</span></template></el-table-column>
        <template #empty><div class="empty-state"><strong>当前没有符合条件的预警</strong><span>系统会在保存异常体征后自动创建预警。</span></div></template>
      </el-table>

      <div v-loading="listLoading" class="mobile-list">
        <article v-for="row in list" :key="row.id" :class="['alert-card', { active: isActive(row) }]">
          <div class="card-top"><div><el-tag :type="levelTag(row.level)" :effect="isActive(row) ? 'dark' : 'light'">{{ levelName(row.level) }}</el-tag><strong>{{ row.alertType }}</strong></div><span :class="['status-pill', statusOf(row).toLowerCase()]">{{ statusName(statusOf(row)) }}</span></div>
          <AlertContent :content="row.content" />
          <div class="card-meta"><time>{{ fmtTime(row.lastTriggerTime || row.createTime) }}</time><span v-if="row.triggerCount > 1">已更新 {{ row.triggerCount }} 次</span></div>
          <div v-if="isActive(row)" class="mobile-actions"><el-button type="primary" plain @click="consultHealth(row)">问健康助手</el-button><el-button type="primary" plain @click="consultDoctor(row)">咨询医生</el-button><el-button type="success" plain @click="resolve(row)">标记解决</el-button><el-button @click="ignore(row)">误录</el-button></div>
        </article>
        <div v-if="!list.length && !listLoading" class="empty-state"><strong>当前没有符合条件的预警</strong><span>系统会在保存异常体征后自动创建预警。</span></div>
      </div>
      <el-pagination v-if="total > query.pageSize" v-model:current-page="query.pageNum" class="pager" layout="prev, pager, next" :total="total" :page-size="query.pageSize" @current-change="loadList" />
    </section>

    <section class="history-check">
      <div><strong>历史数据补偿检测</strong><span>仅用于规则升级或旧体征未生成预警时，日常记录无需操作。</span></div>
      <el-button :loading="previewLoading" @click="historyOpen ? historyOpen = false : loadPreview()">{{ historyOpen ? '收起' : '重新检测历史体征' }}</el-button>
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

    <aside class="medical-note"><strong>重要提示</strong><p>预警由固定阈值规则生成，不构成诊断。若出现持续胸痛、严重呼吸困难、意识异常等紧急情况，请立即联系急救服务。</p></aside>

    <el-dialog v-model="resultVisible" title="历史检测完成" width="min(520px, 92vw)" append-to-body>
      <div v-if="generationResult" class="result-dialog"><span class="result-icon"><el-icon><CircleCheckFilled /></el-icon></span><h3>{{ generationResult.conclusion }}</h3><p>分析 {{ generationResult.analyzableMetricCount }} 条体征，发现 {{ generationResult.abnormalCount }} 条异常。</p><dl><div><dt>新增</dt><dd>{{ generationResult.generatedCount }}</dd></div><div><dt>更新</dt><dd>{{ generationResult.updatedCount }}</dd></div><div><dt>已去重</dt><dd>{{ generationResult.duplicateCount }}</dd></div></dl></div>
      <template #footer><el-button type="primary" @click="resultVisible = false">查看预警事项</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellFilled, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateAlert, getAlertPreview, getAlertSummary, ignoreAlert, pageAlerts, resolveAlert } from '@/api'

const router = useRouter()
const list = ref([]), total = ref(0), listLoading = ref(false)
const historyOpen = ref(false), previewLoading = ref(false), generating = ref(false), preview = ref(null)
const generationResult = ref(null), resultVisible = ref(false)
const summary = reactive({ total: 0, active: 0, highActive: 0, inProgress: 0, resolved: 0, latestCreateTime: null })
const filters = reactive({ status: 'ACTIVE', level: null })
const query = reactive({ pageNum: 1, pageSize: 10 })

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
.alert-page { display: flex; flex-direction: column; gap: 18px; max-width: 1280px; margin: 0 auto; }
.alert-head { display: flex; align-items: center; justify-content: space-between; gap: 32px; padding: 30px 34px; background: #fff; box-shadow: 0 4px 8px rgba(28,63,120,.07); }
.section-label { margin: 0 0 4px; color: #24795f; font-size: 14px; font-weight: 700; }
.alert-head h1 { margin: 0; font-size: 30px; }.alert-head p:last-child { max-width: 68ch; margin: 10px 0 0; color: var(--hda-ink-soft); line-height: 1.65; }
.monitor-state { display: flex; align-items: center; gap: 10px; flex: none; padding: 12px 16px; background: #edf8f4; color: #24795f; }
.monitor-state span { width: 10px; height: 10px; border-radius: 50%; background: #2fa37c; box-shadow: 0 0 0 5px rgba(47,163,124,.14); }
.summary-band { display: flex; align-items: stretch; min-height: 108px; background: #183653; color: #fff; }
.summary-intro { display: flex; align-items: center; gap: 14px; min-width: 290px; padding: 22px 28px; background: #214767; }.summary-icon { display: grid; place-items: center; width: 44px; height: 44px; background: #fff; color: #214767; font-size: 22px; }.summary-intro div { display: flex; flex-direction: column; }.summary-intro span { color: #d5e3ef; font-size: 13px; }
.summary-values { display: grid; grid-template-columns: repeat(4,minmax(110px,1fr)); flex: 1; margin: 0; }.summary-values div { display: flex; flex-direction: column; justify-content: center; padding: 18px 26px; border-left: 1px solid rgba(255,255,255,.14); }.summary-values dt { color: #c6d8e7; font-size: 14px; }.summary-values dd { margin: 3px 0 0; font: 700 26px/1.2 var(--hda-font-display); }.summary-values .danger dd { color: #ff9b88; }.summary-values .warning dd { color: #ffc879; }
.records-panel,.preview-panel { padding: 26px 30px; background: #fff; box-shadow: 0 3px 8px rgba(28,63,120,.06); }.records-head,.preview-title,.preview-title>div { display: flex; align-items: center; justify-content: space-between; gap: 16px; }.records-head { align-items: flex-end; margin-bottom: 18px; }.records-head h2,.preview-title h2 { margin: 0; font-size: 21px; }.records-head p,.preview-title p { margin: 4px 0 0; color: var(--hda-ink-soft); font-size: 14px; }.filters { display: flex; gap: 10px; }.filters :deep(.el-select) { width: 166px; }
.alert-content { display: flex; flex-direction: column; gap: 4px; line-height: 1.55; }.alert-content span { color: var(--hda-ink-soft); font-size: 14px; }.alert-content .peak-risk { color: #b24430; }.trigger-count { display: inline-block; margin-top: 7px; color: #486581; font-size: 13px; }.desktop-table :deep(.active-row.high-risk td) { background: #fff5f2; }.desktop-table :deep(.finished-row td) { color: #738296; background: #fafbfd; }
.status-pill { display: inline-flex; padding: 4px 9px; background: #f0f3f6; color: #66788c; font-size: 13px; font-weight: 700; }.status-pill.open,.status-pill.acknowledged { background: #fff0ec; color: #b84430; }.status-pill.in_progress { background: #edf3ff; color: #275fbd; }.status-pill.resolved { background: #edf8f4; color: #24795f; }.row-actions { display: flex; flex-wrap: wrap; gap: 0 4px; }.finished-note { color: #7b8999; font-size: 13px; }.pager { justify-content: flex-end; margin-top: 18px; }
.history-check { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 18px 24px; background: #f5f8fb; color: var(--hda-ink); }.history-check div { display: flex; flex-direction: column; }.history-check span { color: var(--hda-ink-soft); font-size: 14px; }.preview-panel { box-shadow: inset 0 3px 0 #2e6fe0,0 3px 8px rgba(28,63,120,.06); }.state-mark { display: grid; place-items: center; width: 46px; height: 46px; background: #edf8f4; color: #228765; font-size: 24px; }.state-mark.warning { background: #fff0ec; color: #c94d35; }.preview-facts { display: flex; flex-wrap: wrap; gap: 8px 22px; margin-top: 20px; padding: 13px 16px; background: #f3f7fb; color: #3f5470; font-size: 14px; }.risk-list { margin-top: 18px; }.risk-row { display: flex; align-items: flex-start; gap: 14px; padding: 16px 0; border-bottom: 1px solid var(--hda-line); }.risk-row div { display: flex; flex-direction: column; }.risk-row span { color: var(--hda-ink-soft); font-size: 14px; }.preview-actions { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--hda-line); }.preview-actions p { margin: 0; color: var(--hda-ink-soft); }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 38px 16px; color: var(--hda-ink-soft); }.empty-state strong { color: var(--hda-ink); }.mobile-list { display: none; }.medical-note { display: flex; align-items: baseline; gap: 16px; padding: 17px 22px; background: #eaf1f8; color: #29435f; }.medical-note strong { flex: none; }.medical-note p { margin: 0; font-size: 14px; line-height: 1.65; }
.result-dialog { text-align: center; }.result-icon { color: var(--el-color-success); font-size: 48px; }.result-dialog h3 { margin: 8px 0 4px; font-size: 21px; }.result-dialog>p { margin: 0; color: var(--hda-ink-soft); }.result-dialog dl { display: grid; grid-template-columns: repeat(3,1fr); margin: 22px 0 0; background: #f3f7fb; }.result-dialog dl div { padding: 14px; }.result-dialog dt { color: var(--hda-ink-soft); font-size: 13px; }.result-dialog dd { margin: 3px 0 0; font: 700 22px var(--hda-font-display); }
@media(max-width:900px){.summary-band{flex-direction:column}.summary-intro{min-width:0}.summary-values div:first-child{border-left:0}}
@media(max-width:720px){.alert-page{gap:12px}.alert-head,.records-head,.preview-actions,.history-check{align-items:stretch;flex-direction:column}.alert-head{padding:24px 20px}.monitor-state{justify-content:center}.summary-values{grid-template-columns:repeat(2,1fr)}.summary-values div:nth-child(n+3){border-top:1px solid rgba(255,255,255,.14)}.records-panel,.preview-panel{padding:22px 18px}.filters{width:100%}.filters :deep(.el-select){flex:1;width:auto}.desktop-table{display:none}.mobile-list{display:flex;flex-direction:column;gap:10px;min-height:100px}.alert-card{padding:17px;background:#f7f9fc}.alert-card.active{background:#fff7f4;outline:2px solid #f0b0a3;outline-offset:-2px}.card-top,.card-top>div,.card-meta{display:flex;align-items:center;justify-content:space-between;gap:10px}.card-top>div{justify-content:flex-start}.alert-card .alert-content{margin:14px 0}.card-meta{color:var(--hda-ink-soft);font-size:13px}.mobile-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:14px}.mobile-actions .el-button{margin:0}.medical-note{align-items:flex-start;flex-direction:column;gap:4px}}
@media(prefers-reduced-motion:reduce){.alert-page *{animation:none!important;transition:none!important}}
</style>
