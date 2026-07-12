<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane label="个人资料" name="profile">
        <el-form :model="profile" label-width="100px" style="max-width:500px">
          <el-form-item label="头像">
            <div class="ava-row">
              <el-avatar :size="72" :src="profile.avatar">
                {{ (profile.nickname || profile.username || 'U').charAt(0) }}
              </el-avatar>
              <el-upload :show-file-list="false" accept="image/*" :http-request="doUpload">
                <el-button :loading="uploading">更换头像</el-button>
              </el-upload>
              <span class="ava-tip">jpg/png，2MB 以内，保存资料后生效</span>
            </div>
          </el-form-item>
          <el-form-item label="用户名"><el-input v-model="profile.username" disabled /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="profile.phone" /></el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="profile.gender">
              <el-radio :value="1">男</el-radio><el-radio :value="2">女</el-radio><el-radio :value="0">保密</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="生日"><el-date-picker v-model="profile.birthday" type="date" value-format="YYYY-MM-DD" /></el-form-item>
          <el-button type="primary" @click="saveProfile">保存资料</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="账户安全" name="security">
        <el-form :model="pwd" label-width="100px" style="max-width:500px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
          <el-button type="primary" @click="savePwd">修改密码</el-button>
        </el-form>
        <el-divider />
        <el-button type="danger" @click="doDeactivate">注销账号</el-button>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMe, updateProfile, changePassword, deactivate, uploadAvatar } from '@/api'
import { useUserStore } from '@/store/user'
const router = useRouter(), userStore = useUserStore()
const tab = ref('profile'), profile = ref({}), pwd = ref({ oldPassword: '', newPassword: '' })
const uploading = ref(false)
onMounted(async () => {
  profile.value = (await getMe()).data
  userStore.updateUserInfo(profile.value)
})
async function saveProfile() {
  const res = await updateProfile(profile.value)
  profile.value = res.data
  userStore.updateUserInfo(res.data)
  ElMessage.success('保存成功')
}

/** 头像上传：成功后回填 URL，点“保存资料”落库 */
async function doUpload({ file }) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await uploadAvatar(fd)
    profile.value.avatar = res.data
    ElMessage.success('头像已上传，点击"保存资料"生效')
  } finally { uploading.value = false }
}
async function savePwd() {
  await changePassword(pwd.value); ElMessage.success('密码已修改，请重新登录')
  userStore.logout(); router.push('/login')
}
async function doDeactivate() {
  await ElMessageBox.confirm('注销后账号将无法登录，确认?', '警告', { type: 'warning' })
  await deactivate(); userStore.logout(); router.push('/login')
}
</script>

<style scoped>
.ava-row { display: flex; align-items: center; gap: 16px; }
.ava-tip { font-size: 13px; color: var(--hda-ink-soft); }
</style>
