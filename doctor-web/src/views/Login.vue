<template>
  <div ref="wrap" class="login-wrap" :class="{ 'font-large': fontMode === 'large' }">
    <ParticleNet :count="74" :link="152" :repel="150" />
    <svg class="ecg" viewBox="0 0 1600 200" preserveAspectRatio="none" aria-hidden="true">
      <path
        ref="ecgPath"
        d="M0,100 L420,100 L450,100 L470,58 L495,150 L515,30 L540,132 L560,100 L600,100 L1000,100 L1030,100 L1050,64 L1075,148 L1095,36 L1120,128 L1140,100 L1600,100"
        fill="none"
        stroke="url(#doctorEcgGradient)"
        stroke-width="2.5"
        stroke-linecap="round"
      />
      <defs>
        <linearGradient id="doctorEcgGradient" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="#37B6D9" stop-opacity="0" />
          <stop offset=".5" stop-color="#2E6FE0" stop-opacity=".85" />
          <stop offset="1" stop-color="#37B6D9" stop-opacity="0" />
        </linearGradient>
      </defs>
    </svg>

    <header class="topbar" data-intro>
      <BrandLogo />
      <div class="font-switch" aria-label="字号切换">
        <button :class="{ on: fontMode === 'large' }" @click="fontMode = 'large'">大字号</button>
        <span></span>
        <button :class="{ on: fontMode === 'normal' }" @click="fontMode = 'normal'">小字号</button>
      </div>
    </header>

    <section class="hero" data-intro>
      <h1><span class="hero-prefix">智慧医养</span><span class="hero-product"><i>“</i><span>医生工作台</span><i>”</i></span></h1>
      <p>患者信息统一管理 · <b>医患实时咨询</b> · 健康风险协同处置</p>
    </section>

    <main ref="panel" class="login-panel" :class="{ registering: tab === 'register' }">
      <section class="qr-side" data-intro>
        <h2>{{ tab === 'register' ? '注册' : '登录' }}</h2>
        <p><a>智慧医养医生端</a>/微信小程序</p>
        <div class="qr-box" aria-label="医生端小程序二维码">
          <svg viewBox="0 0 33 33" shape-rendering="crispEdges">
            <rect v-for="(cell, index) in qrCells" :key="index" :x="cell[0]" :y="cell[1]" width="1" height="1" fill="#111" />
          </svg>
        </div>
        <span class="qr-caption">扫码进入医生小程序</span>
      </section>

      <section class="form-side">
        <div class="tabs-row" data-intro>
          <div class="tabs">
            <button :class="{ on: tab === 'login' }" @click="tab = 'login'">医生登录</button>
            <button :class="{ on: tab === 'register' }" @click="tab = 'register'">医生注册</button>
          </div>
          <a v-if="tab === 'register'" @click="tab = 'login'">返回登录 ›</a>
        </div>

        <transition name="form-swap" mode="out-in">
          <el-form v-if="tab === 'login'" key="login" :model="loginForm" size="large" @submit.prevent>
            <el-form-item>
              <el-input v-model="loginForm.username" autocomplete="username" placeholder="请输入医生用户名" />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="loginForm.password"
                type="password"
                autocomplete="current-password"
                show-password
                placeholder="请输入密码"
                @keyup.enter="doLogin"
              />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doLogin">立即登录</el-button>
            <p class="form-note">演示账号：doctor1 至 doctor20 · 密码 123456</p>
          </el-form>

          <el-form v-else key="register" :model="regForm" size="large" @submit.prevent>
            <div class="register-grid">
              <el-form-item><el-input v-model="regForm.username" placeholder="用户名（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.password" type="password" show-password placeholder="密码（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.name" placeholder="姓名（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.phone" placeholder="手机号" /></el-form-item>
              <el-form-item><el-input v-model="regForm.hospital" placeholder="医院（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.department" placeholder="科室（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.title" placeholder="职称（必填）" /></el-form-item>
              <el-form-item><el-input v-model="regForm.speciality" placeholder="擅长领域" /></el-form-item>
            </div>
            <el-form-item><el-input v-model="regForm.introduction" type="textarea" :rows="3" placeholder="医生简介" /></el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doRegister">提交审核</el-button>
            <p class="form-note">注册申请提交后，需由管理员审核通过才能登录。</p>
          </el-form>
        </transition>
      </section>
    </main>

    <div ref="wipe" class="login-wipe"></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import gsap from 'gsap'
