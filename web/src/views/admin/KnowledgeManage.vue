<template>
  <div class="knowledge-page">
    <section class="knowledge-hero">
      <div>
        <p class="eyebrow">EVIDENCE LIBRARY</p>
        <h1>{{ activeAgent === 'APPLICATION' ? '应用使用助手知识库' : '健康助手知识库' }}</h1>
        <p class="hero-copy">两类资料物理分组、检索隔离。请切换到对应助手后，再上传、预览并发布资料。</p>
      </div>
      <div class="hero-actions">
        <el-radio-group v-model="activeAgent" class="agent-switch" @change="switchAgent">
          <el-radio-button label="HEALTH">健康助手</el-radio-button>
          <el-radio-button label="APPLICATION">应用使用助手</el-radio-button>
        </el-radio-group>
        <el-button class="seed-button" :loading="seeding" @click="importSeeds">导入首批语料</el-button>
        <el-button type="primary" @click="openUpload">上传官方资料</el-button>
      </div>
    </section>

    <section class="status-strip">
      <div><span>全部资料</span><strong>{{ page.total }}</strong></div>
      <div><span>已发布</span><strong class="published">{{ counts.PUBLISHED || 0 }}</strong></div>
      <div><span>待审核</span><strong>{{ counts.DRAFT || 0 }}</strong></div>
      <div><span>索引异常</span><strong class="failed">{{ counts.FAILED || 0 }}</strong></div>
    </section>

    <section class="library-panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索标题、机构或分类" clearable @keyup.enter="load" />
        <el-select v-model="query.status" placeholder="全部状态" clearable @change="load">
          <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="page.records" class="knowledge-table">
        <el-table-column label="资料" min-width="270">
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
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link @click="preview(row)">预览</el-button>
            <el-button v-if="['DRAFT','FAILED'].includes(row.status)" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link @click="reindex(row)">重建索引</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="danger" @click="inactive(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" layout="total, prev, pager, next" :total="page.total" @current-change="load" />
    </section>

    <el-dialog v-model="uploadVisible" title="上传权威健康资料" width="620px" class="knowledge-dialog">
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
      <template #header><div><p class="eyebrow">CHUNK PREVIEW</p><h2>{{ activeDocument?.title }}</h2></div></template>
      <div class="source-line"><a :href="activeDocument?.sourceUrl" target="_blank">{{ activeDocument?.sourceOrg }}</a><span>{{ chunks.length }} 个切片</span></div>
      <article v-for="chunk in chunks" :key="chunk.id" class="chunk-card"><div><span>#{{ chunk.chunkNo }}</span><strong>{{ chunk.sectionTitle || '正文' }}</strong></div><p>{{ chunk.content }}</p></article>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminImportApplicationKnowledgeSeeds, adminImportKnowledgeSeeds, adminInactiveKnowledge, adminKnowledgeChunks, adminPageApplicationKnowledge, adminPageKnowledge, adminPublishKnowledge, adminReindexKnowledge, adminUploadApplicationKnowledge, adminUploadKnowledge } from '@/api'

const loading = ref(false), uploading = ref(false), seeding = ref(false)
const uploadVisible = ref(false), previewVisible = ref(false)
const selectedFile = ref(null), activeDocument = ref(null), chunks = ref([])
const page = reactive({ total: 0, records: [] })
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const activeAgent = ref('HEALTH')
const form = reactive({ title: '', sourceOrg: '中华人民共和国国家卫生健康委员会', sourceUrl: '', publishedDate: '', versionNo: '', category: '健康科普' })
const statuses = [{ value: 'DRAFT', label: '待审核' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'FAILED', label: '索引异常' }, { value: 'INACTIVE', label: '已停用' }]
const counts = computed(() => page.records.reduce((all, item) => ({ ...all, [item.status]: (all[item.status] || 0) + 1 }), {}))

async function load() { loading.value = true; try { const api = activeAgent.value === 'APPLICATION' ? adminPageApplicationKnowledge : adminPageKnowledge; const res = await api(query); Object.assign(page, res.data) } finally { loading.value = false } }
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
async function reindex(row) { await ElMessageBox.confirm('确认删除旧向量并重建索引？', '重建索引'); await adminReindexKnowledge(row.id); ElMessage.success('索引已重建'); await load() }
async function inactive(row) { await ElMessageBox.confirm('停用后该资料将不再参与健康咨询检索。', '停用资料'); await adminInactiveKnowledge(row.id); ElMessage.success('资料已停用'); await load() }
function statusLabel(status) { return ({ DRAFT: '待审核', INDEXING: '索引中', PUBLISHED: '已发布', FAILED: '索引异常', INACTIVE: '已停用' })[status] || status }
function fileMark(name='') { const ext = name.split('.').pop()?.toUpperCase(); return ['PDF','HTML','MD','TXT'].includes(ext) ? ext : 'DOC' }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
onMounted(load)
</script>

