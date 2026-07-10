<template>
  <el-card>
    <el-button type="primary" @click="openDialog()">新增体征记录</el-button>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="metricType" label="指标类型" />
      <el-table-column prop="metricValue" label="数值" />
      <el-table-column prop="metricValue2" label="第二值" />
      <el-table-column prop="unit" label="单位" />
      <el-table-column prop="measureTime" label="测量时间" />
      <el-table-column label="是否异常">
        <template #default="{ row }">
          <el-tag :type="row.abnormal ? 'danger' : 'success'">{{ row.abnormal ? '异常' : '正常' }}</el-tag>
        </template>
      </el-table-column>
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
        <el-form-item label="指标类型">
          <el-select v-model="form.metricType">
            <el-option label="血压" value="BLOOD_PRESSURE" />
            <el-option label="血糖" value="BLOOD_SUGAR" />
            <el-option label="心率" value="HEART_RATE" />
            <el-option label="体温" value="TEMPERATURE" />
            <el-option label="体重" value="WEIGHT" />
          </el-select>
        </el-form-item>
        <el-form-item label="数值"><el-input-number v-model="form.metricValue" /></el-form-item>
        <el-form-item label="第二值"><el-input-number v-model="form.metricValue2" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="测量时间">
          <el-date-picker v-model="form.measureTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="是否异常"><el-switch v-model="form.abnormal" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMetric, createMetric, updateMetric, deleteMetric } from '@/api'
const list = ref([]), total = ref(0), dialog = ref(false), form = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })
async function load() { const res = await pageMetric(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) { form.value = row ? { ...row } : { abnormal: 0 }; dialog.value = true }
async function save() {
  form.value.id ? await updateMetric(form.value) : await createMetric(form.value)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(id) {
  await ElMessageBox.confirm('确认删除?', '提示'); await deleteMetric(id); ElMessage.success('已删除'); load()
}
onMounted(load)
</script>
