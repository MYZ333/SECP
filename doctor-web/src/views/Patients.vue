<template>
  <div class="patients-page">
    <div class="page-heading">
      <div>
        <h1>患者列表</h1>
        <p>查看平台患者的基础信息与健康档案。</p>
      </div>
      <div class="search-box">
        <el-input v-model="query.keyword" clearable placeholder="搜索患者昵称或用户名" @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div><b>全部患者</b><span>共 {{ total }} 位</span></div>
        <el-button text :loading="loading" @click="load"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="list" row-class-name="patient-row" @row-dblclick="openDetail">
        <el-table-column label="患者" min-width="220">
          <template #default="{ row }">
            <div class="patient-cell">
              <el-avatar :size="42" :src="resolveServerUrl(row.avatar)">{{ initial(row) }}</el-avatar>
              <div><b>{{ row.nickname || row.username }}</b><span>{{ row.username }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="性别" width="110">
          <template #default="{ row }"><span class="text-cell">{{ genderText(row.gender) }}</span></template>
        </el-table-column>
        <el-table-column label="生日" min-width="150">
          <template #default="{ row }"><span class="text-cell">{{ row.birthday || '未填写' }}</span></template>
        </el-table-column>
        <el-table-column label="账户状态" width="130">
          <template #default><el-tag type="success" effect="light">正常</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="right" class-name="action-column">
          <template #default="{ row }">
            <el-button class="detail-action" type="primary" plain @click="openDetail(row)">查看详情<el-icon><ArrowRight /></el-icon></el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="query.keyword ? '没有找到匹配的患者' : '暂无患者数据'">
            <el-button v-if="query.keyword" @click="clearSearch">清除搜索</el-button>
          </el-empty>
        </template>
      </el-table>
      <div v-if="total > query.pageSize" class="pager">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="changePage" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue'
import { pagePatients } from '@/api'
import { resolveServerUrl } from '@/config/server'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(true)
const query = ref({ pageNum: 1, pageSize: 10, keyword: '' })

function genderText(value) { return value === 1 ? '男' : value === 2 ? '女' : '保密' }
function initial(row) { return (row.nickname || row.username || '患').charAt(0) }
function openDetail(row) { router.push(`/patients/${row.id}`) }
async function load() {
  loading.value = true
  try {
    const res = await pagePatients(query.value)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { query.value.pageNum = 1; load() }
function clearSearch() { query.value.keyword = ''; search() }
function changePage(page) { query.value.pageNum = page; load() }
onMounted(load)
</script>

<style scoped>
.search-box { width: min(460px, 100%); display: flex; gap: 10px; }
.search-box .el-input { flex: 1; }
.table-card :deep(.el-card__body) { padding: 0 24px 20px; }
.table-toolbar { min-height: 66px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--hda-line); }
.table-toolbar b { color: var(--hda-ink); font-size: 18px; }
.table-toolbar span { margin-left: 10px; color: var(--hda-ink-soft); font-size: 14px; }
.patient-cell { display: flex; align-items: center; gap: 12px; }
.patient-cell .el-avatar { flex: 0 0 auto; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 700; }
.patient-cell div { min-width: 0; display: flex; flex-direction: column; }
.patient-cell b { overflow: hidden; color: var(--hda-ink); font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.patient-cell span { color: #8B9AAF; font-size: 13px; }
.text-cell { color: #435A75; }
.table-card :deep(.patient-row) { cursor: pointer; }
.table-card :deep(.patient-row td) { transition: background-color .2s; }
.table-card :deep(.action-column .cell) { overflow: visible; white-space: nowrap; }
.detail-action { min-width: 122px; justify-content: center; }
.detail-action :deep(.el-icon) { margin-left: 4px; }
.table-card :deep(.el-empty) { padding: 54px 0; }
.pager { display: flex; justify-content: center; padding-top: 20px; }
@media (max-width: 700px) { .search-box { width: 100%; } .table-card :deep(.el-card__body) { padding: 0 14px 16px; } }
</style>