<style scoped>
.knowledge-page { max-width: 1380px; margin: 0 auto; color: #24362f; }
.knowledge-hero { position: relative; overflow: hidden; display: flex; justify-content: space-between; align-items: flex-end; min-height: 190px; padding: 34px 38px; border-radius: 24px; color: #f4fbf7; background: #173d32; box-shadow: 0 24px 55px rgba(24,62,51,.16); }
.knowledge-hero::after { content: ''; position: absolute; width: 360px; height: 360px; right: -110px; top: -190px; border: 1px solid rgba(255,255,255,.14); border-radius: 50%; box-shadow: 0 0 0 48px rgba(255,255,255,.035), 0 0 0 96px rgba(255,255,255,.025); }
.knowledge-hero > * { position: relative; z-index: 1; }.eyebrow { margin: 0 0 10px; color: #91c9b5; font-size: 10px; font-weight: 800; letter-spacing: .18em; }.knowledge-hero h1 { margin: 0; font: 600 34px/1.1 var(--hda-font-display); }.hero-copy { max-width: 610px; margin: 15px 0 0; color: #bcd1c9; font-size: 14px; line-height: 1.7; }.hero-actions { display: flex; gap: 10px; }.seed-button { color: #e5f3ed; border-color: rgba(255,255,255,.28); background: rgba(255,255,255,.08); }
.status-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; margin: 18px 0; overflow: hidden; border: 1px solid #e5ebe8; border-radius: 16px; background: #e5ebe8; }.status-strip div { display: flex; justify-content: space-between; align-items: baseline; padding: 18px 22px; background: #fff; }.status-strip span { color: #83908b; font-size: 12px; }.status-strip strong { font-size: 24px; font-variant-numeric: tabular-nums; }.status-strip .published { color: #237356; }.status-strip .failed { color: #bb5047; }
.library-panel { padding: 22px; border: 1px solid #e5ebe8; border-radius: 18px; background: #fff; box-shadow: 0 14px 40px rgba(35,65,55,.06); }.toolbar { display: grid; grid-template-columns: minmax(260px, 1fr) 180px 70px; gap: 10px; max-width: 650px; margin-bottom: 18px; }.doc-cell { display: flex; align-items: center; gap: 12px; }.doc-mark { width: 44px; height: 48px; display: grid; place-items: center; flex-shrink: 0; border-radius: 10px 10px 10px 3px; color: #28664f; background: #e9f3ee; font-size: 9px; font-weight: 900; letter-spacing: .06em; }.doc-cell strong, .doc-cell small, .date { display: block; }.doc-cell strong { color: #2a3e36; font-size: 13px; }.doc-cell small, .date { margin-top: 5px; color: #929d99; font-size: 10px; }.knowledge-table a { color: #27745a; text-decoration: none; }.status { display: inline-flex; padding: 5px 9px; border-radius: 999px; color: #66756f; background: #eef1f0; font-size: 10px; font-weight: 700; }.status.published { color: #237356; background: #e7f4ed; }.status.failed { color: #b9473e; background: #fce9e7; }.status.draft { color: #90631e; background: #fff3dc; }.el-pagination { justify-content: flex-end; margin-top: 18px; }
.agent-switch { margin-right: 8px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }.upload-copy { padding: 20px; }.upload-copy strong, .upload-copy span { display: block; }.upload-copy span { margin-top: 7px; color: #98a29e; font-size: 11px; }.source-line { display: flex; justify-content: space-between; margin-bottom: 18px; color: #87928e; font-size: 12px; }.source-line a { color: #27745a; }.chunk-card { margin-bottom: 12px; padding: 18px 20px; border: 1px solid #e4ebe7; border-radius: 14px; background: #fbfcfb; }.chunk-card > div { display: flex; gap: 10px; color: #2f6552; font-size: 11px; }.chunk-card p { margin: 11px 0 0; color: #455951; font-size: 13px; line-height: 1.8; white-space: pre-wrap; }
@media (max-width: 820px) { .knowledge-hero { align-items: flex-start; flex-direction: column; gap: 24px; }.status-strip { grid-template-columns: 1fr 1fr; }.toolbar, .form-grid { grid-template-columns: 1fr; } }
</style>
