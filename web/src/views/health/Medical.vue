<template>
  <el-card>
    <el-button type="primary" @click="openDialog()">新增就诊记录</el-button>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="visitDate" label="就诊日期" />
      <el-table-column prop="hospital" label="医院" />
      <el-table-column prop="department" label="科室" />
      <el-table-column prop="doctorName" label="医生" />
      <el-table-column prop="diagnosis" label="诊断" />
      <el-table-column prop="prescription" label="处方/用药" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
    <el-dialog v-model="dialog" :title="form.id ? '编辑' : '新增'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="就诊日期"><el-date-picker v-model="form.visitDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="医院"><el-input v-model="form.hospital" /></el-form-item>
        <el-form-item label="科室"><el-input v-model="form.department" /></el-form-item>
        <el-form-item label="医生"><el-input v-model="form.doctorName" /></el-form-item>
        <el-form-item label="诊断"><el-input v-model="form.diagnosis" type="textarea" /></el-form-item>
        <el-form-item label="处方/用药"><el-input v-model="form.prescription" type="textarea" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMedical, createMedical, updateMedical, deleteMedical } from '@/api'
const list = ref([]), total = ref(0), dialog = ref(false), form = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })
async function load() { const res = await pageMedical(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) { form.value = row ? { ...row } : {}; dialog.value = true }
async function save() {
  form.value.id ? await updateMedical(form.value) : await createMedical(form.value)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(id) { await ElMessageBox.confirm('确认删除?', '提示'); await deleteMedical(id); ElMessage.success('已删除'); load() }
onMounted(load)
</script>
