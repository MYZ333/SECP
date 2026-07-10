<template>
  <el-card>
    <el-button type="primary" @click="openDialog()">新增商品</el-button>
    <el-table :data="list" style="margin-top:15px" border>
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="pointsCost" label="所需积分" />
      <el-table-column prop="stock" label="库存" />
      <el-table-column label="状态"><template #default="{ row }">
        <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '上架' : '下架' }}</el-tag>
      </template></el-table-column>
      <el-table-column label="操作" width="150"><template #default="{ row }">
        <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row.id)">删除</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination style="margin-top:15px" layout="prev, pager, next" :total="total"
                   :page-size="query.pageSize" @current-change="onPage" />
    <el-dialog v-model="dialog" :title="form.id ? '编辑' : '新增'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="图片URL"><el-input v-model="form.image" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="所需积分"><el-input-number v-model="form.pointsCost" :min="0" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item>
        <el-form-item label="上架"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminPageProducts, adminCreateProduct, adminUpdateProduct, adminDeleteProduct } from '@/api'
const list = ref([]), total = ref(0), dialog = ref(false), form = ref({})
const query = ref({ pageNum: 1, pageSize: 10 })
async function load() { const res = await adminPageProducts(query.value); list.value = res.data.records; total.value = res.data.total }
function onPage(p) { query.value.pageNum = p; load() }
function openDialog(row) { form.value = row ? { ...row } : { status: 1, stock: 0, pointsCost: 0 }; dialog.value = true }
async function save() {
  form.value.id ? await adminUpdateProduct(form.value) : await adminCreateProduct(form.value)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(id) { await ElMessageBox.confirm('确认删除?', '提示'); await adminDeleteProduct(id); ElMessage.success('已删除'); load() }
onMounted(load)
</script>
