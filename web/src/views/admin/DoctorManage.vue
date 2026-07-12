<template>
  <el-card>
    <el-button type="primary" @click="openDialog()">新增专家</el-button>
    <el-table :data="list" style="margin-top: 15px" border>
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="title" label="职称" />
      <el-table-column prop="hospital" label="医院" />
      <el-table-column prop="department" label="科室" />
      <el-table-column prop="speciality" label="擅长" show-overflow-tooltip />
      <el-table-column label="审核状态" width="110">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)">{{ auditText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <template v-if="row.auditStatus === 'PENDING'">
            <el-button link type="success" @click="audit(row, true)">通过</el-button>
            <el-button link type="warning" @click="audit(row, false)">拒绝</el-button>
          </template>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 15px"
      layout="prev, pager, next"
      :total="total"
      :page-size="query.pageSize"
      @current-change="onPage"
    />

    <el-dialog v-model="dialog" :title="form.id ? '编辑' : '新增'" width="500px" append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="头像 URL"><el-input v-model="form.avatar" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="职称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="医院"><el-input v-model="form.hospital" /></el-form-item>
        <el-form-item label="科室"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="擅长"><el-input v-model="form.speciality" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.introduction" type="textarea" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminAuditDoctor,
  adminCreateDoctor,
  adminDeleteDoctor,
  adminPageDoctors,
  adminUpdateDoctor
} from '@/api'

const list = ref([])
const total = ref(0)
const dialog = ref(false)
const form = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })

async function load() {
  const res = await adminPageDoctors(query.value)
  list.value = res.data.records
  total.value = res.data.total
}

function onPage(page) {
  query.value.pageNum = page
  load()
}

function openDialog(row) {
  form.value = row ? { ...row } : { status: 1, auditStatus: 'APPROVED' }
  dialog.value = true
}

async function save() {
  form.value.id ? await adminUpdateDoctor(form.value) : await adminCreateDoctor(form.value)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function audit(row, approved) {
  const action = approved ? '通过' : '拒绝'
  await ElMessageBox.confirm(`确认${action}医生“${row.name}”的注册申请？`, '审核确认')
  await adminAuditDoctor(row.id, approved)
  ElMessage.success(`已${action}`)
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除？', '提示')
  await adminDeleteDoctor(id)
  ElMessage.success('已删除')
  load()
}

function auditText(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || '已通过'
}

function auditTagType(status) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status] || 'success'
}

onMounted(load)
</script>
