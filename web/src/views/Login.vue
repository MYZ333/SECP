<template>
  <div class="login-wrap">
    <!-- 背景装饰：漂浮的柔和色块 -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>

    <div class="login-panel hda-enter">
      <!-- 左侧品牌区 -->
      <section class="brand">
        <div class="logo">
          <el-icon :size="34"><FirstAidKit /></el-icon>
        </div>
        <h1>智慧医养</h1>
        <p class="slogan">个人健康档案 · 一站式管理</p>
        <ul class="feats">
          <li><el-icon><Notebook /></el-icon> 健康档案随身带</li>
          <li><el-icon><ChatDotRound /></el-icon> AI 健康咨询与预警</li>
          <li><el-icon><GoldMedal /></el-icon> 积分好礼免费兑</li>
        </ul>
      </section>

      <!-- 右侧表单区 -->
      <section class="form-area">
        <div class="tabs">
          <button :class="{ on: tab==='login' }" @click="tab='login'">登录</button>
          <button :class="{ on: tab==='register' }" @click="tab='register'">注册</button>
          <span class="ink" :style="{ transform: tab==='login' ? 'translateX(0)' : 'translateX(100%)' }"></span>
        </div>

        <transition name="swap" mode="out-in">
          <el-form v-if="tab==='login'" key="login" :model="loginForm" size="large" @submit.prevent>
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" show-password
                        placeholder="请输入密码" :prefix-icon="Lock" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>

          <el-form v-else key="register" :model="regForm" size="large" @submit.prevent>
            <el-form-item><el-input v-model="regForm.username" placeholder="用户名（3-20位）" :prefix-icon="User" /></el-form-item>
            <el-form-item><el-input v-model="regForm.password" type="password" show-password placeholder="密码（6-32位）" :prefix-icon="Lock" /></el-form-item>
            <el-form-item><el-input v-model="regForm.nickname" placeholder="昵称（可选）" :prefix-icon="Avatar" /></el-form-item>
            <el-form-item><el-input v-model="regForm.phone" placeholder="手机号（可选）" :prefix-icon="Iphone" /></el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </transition>

        <p class="tip">演示账号：admin / 123456（管理员）&nbsp;·&nbsp; user001 / 123456（用户）</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar, Iphone, FirstAidKit, Notebook, ChatDotRound, GoldMedal } from '@element-plus/icons-vue'
import { login, register } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })
const regForm = ref({ username: '', password: '', nickname: '', phone: '' })

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const res = await login(loginForm.value)
    userStore.setLogin(res.data)
    ElMessage.success('欢迎回来')
    router.push('/dashboard')
  } finally { loading.value = false }
}

async function doRegister() {
  loading.value = true
  try {
    await register(regForm.value)
    ElMessage.success('注册完成，请登录')
    tab.value = 'login'
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100dvh;
  display: flex; align-items: center; justify-content: center;
  background: radial-gradient(1200px 600px at 10% 0%, #E7F5EE 0%, transparent 60%),
              radial-gradient(1000px 700px at 100% 100%, #FBEDDF 0%, transparent 55%),
              var(--hda-bg);
  overflow: hidden; position: relative; padding: 24px;
}
/* 漂浮色块 */
.blob { position: absolute; filter: blur(8px); opacity: .55; animation: hdaBlob 12s ease-in-out infinite, hdaFloat 9s ease-in-out infinite; }
.blob-1 { width: 320px; height: 320px; background: #9FD9C1; top: -60px; left: -40px; }
.blob-2 { width: 260px; height: 260px; background: #F6C39A; bottom: -50px; right: 6%; animation-delay: -3s; }
.blob-3 { width: 180px; height: 180px; background: #BFE3D3; top: 20%; right: 22%; animation-delay: -6s; }

.login-panel {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1.05fr 1fr;
  width: min(920px, 96vw); min-height: 540px;
  background: rgba(255,255,255,.82);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255,255,255,.6);
  border-radius: 30px;
  box-shadow: var(--hda-shadow-lg);
  overflow: hidden;
}

/* 品牌区 */
.brand {
  padding: 56px 48px; color: #fff;
  background: linear-gradient(150deg, #35AE85 0%, #2FA37C 55%, #23856A 100%);
  display: flex; flex-direction: column;
}
.brand .logo {
  width: 68px; height: 68px; border-radius: 20px;
  background: rgba(255,255,255,.2); display: grid; place-items: center;
  margin-bottom: 22px; animation: hdaFloat 5s ease-in-out infinite;
}
.brand h1 { font-size: 40px; margin: 0 0 8px; letter-spacing: .04em; }
.slogan { font-size: 18px; opacity: .92; margin: 0 0 40px; }
.feats { list-style: none; padding: 0; margin: auto 0 0; }
.feats li { display: flex; align-items: center; gap: 12px; font-size: 18px; padding: 12px 0; opacity: .96; }
.feats li .el-icon { font-size: 22px; }

/* 表单区 */
.form-area { padding: 52px 46px; display: flex; flex-direction: column; }
.tabs { position: relative; display: flex; gap: 8px; margin-bottom: 34px; }
.tabs button {
  flex: 1; background: none; border: none; cursor: pointer;
  font-size: 22px; font-weight: 700; color: var(--hda-ink-soft);
  padding: 8px 0; transition: color .3s;
}
.tabs button.on { color: var(--el-color-primary); }
.tabs .ink {
  position: absolute; bottom: -4px; left: 0; width: 50%; height: 4px;
  border-radius: 999px; background: var(--el-color-primary);
  transition: transform .35s cubic-bezier(.2,.8,.2,1);
}
.submit { width: 100%; font-size: 20px; height: 54px; letter-spacing: .3em; margin-top: 6px; }
.tip { margin-top: auto; padding-top: 24px; font-size: 14px; color: var(--hda-ink-soft); text-align: center; }

/* 表单切换动画 */
.swap-enter-active, .swap-leave-active { transition: all .28s cubic-bezier(.2,.8,.2,1); }
.swap-enter-from { opacity: 0; transform: translateX(24px); }
.swap-leave-to { opacity: 0; transform: translateX(-24px); }

@media (max-width: 760px) {
  .login-panel { grid-template-columns: 1fr; }
  .brand { display: none; }
}
</style>
