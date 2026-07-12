<template>
  <el-card>
    <div class="bar">
      <el-button type="primary" @click="generate" :loading="loading">分析近7日体征并生成预警</el-button>
      <span class="hint">基于血压/血糖/心率/体温阈值规则自动分析</span>
    </div>
    <el-table :data="list" style="margin-top:15px" border :row-class-name="rowClass">
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="级别" width="90"><template #default="{ row }">
        <el-tag :type="{ LOW:'info', MEDIUM:'warning', HIGH:'danger' }[row.level]">
          {{ { LOW:'提示', MEDIUM:'中度', HIGH:'高危' }[row.level] || row.level }}
        </el-tag>
      </template></el-table-column>
      <el-table-column prop="alertType" label="类型" width="120" />
      <el-table-column prop="content" label="内容/建议" />
      <el-table-column label="状态" width="90"><template #default="{ row }">
        <el-tag :type="row.readFlag ? 'info' : 'danger'" :effect="row.readFlag ? 'light' : 'dark'">
          {{ row.readFlag ? '已读' : '未读' }}
        </el-tag>
      </template></el-table-column>
      <el-table-column label="操作" width="110"><template #default="{ row }">
        <el-button v-if="!row.readFlag" link type="primary" @click="read(row)">标记已读</el-button>
        <span v-else class="done">—</span>
      </template></el-table-column>
    </el-table>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAlerts, generateAlert, markAlertRead } from '@/api'
const list = ref([]), total = ref(0), loading = ref(false)
const query = ref({ pageNum: 1, pageSize: 10 })
async function load() { const res = await pageAlerts(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
async function generate() {
  loading.value = true
  try {
    const res = await generateAlert()
    ElMessage.success(res.message || '分析完成')
    load()
  } finally { loading.value = false }
}
async function read(row) {
  await markAlertRead(row.id)
  row.readFlag = 1
  ElMessage.success('已标记为已读')
}
function rowClass({ row }) { return row.readFlag ? '' : 'unread-row' }
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; gap: 14px; }
.hint { font-size: 14px; color: var(--hda-ink-soft); }
.done { color: #b7c2d0; }
:deep(.unread-row) { font-weight: 600; }
:deep(.unread-row td) { background: #FFF9F5 !important; }
</style>
