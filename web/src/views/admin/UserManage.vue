<template>
  <el-card>
    <el-input v-model="query.keyword" placeholder="用户名/昵称" style="width:220px" @keyup.enter="load" clearable>
      <template #append><el-button @click="load">搜索</el-button></template>
    </el-input>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="role" label="角色" />
      <el-table-column prop="points" label="积分" />
      <el-table-column label="状态"><template #default="{ row }">
        <el-tag :type="row.status ? 'danger' : 'success'">{{ row.status ? '禁用' : '正常' }}</el-tag>
      </template></el-table-column>
      <el-table-column label="操作" width="180"><template #default="{ row }">
        <el-button link type="primary" @click="toggle(row)">{{ row.status ? '启用' : '禁用' }}</el-button>
        <el-button link type="warning" @click="adjust(row)">调整积分</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminPageUsers } from '@/api'
import request from '@/api/request'
const list = ref([]), total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, keyword: '' })
async function load() { const res = await adminPageUsers(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
async function toggle(row) {
  await request.put(`/admin/user/${row.id}/status/${row.status ? 0 : 1}`); ElMessage.success('操作成功'); load()
}
async function adjust(row) {
  const { value } = await ElMessageBox.prompt('输入积分变动值(正加负减)', '调整积分')
  await request.post(`/admin/user/${row.id}/points?change=${value}`); ElMessage.success('操作成功'); load()
}
onMounted(load)
</script>
