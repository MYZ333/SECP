<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>健康基本信息</span>
        <el-button type="primary" :icon="Edit" @click="openEdit">修改信息</el-button>
      </div>
    </template>

    <!-- 只读展示 -->
    <el-descriptions :column="2" border>
      <el-descriptions-item label="身高(cm)">{{ profile.height ?? '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="体重(kg)">{{ profile.weight ?? '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="BMI">{{ bmi }}</el-descriptions-item>
      <el-descriptions-item label="血型">{{ profile.bloodType || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="过敏史" :span="2">{{ profile.allergyHistory || '无' }}</el-descriptions-item>
      <el-descriptions-item label="家族病史" :span="2">{{ profile.familyHistory || '无' }}</el-descriptions-item>
      <el-descriptions-item label="既往病史" :span="2">{{ profile.pastHistory || '无' }}</el-descriptions-item>
      <el-descriptions-item label="紧急联系人">{{ profile.emergencyContact || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="紧急联系电话">{{ profile.emergencyPhone || '未填写' }}</el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ profile.remark || '无' }}</el-descriptions-item>
    </el-descriptions>

    <el-empty v-if="!profile.id" description="还没有健康档案，点击右上角「修改信息」完善" />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialog" title="修改健康基本信息" width="600px" append-to-body>
      <el-form :model="form" label-width="120px">
        <el-form-item label="身高(cm)"><el-input-number v-model="form.height" :min="0" :max="250" /></el-form-item>
        <el-form-item label="体重(kg)"><el-input-number v-model="form.weight" :min="0" :max="300" /></el-form-item>
        <el-form-item label="血型">
          <el-select v-model="form.bloodType" placeholder="请选择" clearable>
            <el-option v-for="t in ['A','B','O','AB','未知']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="过敏史">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="家族病史">
          <el-input v-model="form.familyHistory" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="form.pastHistory" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" maxlength="50" /></el-form-item>
        <el-form-item label="紧急联系电话"><el-input v-model="form.emergencyPhone" maxlength="20" /></el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { getHealthProfile, saveHealthProfile } from '@/api'

const profile = ref({})
const form = ref({})
const dialog = ref(false)

const bmi = computed(() => {
  const { height, weight } = profile.value
  if (!height || !weight) return '未填写'
  const h = height / 100
  return (weight / (h * h)).toFixed(1)
})

async function load() {
  const res = await getHealthProfile()
  if (res.data) profile.value = res.data
}

function openEdit() {
  form.value = { ...profile.value }
  dialog.value = true
}

async function save() {
  await saveHealthProfile(form.value)
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
</style>
