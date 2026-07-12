<template>
  <div class="profile-page">
    <div class="page-heading">
      <div><h1>个人资料</h1><p>维护患者可见的医生身份与专业信息。</p></div>
    </div>

    <el-skeleton v-if="loading" animated :rows="8" class="profile-skeleton" />
    <div v-else class="profile-layout">
      <aside class="identity glass-panel">
        <div class="avatar-wrap">
          <el-avatar :size="96" :src="form.avatar">{{ initial }}</el-avatar>
          <el-upload :show-file-list="false" accept="image/*" :http-request="upload">
            <el-tooltip content="更换头像" placement="right">
              <el-button class="avatar-button" type="primary" circle :loading="uploading" aria-label="更换头像"><el-icon><Camera /></el-icon></el-button>
            </el-tooltip>
          </el-upload>
        </div>
        <h2>{{ form.name || '医生姓名' }}</h2>
        <p>{{ form.title || '职称未填写' }}</p>
        <div class="identity-line"></div>
        <dl>
          <div><dt>医院</dt><dd>{{ form.hospital || '未填写' }}</dd></div>
          <div><dt>科室</dt><dd>{{ form.department || '未填写' }}</dd></div>
          <div><dt>审核状态</dt><dd><el-tag :type="form.auditStatus === 'APPROVED' ? 'success' : 'warning'">{{ auditText }}</el-tag></dd></div>
        </dl>
      </aside>

      <el-card class="profile-form-card">
        <el-form :model="form" label-position="top" size="large">
          <section class="form-section">
            <div class="section-title"><el-icon><User /></el-icon><div><h3>基本信息</h3><p>用于医生端身份展示</p></div></div>
            <div class="form-grid">
              <el-form-item label="姓名"><el-input v-model="form.name" placeholder="请输入姓名" /></el-form-item>
              <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
            </div>
          </section>

          <section class="form-section">
            <div class="section-title"><el-icon><OfficeBuilding /></el-icon><div><h3>执业信息</h3><p>帮助患者了解您的专业背景</p></div></div>
            <div class="form-grid">
              <el-form-item label="医院"><el-input v-model="form.hospital" placeholder="请输入执业医院" /></el-form-item>
              <el-form-item label="科室"><el-input v-model="form.department" placeholder="请输入科室" /></el-form-item>
              <el-form-item label="职称"><el-input v-model="form.title" placeholder="请输入职称" /></el-form-item>
              <el-form-item label="擅长领域"><el-input v-model="form.speciality" placeholder="请输入擅长领域" /></el-form-item>
            </div>
            <el-form-item label="医生简介"><el-input v-model="form.introduction" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="介绍您的诊疗经验与专业方向" /></el-form-item>
          </section>

          <footer class="form-actions">
            <span>资料保存后会同步展示给咨询患者。</span>
            <div><el-button @click="reset">恢复修改</el-button><el-button type="primary" :loading="saving" @click="save"><el-icon><Check /></el-icon>保存资料</el-button></div>
          </footer>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Check, OfficeBuilding, User } from '@element-plus/icons-vue'
import { getMe, updateMe, uploadAvatar } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const form = ref({})
const original = ref({})
const loading = ref(true)
const uploading = ref(false)
const saving = ref(false)
const initial = computed(() => (form.value.name || '医').charAt(0))
const auditText = computed(() => ({ APPROVED: '已通过', PENDING: '待审核', REJECTED: '已拒绝' }[form.value.auditStatus] || '已通过'))

onMounted(async () => {
  try {
    form.value = (await getMe()).data || {}
    original.value = { ...form.value }
  } finally { loading.value = false }
})

async function save() {
  saving.value = true
  try {
    const res = await updateMe(form.value)
    form.value = res.data
    original.value = { ...form.value }
    userStore.updateUserInfo({ nickname: form.value.name, avatar: form.value.avatar })
    ElMessage.success('资料已保存')
  } finally { saving.value = false }
}
function reset() { form.value = { ...original.value }; ElMessage.info('已恢复到上次保存的内容') }
async function upload({ file }) {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadAvatar(formData)
    form.value.avatar = res.data
    ElMessage.success('头像已上传，保存资料后生效')
  } finally { uploading.value = false }
}
</script>

<style scoped>
.profile-skeleton { padding: 30px; background: rgba(255,255,255,.72); }
.profile-layout { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 18px; align-items: start; }
.identity { position: sticky; top: 84px; padding: 34px 26px; text-align: center; }
.avatar-wrap { position: relative; width: 96px; margin: 0 auto; }
.avatar-wrap .el-avatar { color: #fff; background: linear-gradient(135deg, #3E86EC, #2E6FE0); font-size: 34px; font-weight: 700; }
.avatar-button { position: absolute; right: -8px; bottom: -4px; width: 34px; height: 34px; padding: 0; border-radius: 50% !important; }
.identity h2 { margin: 18px 0 2px; color: var(--hda-ink); font-size: 24px; }
.identity > p { margin: 0; color: var(--el-color-primary); font-size: 15px; }
.identity-line { height: 1px; margin: 24px 0 16px; background: var(--hda-line); }
.identity dl { margin: 0; text-align: left; }
.identity dl div { padding: 8px 0; display: flex; justify-content: space-between; gap: 14px; }
.identity dt { color: #8A99AD; font-size: 13px; }
.identity dd { margin: 0; overflow: hidden; color: var(--hda-ink); font-size: 14px; font-weight: 600; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
.profile-form-card :deep(.el-card__body) { padding: 0 30px; }
.form-section { padding: 28px 0 14px; }
.form-section + .form-section { border-top: 1px solid var(--hda-line); }
.section-title { margin-bottom: 20px; display: flex; align-items: flex-start; gap: 11px; }
.section-title > .el-icon { width: 38px; height: 38px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 20px; }
.section-title h3 { margin: 0; color: var(--hda-ink); font-size: 18px; }
.section-title p { margin: 2px 0 0; color: #8A99AD; font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.profile-form-card :deep(.el-form-item__label) { color: #536981; font-weight: 600; }
.profile-form-card :deep(.el-textarea__inner) { padding: 13px 15px; }
.form-actions { min-height: 84px; margin: 8px -30px 0; padding: 18px 30px; border-top: 1px solid var(--hda-line); display: flex; align-items: center; justify-content: space-between; gap: 20px; background: rgba(247,250,253,.7); }
.form-actions > span { color: #7D8EA4; font-size: 13px; }
.form-actions > div { display: flex; gap: 10px; }
@media (max-width: 900px) { .profile-layout { grid-template-columns: 1fr; } .identity { position: static; } }
@media (max-width: 620px) { .form-grid { grid-template-columns: 1fr; } .profile-form-card :deep(.el-card__body) { padding: 0 18px; } .form-actions { margin-right: -18px; margin-left: -18px; padding: 16px 18px; align-items: stretch; flex-direction: column; } .form-actions > div { display: grid; grid-template-columns: 1fr 1fr; } }
</style>
