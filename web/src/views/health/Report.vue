<template>
  <el-card>
    <el-button type="primary" @click="openDialog()">新增健康报告</el-button>
    <el-button type="success" :loading="generating" @click="doGenerate">一键生成分析报告（近30天）</el-button>
    <span class="gen-hint">自动统计各项体征的均值/极值/异常次数</span>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="title" label="标题" />
      <el-table-column label="类型" width="110"><template #default="{ row }">
        <el-tag :type="{ PHYSICAL:'primary', AI:'success', OTHER:'info' }[row.reportType]">
          {{ { PHYSICAL:'体检报告', AI:'自动生成', OTHER:'其他' }[row.reportType] || row.reportType }}
        </el-tag>
      </template></el-table-column>
      <el-table-column prop="reportDate" label="报告日期" width="130" />
      <el-table-column prop="content" label="内容/结论" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="view(row)">查看</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 报告详情 -->
    <el-dialog v-model="viewDialog" :title="viewRow.title" width="620px" append-to-body>
      <pre class="report-content">{{ viewRow.content }}</pre>
    </el-dialog>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
    <el-dialog v-model="dialog" :title="form.id ? '编辑' : '新增'" width="500px" append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.reportType">
            <el-option label="体检报告" value="PHYSICAL" />
            <el-option label="AI生成" value="AI" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告日期"><el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="内容/结论"><el-input v-model="form.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="附件URL"><el-input v-model="form.fileUrl" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageReport, createReport, updateReport, deleteReport, generateReport } from '@/api'
const list = ref([]), total = ref(0), dialog = ref(false), form = ref({})
const generating = ref(false), viewDialog = ref(false), viewRow = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })

function view(row) { viewRow.value = row; viewDialog.value = true }
async function doGenerate() {
  generating.value = true
  try {
    const res = await generateReport()
    ElMessage.success(res.message || '报告已生成')
    load()
    view(res.data)
  } finally { generating.value = false }
}
async function load() { const res = await pageReport(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) { form.value = row ? { ...row } : { reportType: 'OTHER' }; dialog.value = true }
async function save() {
  form.value.id ? await updateReport(form.value) : await createReport(form.value)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(id) { await ElMessageBox.confirm('确认删除?', '提示'); await deleteReport(id); ElMessage.success('已删除'); load() }
onMounted(load)
</script>

<style scoped>
.gen-hint { margin-left: 12px; font-size: 14px; color: var(--hda-ink-soft); }
.report-content {
  margin: 0; padding: 16px; white-space: pre-wrap; word-break: break-all;
  font-family: inherit; font-size: 16px; line-height: 1.8; color: var(--hda-ink);
  background: var(--hda-bg-soft); max-height: 60vh; overflow-y: auto;
}
</style>
