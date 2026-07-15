<template>
  <div class="knowledge-page">
    <section class="page-heading">
      <div class="heading-main">
        <span class="heading-icon" aria-hidden="true"><el-icon><Collection /></el-icon></span>
        <div>
          <p class="section-label">知识库管理</p>
          <h1>{{ activeAgent === 'APPLICATION' ? '应用使用助手知识库' : '健康助手知识库' }}</h1>
          <p class="heading-copy">资料按助手独立存储和检索，发布后才会参与智能问答。</p>
        </div>
      </div>
      <div class="heading-actions">
        <el-radio-group v-model="activeAgent" class="agent-switch" @change="switchAgent">
          <el-radio-button label="HEALTH">健康助手</el-radio-button>
          <el-radio-button label="APPLICATION">应用使用助手</el-radio-button>
        </el-radio-group>
        <div class="primary-actions">
          <el-button :icon="Download" :loading="seeding" @click="importSeeds">导入语料</el-button>
          <el-button type="primary" :icon="UploadFilled" @click="openUpload">上传资料</el-button>
        </div>
      </div>
    </section>

    <section class="overview-strip" aria-label="知识库概览">
      <div class="overview-item total"><span class="overview-icon"><el-icon><Document /></el-icon></span><div><span>全部资料</span><strong>{{ page.total }}</strong></div></div>
      <div class="overview-item published"><span class="overview-icon"><el-icon><CircleCheckFilled /></el-icon></span><div><span>当前页已发布</span><strong>{{ counts.PUBLISHED || 0 }}</strong></div></div>
      <div class="overview-item draft"><span class="overview-icon"><el-icon><Clock /></el-icon></span><div><span>当前页待审核</span><strong>{{ counts.DRAFT || 0 }}</strong></div></div>
      <div class="overview-item failed"><span class="overview-icon"><el-icon><WarningFilled /></el-icon></span><div><span>当前页索引异常</span><strong>{{ counts.FAILED || 0 }}</strong></div></div>
    </section>

    <section class="library-panel">
      <div class="panel-heading">
        <div><h2>资料列表</h2><p>管理资料内容、发布状态和向量索引</p></div>
        <span class="result-count">{{ page.total }} 份资料</span>
      </div>

      <div class="toolbar-row">
        <div class="toolbar">
          <el-input v-model="query.keyword" :prefix-icon="Search" placeholder="搜索标题、机构或分类" clearable @keyup.enter="load" />
          <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
            <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" plain :icon="Search" @click="load">查询</el-button>
        </div>
      </div>

      <div v-if="selectedRows.length" class="batch-bar" aria-live="polite">
        <span>已选择 <strong>{{ selectedRows.length }}</strong> 份资料</span>
        <div>
          <el-button :disabled="!publishableIds.length" :loading="batchAction === 'publish'" @click="batchPublish">批量发布<span v-if="publishableIds.length">（{{ publishableIds.length }}）</span></el-button>
          <el-button type="danger" plain :disabled="!publishedIds.length" :loading="batchAction === 'inactive'" @click="batchInactive">批量停用<span v-if="publishedIds.length">（{{ publishedIds.length }}）</span></el-button>
        </div>
      </div>

      <el-table ref="tableRef" v-loading="loading" :data="page.records" row-key="id" class="knowledge-table" @selection-change="selectedRows = $event">
        <el-table-column type="selection" width="48" reserve-selection />
        <el-table-column label="资料" min-width="300">
          <template #default="{ row }">
            <div class="doc-cell"><span class="doc-mark">{{ fileMark(row.fileName) }}</span><div><strong>{{ row.title }}</strong><small>{{ row.category }} · {{ row.fileName }}</small></div></div>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="220">
          <template #default="{ row }"><a :href="row.sourceUrl" target="_blank" rel="noopener">{{ row.sourceOrg }}</a><small class="date">{{ row.publishedDate || '未标注发布日期' }}</small></template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="切片" width="78" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><span class="status" :class="row.status.toLowerCase()">{{ statusLabel(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="更新时间" width="160"><template #default="{ row }">{{ formatTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="right" header-align="right">
          <template #default="{ row }">
            <el-button link @click="preview(row)">预览</el-button>
            <el-button v-if="['DRAFT','FAILED','INACTIVE'].includes(row.status)" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="danger" @click="inactive(row)">停用</el-button>
            <el-button v-if="deletableStatuses.includes(row.status)" link type="danger" @click="deleteDocument(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <span class="empty-icon" aria-hidden="true"><el-icon><Files /></el-icon></span>
            <strong>暂无知识资料</strong>
            <p>上传官方资料，或导入项目内置语料开始构建知识库。</p>
            <el-button type="primary" :icon="UploadFilled" @click="openUpload">上传资料</el-button>
          </div>
        </template>
      </el-table>
      <div class="pagination-row"><el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" layout="total, prev, pager, next" :total="page.total" @current-change="load" /></div>
    </section>

    <el-dialog v-model="uploadVisible" :title="activeAgent === 'APPLICATION' ? '上传应用使用资料' : '上传健康知识资料'" width="620px" class="knowledge-dialog">
      <el-form label-position="top">
        <el-form-item label="文档"><el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.html,.htm,.md,.txt" :on-change="onFileChange" :on-remove="() => selectedFile = null"><div class="upload-copy"><strong>拖入 PDF / HTML / Markdown / TXT</strong><span>单文件不超过 5MB，上传后先进入草稿</span></div></el-upload></el-form-item>
        <div class="form-grid">
          <el-form-item label="资料标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="分类"><el-input v-model="form.category" :placeholder="activeAgent === 'APPLICATION' ? '如：平台功能说明' : '如：基础健康素养'" /></el-form-item>
          <el-form-item label="发布机构"><el-input v-model="form.sourceOrg" :placeholder="activeAgent === 'APPLICATION' ? '如：智慧医养平台产品组' : '如：国家卫生健康委员会'" /></el-form-item>
          <el-form-item label="发布日期"><el-date-picker v-model="form.publishedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        </div>
        <el-form-item label="官方来源链接"><el-input v-model="form.sourceUrl" placeholder="https://www.nhc.gov.cn/..." /></el-form-item>
        <el-form-item label="版本"><el-input v-model="form.versionNo" placeholder="可选，如：2024版" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="uploadVisible=false">取消</el-button><el-button type="primary" :loading="uploading" @click="submitUpload">解析为草稿</el-button></template>
    </el-dialog>

    <el-drawer v-model="previewVisible" size="58%" direction="rtl" class="chunk-drawer">
      <template #header><div class="drawer-heading"><p>切片预览</p><h2>{{ activeDocument?.title }}</h2></div></template>
      <div class="source-line"><a :href="activeDocument?.sourceUrl" target="_blank">{{ activeDocument?.sourceOrg }}</a><span>{{ chunks.length }} 个切片</span></div>
      <article v-for="chunk in chunks" :key="chunk.id" class="chunk-card"><div><span>#{{ chunk.chunkNo }}</span><strong>{{ chunk.sectionTitle || '正文' }}</strong></div><p>{{ chunk.content }}</p></article>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheckFilled, Clock, Collection, Document, Download, Files, Search, UploadFilled, WarningFilled } from '@element-plus/icons-vue'