import BrandLogo from '@/components/BrandLogo.vue'
import ParticleNet from '@/components/ParticleNet.vue'
import { login, registerDoctor } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('login')
const fontMode = ref('normal')
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })
const regForm = ref({ username: '', password: '', name: '', phone: '', hospital: '', department: '', title: '', speciality: '', introduction: '' })
const wrap = ref(null)
const panel = ref(null)
const wipe = ref(null)
const ecgPath = ref(null)
let animationContext

const qrCells = (() => {
  const cells = []
  const finder = (offsetX, offsetY) => {
    for (let x = 0; x < 7; x++) for (let y = 0; y < 7; y++) {
      if (x === 0 || x === 6 || y === 0 || y === 6 || (x >= 2 && x <= 4 && y >= 2 && y <= 4)) cells.push([offsetX + x, offsetY + y])
    }
  }
  finder(0, 0); finder(26, 0); finder(0, 26)
  let seed = 57
  const random = () => (seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648
  for (let x = 0; x < 33; x++) for (let y = 0; y < 33; y++) {
    const reserved = (x < 9 && y < 9) || (x > 23 && y < 9) || (x < 9 && y > 23)
    if (!reserved && random() < .45) cells.push([x, y])
  }
  return cells
})()

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const res = await login(loginForm.value)
    if (res.data.role !== 'DOCTOR') return ElMessage.error('请使用医生账号登录')
    userStore.setLogin(res.data)
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      await gsap.to(wipe.value, { scale: 90, duration: .75, ease: 'power3.in' })
    }
    router.push('/dashboard')
  } finally { loading.value = false }
}

async function doRegister() {
  const required = ['username', 'password', 'name', 'hospital', 'department', 'title']
  if (required.some(key => !String(regForm.value[key] || '').trim())) return ElMessage.warning('请填写全部必填信息')
  if (regForm.value.password.length < 6 || regForm.value.password.length > 32) return ElMessage.warning('密码长度为6-32位')
  loading.value = true
  try {
    await registerDoctor(regForm.value)
    ElMessage.success('注册申请已提交，请等待管理员审核')
    tab.value = 'login'
  } finally { loading.value = false }
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  animationContext = gsap.context(() => {
    const timeline = gsap.timeline({ defaults: { ease: 'power3.out' } })
    timeline.from(panel.value, { y: 44, opacity: 0, scale: .97, duration: .85 })
      .from('[data-intro]', { y: 22, opacity: 0, stagger: .08, duration: .65 }, '-=.42')
    const pathLength = ecgPath.value.getTotalLength()
    gsap.set(ecgPath.value, { strokeDasharray: `${pathLength * .22} ${pathLength}`, strokeDashoffset: pathLength * 1.22 })
    gsap.to(ecgPath.value, { strokeDashoffset: -pathLength * .22, duration: 7, repeat: -1, ease: 'none' })
  }, wrap.value)
})
onBeforeUnmount(() => animationContext?.revert())
</script>

