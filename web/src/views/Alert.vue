<template>
  <el-card>
    <el-button type="primary" @click="generate" :loading="loading">生成健康预警(AI分析)</el-button>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column label="级别" width="90"><template #default="{ row }">
        <el-tag :type="{ LOW:'info', MEDIUM:'warning', HIGH:'danger' }[row.level]">{{ row.level }}</el-tag>
      </template></el-table-column>
      <el-table-column prop="alertType" label="类型" width="120" />
      <el-table-column prop="content" label="内容/建议" />
      <el-table-column label="状态" width="90"><template #default="{ row }">
        <el-tag :type="row.readFlag ? 'info' : 'success'">{{ row.readFlag ? '已读' : '未读' }}</el-tag>
      </template></el-table-column>
    </el-table>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAlerts, generateAlert } from '@/api'
const list = ref([]), total = ref(0), loading = ref(false)
const query = ref({ pageNum: 1, pageSize: 10 })
async function load() { const res = await pageAlerts(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
async function generate() { loading.value = true; try { await generateAlert(); ElMessage.success('已生成'); load() } finally { loading.value = false } }
onMounted(load)
</script>