import { adminDeleteKnowledge, adminImportApplicationKnowledgeSeeds, adminImportKnowledgeSeeds, adminInactiveKnowledge, adminInactiveKnowledgeBatch, adminKnowledgeChunks, adminPageApplicationKnowledge, adminPageKnowledge, adminPublishKnowledge, adminPublishKnowledgeBatch, adminUploadApplicationKnowledge, adminUploadKnowledge } from '@/api'

const loading = ref(false), uploading = ref(false), seeding = ref(false)
const batchAction = ref(''), tableRef = ref(null), selectedRows = ref([])
const uploadVisible = ref(false), previewVisible = ref(false)
const selectedFile = ref(null), activeDocument = ref(null), chunks = ref([])
const page = reactive({ total: 0, records: [] })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const activeAgent = ref('HEALTH')
const form = reactive({ title: '', sourceOrg: '中华人民共和国国家卫生健康委员会', sourceUrl: '', publishedDate: '', versionNo: '', category: '健康科普' })
const statuses = [{ value: 'DRAFT', label: '待审核' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'FAILED', label: '索引异常' }, { value: 'INACTIVE', label: '已停用' }]
const deletableStatuses = ['DRAFT', 'FAILED', 'INACTIVE']
const counts = computed(() => page.records.reduce((all, item) => ({ ...all, [item.status]: (all[item.status] || 0) + 1 }), {}))
const publishableIds = computed(() => selectedRows.value.filter(row => ['DRAFT', 'FAILED', 'INACTIVE'].includes(row.status)).map(row => row.id))
const publishedIds = computed(() => selectedRows.value.filter(row => row.status === 'PUBLISHED').map(row => row.id))

