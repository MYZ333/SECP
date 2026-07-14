<template>
  <el-card>
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="query.keyword" clearable placeholder="搜索药品名/通用名/分类" @keyup.enter="load" />
        <el-select v-model="query.status" clearable placeholder="状态">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button @click="load">查询</el-button>
      </div>
      <el-button type="primary" @click="openDialog()">新增药品</el-button>
    </div>

    <el-table :data="list" style="margin-top: 15px" border>
      <el-table-column prop="name" label="药品名称" min-width="140" />
      <el-table-column prop="genericName" label="通用名" width="120" />
      <el-table-column prop="category" label="分类" width="110" />
      <el-table-column prop="dosageForm" label="剂型" width="90" />
      <el-table-column prop="specification" label="规格" width="130" />
      <el-table-column label="默认用法" min-width="180">
        <template #default="{ row }">
          {{ [row.defaultUsage, row.defaultDosage, row.defaultFrequency].filter(Boolean).join(' · ') || '未设置' }}
        </template>
      </el-table-column>
      <el-table-column label="线下确认" width="100">
        <template #default="{ row }">
          <el-tag :type="row.requiresOffline ? 'warning' : 'success'">{{ row.requiresOffline ? '需要' : '不需要' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
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

    <el-dialog v-model="dialog" :title="form.id ? '编辑药品' : '新增药品'" width="780px" append-to-body>
      <el-form :model="form" label-width="110px">
        <div class="form-grid">
          <el-form-item label="药品名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="通用名"><el-input v-model="form.genericName" /></el-form-item>
          <el-form-item label="商品名"><el-input v-model="form.brandName" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category" allow-create filterable clearable placeholder="选择或输入分类" style="width: 100%">
              <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="剂型"><el-input v-model="form.dosageForm" /></el-form-item>
          <el-form-item label="规格"><el-input v-model="form.specification" /></el-form-item>
          <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
          <el-form-item label="常用用法"><el-input v-model="form.defaultUsage" /></el-form-item>
          <el-form-item label="常用剂量"><el-input v-model="form.defaultDosage" /></el-form-item>
          <el-form-item label="常用频次"><el-input v-model="form.defaultFrequency" /></el-form-item>
          <el-form-item label="建议疗程">
            <el-input-number v-model="form.defaultDurationDays" :min="0" />
          </el-form-item>
          <el-form-item label="最大天数">
            <el-input-number v-model="form.maxDurationDays" :min="0" />
          </el-form-item>
        </div>
        <el-form-item label="适应症"><el-input v-model="form.indications" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="禁忌症"><el-input v-model="form.contraindications" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="注意事项"><el-input v-model="form.precautions" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="不良反应"><el-input v-model="form.adverseReactions" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="线下确认">
          <el-switch v-model="form.requiresOffline" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
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
import { adminCreateMedicine, adminDeleteMedicine, adminPageMedicines, adminUpdateMedicine } from '@/api'

const categories = ['解热镇痛', '抗过敏', '消化系统', '补液电解质', '抗感染', '慢病用药']
const list = ref([])
const total = ref(0)
const dialog = ref(false)
const form = ref({})
const query = ref({ pageNum: 1, pageSize: 10, keyword: '', status: undefined })

async function load() {
  const res = await adminPageMedicines(query.value)
  list.value = res.data.records || []
  total.value = res.data.total
}

function onPage(page) {
  query.value.pageNum = page
  load()
}

function openDialog(row) {
  form.value = row ? { ...row } : {
    status: 1,
    requiresOffline: 0,
    defaultDurationDays: 0,
    maxDurationDays: 0
  }
  dialog.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('请填写药品名称')
    return
  }
  form.value.id ? await adminUpdateMedicine(form.value) : await adminCreateMedicine(form.value)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该药品？', '提示')
  await adminDeleteMedicine(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.filters { display: flex; align-items: center; gap: 10px; }
.filters .el-input { width: 260px; }
.filters .el-select { width: 120px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 14px; }
@media (max-width: 760px) {
  .toolbar, .filters { align-items: stretch; flex-direction: column; }
  .filters .el-input, .filters .el-select { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