<style scoped>
.login-wrap {
  --font-scale: 1;
  position: relative; isolation: isolate; overflow: hidden;
  min-height: 100dvh; padding: 0 24px 40px;
  display: flex; flex-direction: column; align-items: center;
  background:
    radial-gradient(1100px 620px at 8% -4%, #DFEAFB 0%, transparent 58%),
    radial-gradient(950px 640px at 102% 104%, #DCF2F8 0%, transparent 55%),
    var(--hda-bg);
}
.font-large { --font-scale: 1.16; }
.ecg { position: absolute; top: 14%; left: 0; z-index: -1; width: 100%; height: 180px; opacity: .78; pointer-events: none; filter: drop-shadow(0 0 6px rgba(46,111,224,.38)); }
.topbar { position: relative; z-index: 2; width: 100%; max-width: 1440px; padding: 22px 8px 0; display: flex; align-items: center; justify-content: space-between; }
.font-switch { display: flex; align-items: center; gap: 10px; font-size: calc(15px * var(--font-scale)); }
.font-switch button { padding: 4px 2px; border: 0; color: #333; background: transparent; cursor: pointer; font-size: inherit; }
.font-switch button.on { color: var(--el-color-primary); font-weight: 700; }
.font-switch span { width: 1px; height: 14px; background: rgba(0,0,0,.2); }
.hero { position: relative; z-index: 1; margin: clamp(20px, 5vh, 54px) 0 10px; text-align: center; }
.hero h1 { margin: 0; color: #1A1A1A; font-size: calc(46px * var(--font-scale)); font-weight: 800; }
.hero-prefix, .hero-product { display: inline; }
.hero-product { white-space: nowrap; }
.hero h1 i { color: #B9BFD0; font-style: normal; }
.hero h1 span { color: transparent; background: linear-gradient(90deg, #2E6FE0, #37B6D9 52%, #9B6AD8); background-clip: text; }
.hero p { margin: 10px 0 0; color: #4A5468; font-size: calc(18px * var(--font-scale)); }
.hero p b { color: #8E63D2; font-weight: 500; }
.login-panel {
  position: relative; z-index: 2; overflow: hidden;
  display: grid; grid-template-columns: .82fr 1.18fr; gap: 48px;
  width: min(940px, 96vw); min-height: 480px; margin-top: clamp(16px, 4vh, 42px); padding: 48px 56px;
  border: 1px solid rgba(255,255,255,.75); background: rgba(235,243,250,.88);
  box-shadow: var(--hda-shadow-lg), inset 0 1px 0 rgba(255,255,255,.9); backdrop-filter: blur(20px);
  transition: min-height .45s var(--ease-out);
}
.login-panel.registering { min-height: 590px; }
.qr-side { display: flex; flex-direction: column; }
.qr-side h2 { margin: 0 0 18px; color: #111; font-size: calc(34px * var(--font-scale)); letter-spacing: .06em; }
.qr-side p { margin: 0 0 14px; color: #555; font-size: calc(14px * var(--font-scale)); }
.qr-side a { color: var(--el-color-primary); }
.qr-box { width: calc(170px * var(--font-scale)); height: calc(170px * var(--font-scale)); padding: 10px; border: 1px solid rgba(0,0,0,.06); background: #fff; }
.qr-box svg { display: block; width: 100%; height: 100%; }
.qr-caption { margin-top: 12px; color: #8A93A6; font-size: calc(13px * var(--font-scale)); }
.form-side { min-width: 0; padding-top: 6px; }
.tabs-row { min-height: 42px; margin-bottom: 30px; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.tabs { display: flex; gap: 30px; }
.tabs button { position: relative; padding: 0 2px 12px; border: 0; color: #333; background: transparent; cursor: pointer; font-size: calc(16px * var(--font-scale)); }
.tabs button.on { color: var(--el-color-primary); font-weight: 700; }
.tabs button.on::after { content: ""; position: absolute; right: 0; bottom: 0; left: 0; height: 3px; background: var(--el-color-primary); }
.tabs-row > a { color: var(--el-color-primary); cursor: pointer; font-size: calc(14px * var(--font-scale)); font-weight: 600; }
.form-side :deep(.el-form-item) { margin-bottom: 24px; }
.form-side :deep(.el-input__wrapper) { min-height: calc(50px * var(--font-scale)); padding: 0 16px; background: rgba(255,255,255,.92); }
.submit { width: 100%; min-height: calc(50px * var(--font-scale)); margin-top: 18px; font-size: calc(17px * var(--font-scale)); }
.register-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px; }
.registering .form-side :deep(.el-form-item) { margin-bottom: 14px; }
.registering .submit { margin-top: 6px; }
.form-note { margin: 28px 0 0; color: #6C7D92; text-align: center; font-size: calc(13px * var(--font-scale)); }
.form-swap-enter-active, .form-swap-leave-active { transition: opacity .25s var(--ease-out), transform .25s var(--ease-out); }
.form-swap-enter-from { opacity: 0; transform: translateX(14px); }
.form-swap-leave-to { opacity: 0; transform: translateX(-10px); }
.login-wipe { position: fixed; right: 12%; bottom: 12%; z-index: 20; width: 24px; height: 24px; border-radius: 50%; background: var(--el-color-primary); transform: scale(0); pointer-events: none; }
@media (max-width: 760px) {
  .login-wrap { padding: 0 14px 24px; overflow-y: auto; }
  .topbar { padding-top: 16px; }
  .font-switch { display: none; }
  .hero { width: 100%; margin-top: 34px; }
  .hero h1 { font-size: clamp(26px, 8vw, 31px); }
  .hero-prefix, .hero-product { display: block; }
  .hero-product { margin-top: 5px; }
  .hero p { max-width: 340px; margin-right: auto; margin-left: auto; padding: 0 8px; font-size: 14px; line-height: 1.7; }
  .login-panel, .login-panel.registering { grid-template-columns: minmax(0, 1fr); width: calc(100vw - 28px); max-width: calc(100vw - 28px); min-height: 0; padding: 28px 22px; gap: 0; }
  .qr-side { display: none; }
  .form-side, .form-side form, .form-side :deep(.el-form-item), .form-side :deep(.el-input) { width: 100%; min-width: 0; }
  .tabs-row { margin-bottom: 22px; }
  .register-grid { grid-template-columns: 1fr; }
  .form-note { overflow-wrap: anywhere; }
}
</style>