async function load() { loading.value = true; try { const api = activeAgent.value === 'APPLICATION' ? adminPageApplicationKnowledge : adminPageKnowledge; const res = await api(query); Object.assign(page, res.data); tableRef.value?.clearSelection(); selectedRows.value = [] } finally { loading.value = false } }
async function switchAgent() {
  query.pageNum = 1
  if (activeAgent.value === 'APPLICATION') Object.assign(form, { sourceOrg: '智慧医养平台产品组', sourceUrl: 'https://platform.example.com/help', category: '平台功能说明' })
  else Object.assign(form, { sourceOrg: '中华人民共和国国家卫生健康委员会', sourceUrl: '', category: '健康科普' })
  Object.assign(page, { records: [], total: 0 })
  await load()
}
function openUpload() { uploadVisible.value = true }
function onFileChange(file) { selectedFile.value = file.raw; if (!form.title) form.title = file.name.replace(/\.[^.]+$/, '') }
async function submitUpload() {
  if (!selectedFile.value || !form.title || !form.sourceOrg || !form.sourceUrl || !form.category) return ElMessage.warning('请完整填写文档和来源信息')
  uploading.value = true
  try { const data = new FormData(); data.append('file', selectedFile.value); Object.entries(form).forEach(([key, value]) => { if (value) data.append(key, value) }); const upload = activeAgent.value === 'APPLICATION' ? adminUploadApplicationKnowledge : adminUploadKnowledge; await upload(data); ElMessage.success('解析完成，请预览切片后发布'); uploadVisible.value = false; await load() }
  finally { uploading.value = false }
}
async function importSeeds() { seeding.value = true; try { const seed = activeAgent.value === 'APPLICATION' ? adminImportApplicationKnowledgeSeeds : adminImportKnowledgeSeeds; const res = await seed(); ElMessage.success(`已导入 ${res.data} 份首批语料`); await load() } finally { seeding.value = false } }
async function preview(row) { activeDocument.value = row; chunks.value = (await adminKnowledgeChunks(row.id)).data || []; previewVisible.value = true }
async function publish(row) { await ElMessageBox.confirm('发布后将调用百炼 Embedding 并写入 Chroma，确认继续？', '发布资料'); await adminPublishKnowledge(row.id); ElMessage.success('资料已发布'); await load() }
async function inactive(row) { await ElMessageBox.confirm('停用后将删除该资料的全部向量；再次发布时会生成新的向量。', '停用资料'); await adminInactiveKnowledge(row.id); ElMessage.success('资料已停用，向量已删除'); await load() }
async function deleteDocument(row) { await ElMessageBox.confirm(`确认彻底删除“${row.title}”？文档及其全部切片将被删除，且无法恢复。`, '删除文档', { confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }); await adminDeleteKnowledge(row.id); ElMessage.success('文档已删除'); await load() }
async function runBatch(action, ids, confirmText, successText) {
  await ElMessageBox.confirm(confirmText, '确认批量操作', { confirmButtonText: '确认执行', cancelButtonText: '取消' })
  batchAction.value = action
  try {
    const request = action === 'publish' ? adminPublishKnowledgeBatch : adminInactiveKnowledgeBatch
    const result = (await request(ids)).data
    if (result.failed) {
      const firstFailure = result.failures?.[0]
      ElMessage.warning(`${successText}：成功 ${result.succeeded} 份，失败 ${result.failed} 份${firstFailure ? `；资料 #${firstFailure.documentId}：${firstFailure.reason}` : ''}`)
    }
    else ElMessage.success(`${successText}：共 ${result.succeeded} 份`)
    await load()
  } finally { batchAction.value = '' }
}
function batchPublish() { return runBatch('publish', publishableIds.value, `将发布 ${publishableIds.value.length} 份资料，并为所有切片生成向量。`, '批量发布完成') }
function batchInactive() { return runBatch('inactive', publishedIds.value, `将停用 ${publishedIds.value.length} 份已发布资料，并从 Chroma 删除对应向量键。`, '批量停用完成') }
function statusLabel(status) { return ({ DRAFT: '待审核', INDEXING: '索引中', PUBLISHED: '已发布', FAILED: '索引异常', INACTIVE: '已停用' })[status] || status }
function fileMark(name='') { const ext = name.split('.').pop()?.toUpperCase(); return ['PDF','HTML','MD','TXT'].includes(ext) ? ext : 'DOC' }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
onMounted(load)
</script>

<style scoped>
.knowledge-page {
  --page-blue: #2e6fe0;
  --page-blue-soft: #edf4ff;
  --page-orange: #ff6a00;
  --page-ink: #16283e;
  --page-muted: #5d7189;
  --page-line: #e1e9f4;
  --page-surface: rgba(255, 255, 255, .92);
  max-width: 1380px;
  margin: 0 auto;
  color: var(--page-ink);
}
.page-heading {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
  min-height: 154px;
  padding: 30px 34px;
  overflow: hidden;
  border-radius: 14px;
  background:
    radial-gradient(circle at 92% -60%, rgba(46,111,224,.16), transparent 42%),
    var(--page-surface);
  box-shadow: 0 6px 16px rgba(38,83,150,.08);
}
.heading-main { display: flex; align-items: center; gap: 18px; min-width: 0; }
.heading-icon { width: 54px; height: 54px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 14px; color: #fff; background: var(--page-blue); box-shadow: 0 6px 12px rgba(46,111,224,.22); }
.heading-icon .el-icon { font-size: 27px; }
.section-label { margin: 0 0 5px; color: var(--page-blue); font-size: 14px; font-weight: 700; }
.page-heading h1 { margin: 0; color: var(--page-ink); font-size: 30px; font-weight: 800; line-height: 1.2; letter-spacing: -.02em; text-wrap: balance; }
.heading-copy { max-width: 58ch; margin: 9px 0 0; color: var(--page-muted); font-size: 15px; line-height: 1.6; text-wrap: pretty; }
.heading-actions { display: flex; align-items: center; gap: 14px; flex: 0 0 auto; }
.primary-actions { display: flex; gap: 10px; }
.agent-switch { padding: 4px; border-radius: 12px; background: #edf2f8; }
.agent-switch :deep(.el-radio-button__inner) { min-height: 40px; display: inline-flex; align-items: center; padding: 0 17px; border: 0; border-radius: 9px; color: #52657e; background: transparent; box-shadow: none; font-size: 14px; font-weight: 600; }
.agent-switch :deep(.el-radio-button:first-child .el-radio-button__inner), .agent-switch :deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 9px; }
.agent-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { color: var(--page-blue); background: #fff; box-shadow: 0 2px 6px rgba(38,83,150,.1); }
.overview-strip { display: grid; grid-template-columns: repeat(4, 1fr); margin: 18px 0; border-radius: 14px; background: var(--page-surface); box-shadow: 0 4px 12px rgba(38,83,150,.06); }
.overview-item { position: relative; display: flex; align-items: center; gap: 13px; min-width: 0; padding: 19px 24px; }
.overview-item + .overview-item::before { content: ''; position: absolute; left: 0; top: 22%; width: 1px; height: 56%; background: var(--page-line); }
.overview-icon { width: 38px; height: 38px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 11px; color: #60738c; background: #f1f5fa; }
.overview-icon .el-icon { font-size: 19px; }
.overview-item > div { display: flex; flex-direction: column-reverse; gap: 2px; }
.overview-item span:not(.overview-icon) { color: var(--page-muted); font-size: 13px; }
.overview-item strong { color: var(--page-ink); font-size: 25px; font-weight: 800; line-height: 1; font-variant-numeric: tabular-nums; }
.overview-item.published .overview-icon { color: #1f8a62; background: #eaf7f1; }
.overview-item.draft .overview-icon { color: #bd7a13; background: #fff5e2; }
.overview-item.failed .overview-icon { color: #c75349; background: #fff0ee; }
.overview-item.failed strong { color: #b9443b; }
.library-panel { overflow: hidden; border-radius: 14px; background: var(--page-surface); box-shadow: 0 6px 16px rgba(38,83,150,.08); }
.panel-heading { display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 23px 26px 18px; }
.panel-heading h2 { margin: 0; color: var(--page-ink); font-size: 20px; font-weight: 800; }
.panel-heading p { margin: 5px 0 0; color: var(--page-muted); font-size: 13px; }
.result-count { flex: 0 0 auto; padding: 6px 10px; border-radius: 999px; color: var(--page-blue); background: var(--page-blue-soft); font-size: 12px; font-weight: 700; }
.toolbar-row { padding: 0 26px 18px; }
.toolbar { display: grid; grid-template-columns: minmax(280px, 430px) 176px 92px; gap: 10px; }
.toolbar :deep(.el-input__wrapper), .toolbar :deep(.el-select__wrapper) { min-height: 42px; border-radius: 10px; background: #f8fafd; box-shadow: 0 0 0 1px var(--page-line) inset; }
.toolbar :deep(.el-input__wrapper:hover), .toolbar :deep(.el-select__wrapper:hover) { box-shadow: 0 0 0 1px #9bbaf0 inset; }
.toolbar :deep(.el-input__wrapper.is-focus), .toolbar :deep(.el-select__wrapper.is-focused) { box-shadow: 0 0 0 2px rgba(46,111,224,.28) inset; }
.toolbar :deep(.el-input__inner::placeholder), .toolbar :deep(.el-select__placeholder) { color: #657891; }
.batch-bar { display: flex; min-height: 54px; margin: 0 26px 16px; padding: 8px 10px 8px 16px; justify-content: space-between; align-items: center; gap: 16px; border-radius: 11px; color: #35506e; background: var(--page-blue-soft); animation: batch-in .2s cubic-bezier(.22,1,.36,1); }
.batch-bar > span { font-size: 13px; }.batch-bar > span strong { color: var(--page-blue); font-size: 16px; }
.batch-bar > div { display: flex; flex-wrap: wrap; gap: 8px; }
@keyframes batch-in { from { opacity: 0; transform: translateY(-5px); } }
.knowledge-table { width: 100%; }
.knowledge-table :deep(.el-table__inner-wrapper::before) { display: none; }
.knowledge-table :deep(th.el-table__cell) { height: 50px; padding: 0; color: #52657e; background: #f4f7fb; font-size: 13px; font-weight: 700; }
.knowledge-table :deep(td.el-table__cell) { height: 78px; padding: 0; border-bottom-color: #edf1f6; }
.knowledge-table :deep(.el-table__row) { transition: background-color .18s ease; }
.knowledge-table :deep(.el-table__row:hover > td.el-table__cell) { background: #f8fbff; }
.knowledge-table :deep(.el-checkbox__inner) { width: 17px; height: 17px; border-radius: 5px; }
.knowledge-table :deep(.el-button.is-link) { min-height: 36px; padding: 0 7px; font-size: 14px; font-weight: 600; }
.doc-cell { display: flex; align-items: center; gap: 13px; min-width: 0; }
.doc-cell > div { min-width: 0; }
.doc-mark { width: 42px; height: 46px; display: grid; place-items: center; flex: 0 0 auto; border-radius: 10px; color: var(--page-blue); background: var(--page-blue-soft); font-size: 10px; font-weight: 800; letter-spacing: .04em; }
.doc-cell strong, .doc-cell small, .date { display: block; }
.doc-cell strong { overflow: hidden; color: var(--page-ink); font-size: 14px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.doc-cell small, .date { margin-top: 5px; overflow: hidden; color: #667a92; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.knowledge-table a { color: #315f9f; font-size: 14px; text-decoration: none; }
.knowledge-table a:hover { color: var(--page-blue); text-decoration: underline; text-underline-offset: 3px; }
.status { display: inline-flex; align-items: center; min-height: 28px; padding: 0 10px; border-radius: 999px; color: #5a6c82; background: #edf1f5; font-size: 12px; font-weight: 700; white-space: nowrap; }
.status.published { color: #207454; background: #e7f5ee; }
.status.failed { color: #b9443b; background: #fdecea; }
.status.draft { color: #9c6611; background: #fff2d9; }
.status.inactive { color: #5d6a7b; background: #edf0f3; }
.status.indexing { color: var(--page-blue); background: var(--page-blue-soft); }
.pagination-row { display: flex; justify-content: flex-end; padding: 17px 24px 20px; }
.pagination-row :deep(.el-pagination) { margin: 0; }
.empty-state { display: flex; min-height: 280px; flex-direction: column; align-items: center; justify-content: center; padding: 34px; }
.empty-icon { width: 58px; height: 58px; display: grid; place-items: center; margin-bottom: 14px; border-radius: 14px; color: var(--page-blue); background: var(--page-blue-soft); }
.empty-icon .el-icon { font-size: 27px; }
.empty-state strong { color: var(--page-ink); font-size: 16px; }
.empty-state p { margin: 7px 0 17px; color: var(--page-muted); font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
.upload-copy { padding: 20px; }.upload-copy strong, .upload-copy span { display: block; }.upload-copy span { margin-top: 7px; color: var(--page-muted); font-size: 12px; }
.drawer-heading p { margin: 0 0 5px; color: var(--page-blue); font-size: 13px; font-weight: 700; }.drawer-heading h2 { margin: 0; color: var(--page-ink); font-size: 20px; }
.source-line { display: flex; justify-content: space-between; margin-bottom: 18px; color: var(--page-muted); font-size: 13px; }.source-line a { color: var(--page-blue); }
.chunk-card { margin-bottom: 12px; padding: 18px 20px; border-radius: 12px; background: #f7f9fc; }
.chunk-card > div { display: flex; gap: 10px; color: #315f9f; font-size: 12px; }.chunk-card p { margin: 11px 0 0; color: #344a64; font-size: 14px; line-height: 1.8; white-space: pre-wrap; }
.knowledge-page :deep(.el-button) { min-height: 40px; border-radius: 10px; font-weight: 600; transition: transform .16s ease, background-color .16s ease, border-color .16s ease, color .16s ease; }
.knowledge-page :deep(.el-button:active:not(.is-disabled)) { transform: translateY(1px); }
:global(.knowledge-dialog.el-dialog), :global(.chunk-drawer.el-drawer) { border-radius: 14px; }
:global(.knowledge-dialog .el-dialog__header), :global(.chunk-drawer .el-drawer__header) { margin: 0; padding: 22px 24px 16px; border-bottom: 1px solid #e1e9f4; }
:global(.knowledge-dialog .el-dialog__body) { padding: 22px 24px 10px; }
:global(.knowledge-dialog .el-dialog__footer) { padding: 12px 24px 22px; }
@media (max-width: 1180px) {
  .page-heading { align-items: flex-start; flex-direction: column; }
  .heading-actions { width: 100%; justify-content: space-between; }
}
@media (max-width: 820px) {
  .page-heading { padding: 24px 20px; }
  .heading-main { align-items: flex-start; }.heading-icon { width: 46px; height: 46px; }
  .page-heading h1 { font-size: 25px; }
  .heading-actions { align-items: stretch; flex-direction: column; }
  .primary-actions > .el-button { flex: 1; }
  .agent-switch { display: flex; }.agent-switch :deep(.el-radio-button) { flex: 1; }.agent-switch :deep(.el-radio-button__inner) { width: 100%; justify-content: center; }
  .overview-strip { grid-template-columns: 1fr 1fr; }
  .overview-item:nth-child(3)::before { display: none; }.overview-item:nth-child(n+3) { border-top: 1px solid var(--page-line); }
  .panel-heading, .toolbar-row { padding-inline: 18px; }
  .toolbar { grid-template-columns: 1fr; }
  .batch-bar { align-items: flex-start; flex-direction: column; margin-inline: 18px; }.batch-bar > div { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
}
@media (max-width: 520px) {
  .knowledge-page { margin-inline: -12px; }
  .heading-main { gap: 12px; }.heading-copy { font-size: 14px; }
  .overview-item { padding: 16px 14px; }.overview-icon { display: none; }
  .overview-item strong { font-size: 22px; }
  .primary-actions { flex-direction: column; }
  .panel-heading { align-items: flex-start; }.result-count { margin-top: 2px; }
  .pagination-row { justify-content: center; padding-inline: 10px; }
}
@media (prefers-reduced-motion: reduce) {
  .batch-bar { animation: none; }
  .knowledge-page *, .knowledge-page *::before, .knowledge-page *::after { scroll-behavior: auto !important; transition-duration: .001ms !important; animation-duration: .001ms !important; }
}
</style>
