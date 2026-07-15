<template>
  <div class="login-wrap" :class="{ 'font-large': fontMode === 'large' }" ref="wrap">
    <!-- 粒子网络画布（保持原状） -->
    <canvas ref="canvas" class="net"></canvas>

    <!-- 心电图光线（保持原状） -->
    <svg class="ecg" viewBox="0 0 1600 200" preserveAspectRatio="none">
      <path ref="ecgPath" d="M0,100 L420,100 L450,100 L470,58 L495,150 L515,30 L540,132 L560,100 L600,100 L1000,100 L1030,100 L1050,64 L1075,148 L1095,36 L1120,128 L1140,100 L1600,100"
            fill="none" stroke="url(#ecgGrad)" stroke-width="2.5" stroke-linecap="round" />
      <defs>
        <linearGradient id="ecgGrad" x1="0" y1="0" x2="1" y2="0">
          <stop offset="0" stop-color="#37B6D9" stop-opacity="0" />
          <stop offset=".5" stop-color="#2E6FE0" stop-opacity=".85" />
          <stop offset="1" stop-color="#37B6D9" stop-opacity="0" />
        </linearGradient>
      </defs>
    </svg>

    <!-- 柔和光晕色块（保持原状） -->
    <div class="blob blob-1"></div>
    <div class="blob blob-2"></div>
    <div class="blob blob-3"></div>

    <!-- 顶栏：左上角 Logo + 右上角字号切换 -->
    <header class="topbar">
      <div class="logo-mark" data-intro>
        <svg class="logo-icon" viewBox="0 0 52 34" aria-hidden="true">
          <path d="M15.5 1.5 C7 1.5 1.5 7 1.5 15.5 v3 C1.5 27 7 32.5 15.5 32.5 h6.2 l-4.6-5.6 h-1.6 c-4.6 0-7-2.4-7-7 v-5.8 c0-4.6 2.4-7 7-7 h1.6 L21.7 1.5 Z" />
          <path d="M36.5 1.5 C45 1.5 50.5 7 50.5 15.5 v3 c0 8.5-5.5 14-14 14 h-6.2 l4.6-5.6 h1.6 c4.6 0 7-2.4 7-7 v-5.8 c0-4.6-2.4-7-7-7 h-1.6 L30.3 1.5 Z" />
          <rect x="19" y="14.6" width="14" height="4.8" rx="1" />
        </svg>
        <span class="logo-text">12组</span>
      </div>
      <div class="font-switch" data-intro>
        <button :class="{ on: fontMode === 'large' }" @click="fontMode = 'large'">大字号</button>
        <span class="divider"></span>
        <button :class="{ on: fontMode === 'normal' }" @click="fontMode = 'normal'">小字号</button>
      </div>
    </header>

    <!-- 介绍区：放在登录框上方 -->
    <div class="hero" data-intro>
      <h1>智慧医养 <span class="quote">“</span><span class="grad">个人健康档案</span><span class="quote">”</span></h1>
      <p class="hero-sub">健康档案随身带 · <em>AI 健康咨询与预警</em> · 积分好礼免费兑</p>
    </div>

    <!-- 登录卡片（直角边） -->
    <div class="login-panel" ref="panel">
      <!-- 左侧：登录标题 + 扫码区 -->
      <section class="qr-side" data-intro>
        <h2>{{ tab === 'register' ? '注册' : '登录' }}</h2>
        <p class="qr-tip"><a>智慧医养APP</a>/微信/支付宝</p>
        <div class="qr-box">
          <svg viewBox="0 0 33 33" shape-rendering="crispEdges">
            <rect v-for="(c, i) in qrCells" :key="i" :x="c[0]" :y="c[1]" width="1" height="1" fill="#111" />
          </svg>
        </div>
        <p class="qr-scan">扫码快捷登录</p>
      </section>

      <!-- 右侧：表单区 -->
      <section class="form-side">
        <template v-if="tab === 'account' || tab === 'phone'">
          <div class="tabs-row" data-intro>
            <div class="tabs">
              <button :class="{ on: tab === 'account' }" @click="tab = 'account'">账密登录</button>
              <button :class="{ on: tab === 'phone' }" @click="tab = 'phone'">手机号登录</button>
            </div>
            <div class="links">
              <a class="forgot" @click="tab = 'reset'">忘记密码</a>
              <a class="go-reg" @click="tab = 'register'">前往注册 ›</a>
            </div>
          </div>

          <transition name="swap" mode="out-in">
            <!-- 账密登录 -->
            <el-form v-if="tab === 'account'" key="account" :model="loginForm" size="large" @submit.prevent>
              <el-form-item data-intro>
                <el-input v-model="loginForm.username" placeholder="请输入用户名" />
              </el-form-item>
              <el-form-item data-intro>
                <el-input v-model="loginForm.password" type="password" show-password
                          placeholder="请输入密码" @keyup.enter="doLogin" />
              </el-form-item>
              <el-button type="primary" class="submit" data-intro :loading="loading" @click="doLogin">立即登录</el-button>
            </el-form>

            <!-- 手机号登录（未注册自动注册） -->
            <el-form v-else key="phone" :model="phoneForm" size="large" @submit.prevent>
              <el-form-item>
                <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11">
                  <template #prepend>+86</template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-input v-model="phoneForm.code" placeholder="请输入验证码" maxlength="6" @keyup.enter="doPhoneLogin">
                  <template #prepend>验证码</template>
                  <template #append>
                    <el-button class="code-btn" :disabled="countdown > 0" @click="sendCode(phoneForm.phone)">
                      {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-button type="primary" class="submit" :loading="loading" @click="doPhoneLogin">立即登录</el-button>
              <p class="phone-hint">未注册的手机号验证通过后将自动创建账号</p>
            </el-form>
          </transition>
        </template>

        <!-- 注册 -->
        <template v-else-if="tab === 'register'">
          <div class="tabs-row">
            <div class="tabs"><button class="on">注册账号</button></div>
            <a class="go-reg" @click="tab = 'account'">返回登录 ›</a>
          </div>
          <el-form :model="regForm" size="large" @submit.prevent>
            <el-form-item><el-input v-model="regForm.username" placeholder="用户名（3-20位）" /></el-form-item>
            <el-form-item><el-input v-model="regForm.password" type="password" show-password placeholder="密码（6-32位）" /></el-form-item>
            <el-form-item><el-input v-model="regForm.nickname" placeholder="昵称（可选）" /></el-form-item>
            <el-form-item>
              <el-input v-model="regForm.phone" placeholder="手机号（必填）" maxlength="11">
                <template #prepend>+86</template>
              </el-input>
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doRegister">立即注册</el-button>
          </el-form>
        </template>

        <!-- 忘记密码：手机号+验证码重置 -->
        <template v-else>
          <div class="tabs-row">
            <div class="tabs"><button class="on">忘记密码</button></div>
            <a class="go-reg" @click="tab = 'account'">返回登录 ›</a>
          </div>
          <el-form :model="resetForm" size="large" @submit.prevent>
            <el-form-item>
              <el-input v-model="resetForm.phone" placeholder="请输入注册手机号" maxlength="11">
                <template #prepend>+86</template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-input v-model="resetForm.code" placeholder="请输入验证码" maxlength="6">
                <template #prepend>验证码</template>
                <template #append>
                  <el-button class="code-btn" :disabled="countdown > 0" @click="sendCode(resetForm.phone)">
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item>
              <el-input v-model="resetForm.password" type="password" show-password
                        placeholder="新密码（6-32位）" @keyup.enter="doReset" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="doReset">重置密码</el-button>
          </el-form>
        </template>

        <p class="tip" data-intro>演示账号：admin / 123456（管理员）&nbsp;·&nbsp; user001 / 123456（用户）</p>
      </section>
    </div>

    <!-- 登录成功圆形转场 -->
    <div class="hda-wipe" ref="wipe"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import gsap from 'gsap'
import { login, register, sendSmsCode, phoneLogin, resetPassword } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('account')          // account | phone | register | reset
const fontMode = ref('normal')      // normal | large
const loading = ref(false)
const countdown = ref(0)
let countdownTimer = null

const loginForm = ref({ username: '', password: '' })
const phoneForm = ref({ phone: '', code: '' })
const regForm = ref({ username: '', password: '', nickname: '', phone: '' })
const resetForm = ref({ phone: '', code: '', password: '' })

const wrap = ref(null), canvas = ref(null), panel = ref(null), wipe = ref(null), ecgPath = ref(null)

/* 二维码占位图（伪随机模块，固定种子保证每次渲染一致） */
const qrCells = (() => {
  const cells = []
  const finder = (ox, oy) => {
    for (let x = 0; x < 7; x++) for (let y = 0; y < 7; y++) {
      const edge = x === 0 || x === 6 || y === 0 || y === 6
      const core = x >= 2 && x <= 4 && y >= 2 && y <= 4
      if (edge || core) cells.push([ox + x, oy + y])
    }
  }
  finder(0, 0); finder(26, 0); finder(0, 26)
  let s = 42
  const rnd = () => (s = (s * 1103515245 + 12345) % 2147483648) / 2147483648
  for (let x = 0; x < 33; x++) for (let y = 0; y < 33; y++) {
    const inFinder = (x < 9 && y < 9) || (x > 23 && y < 9) || (x < 9 && y > 23)
    if (!inFinder && rnd() < 0.45) cells.push([x, y])
  }
  return cells
})()

async function doLogin() {
  if (!loginForm.value.username || !loginForm.value.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const res = await login(loginForm.value)
    userStore.setLogin(res.data)
    await gsap.to(wipe.value, { scale: 90, duration: .8, ease: 'power3.in' })
    router.push(homePath(res.data))
  } finally { loading.value = false }
}

/** 发送验证码（手机号登录 / 忘记密码共用；后端限流：60s 间隔、每天 10 条） */
async function sendCode(phone) {
  if (!/^1\d{10}$/.test(phone)) return ElMessage.warning('请输入正确的手机号')
  if (countdown.value > 0) return
  try {
    const res = await sendSmsCode({ phone })
    ElMessage.success(res.message || '验证码已发送')
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(countdownTimer)
    }, 1000)
  } catch (e) { /* 错误信息由响应拦截器统一提示 */ }
}

async function doPhoneLogin() {
  if (!/^1\d{10}$/.test(phoneForm.value.phone)) return ElMessage.warning('请输入正确的手机号')
  if (!/^\d{6}$/.test(phoneForm.value.code)) return ElMessage.warning('请输入6位验证码')
  loading.value = true
  try {
    const res = await phoneLogin({ phone: phoneForm.value.phone, code: phoneForm.value.code })
    userStore.setLogin(res.data)
    await gsap.to(wipe.value, { scale: 90, duration: .8, ease: 'power3.in' })
    router.push(homePath(res.data))
  } finally { loading.value = false }
}

async function doReset() {
  if (!/^1\d{10}$/.test(resetForm.value.phone)) return ElMessage.warning('请输入正确的手机号')
  if (!/^\d{6}$/.test(resetForm.value.code)) return ElMessage.warning('请输入6位验证码')
  if (resetForm.value.password.length < 6 || resetForm.value.password.length > 32)
    return ElMessage.warning('新密码长度为6-32位')
  loading.value = true
  try {
    const res = await resetPassword({
      phone: resetForm.value.phone,
      code: resetForm.value.code,
      newPassword: resetForm.value.password,
    })
    ElMessage.success(res.message || '密码已重置，请使用新密码登录')
    resetForm.value = { phone: '', code: '', password: '' }
    tab.value = 'account'
  } finally { loading.value = false }
}

async function doRegister() {
  if (!regForm.value.username || regForm.value.username.length < 3 || regForm.value.username.length > 20)
    return ElMessage.warning('用户名长度为3-20位')
  if (regForm.value.password.length < 6 || regForm.value.password.length > 32) return ElMessage.warning('密码长度为6-32位')
  if (!/^1\d{10}$/.test(regForm.value.phone)) return ElMessage.warning('请输入正确的手机号')
  loading.value = true
  try {
    await register(regForm.value)
    ElMessage.success('注册完成，请登录')
    tab.value = 'account'
  } finally { loading.value = false }
}

/* ============ 开场动画（GSAP 时间线） ============ */
function homePath(userInfo) {
  return userInfo?.role === 'ADMIN' ? '/admin' : '/dashboard'
}

let ctx
onMounted(() => {
  ctx = gsap.context(() => {
    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })
    tl.from(panel.value, { y: 48, opacity: 0, scale: .96, duration: .9 })
      .from('[data-intro]', { y: 26, opacity: 0, stagger: .09, duration: .7 }, '-=.45')
      .from('.blob', { scale: 0, opacity: 0, stagger: .12, duration: 1.1, ease: 'elastic.out(1, .6)' }, 0)
    const p = ecgPath.value
    const len = p.getTotalLength()
    gsap.set(p, { strokeDasharray: `${len * .22} ${len}`, strokeDashoffset: len * 1.22 })
    gsap.to(p, { strokeDashoffset: -len * .22, duration: 7, repeat: -1, ease: 'none' })
  }, wrap.value)
  initParticles()
})
onBeforeUnmount(() => { ctx && ctx.revert(); stopParticles(); countdownTimer && clearInterval(countdownTimer) })

/* ============ 粒子网络背景（鼠标可交互，保持原状） ============ */
let raf = 0, onResize, onMove
function initParticles() {
  const cv = canvas.value, cx = cv.getContext('2d')
  let W, H, pts = []
  const mouse = { x: -9999, y: -9999 }
  const N = 70, LINK = 150

  function resize() {
    W = cv.width = cv.offsetWidth * devicePixelRatio
    H = cv.height = cv.offsetHeight * devicePixelRatio
  }
  function seed() {
    pts = Array.from({ length: N }, () => ({
      x: Math.random() * W, y: Math.random() * H,
      vx: (Math.random() - .5) * .35 * devicePixelRatio,
      vy: (Math.random() - .5) * .35 * devicePixelRatio,
      r: (Math.random() * 1.6 + .9) * devicePixelRatio
    }))
  }
  function step() {
    cx.clearRect(0, 0, W, H)
    for (const p of pts) {
      p.x += p.vx; p.y += p.vy
      if (p.x < 0 || p.x > W) p.vx *= -1
      if (p.y < 0 || p.y > H) p.vy *= -1
      const dxm = mouse.x - p.x, dym = mouse.y - p.y
      const dm = Math.hypot(dxm, dym)
      if (dm < 220 * devicePixelRatio && dm > 1) { p.x += dxm / dm * .3; p.y += dym / dm * .3 }
      cx.beginPath(); cx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      cx.fillStyle = 'rgba(46,111,224,.45)'; cx.fill()
    }
    const L = LINK * devicePixelRatio
    for (let i = 0; i < N; i++) for (let j = i + 1; j < N; j++) {
      const a = pts[i], b = pts[j]
      const d = Math.hypot(a.x - b.x, a.y - b.y)
      if (d < L) {
        cx.beginPath(); cx.moveTo(a.x, a.y); cx.lineTo(b.x, b.y)
        cx.strokeStyle = `rgba(46,111,224,${(1 - d / L) * .16})`
        cx.lineWidth = devicePixelRatio; cx.stroke()
      }
    }
    raf = requestAnimationFrame(step)
  }
  onResize = () => { resize(); seed() }
  onMove = (e) => {
    const r = cv.getBoundingClientRect()
    mouse.x = (e.clientX - r.left) * devicePixelRatio
    mouse.y = (e.clientY - r.top) * devicePixelRatio
  }
  window.addEventListener('resize', onResize)
  window.addEventListener('mousemove', onMove)
  resize(); seed(); step()
}
function stopParticles() {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('mousemove', onMove)
}
</script>

<style scoped>
.login-wrap {
  --fz: 1; /* 字号缩放系数 */
  min-height: 100dvh;
  display: flex; flex-direction: column; align-items: center;
  background:
    radial-gradient(1100px 620px at 8% -4%, #DFEAFB 0%, transparent 58%),
    radial-gradient(950px 640px at 102% 104%, #DCF2F8 0%, transparent 55%),
    var(--hda-bg);
  overflow: hidden; position: relative; padding: 0 24px 40px;
}
.login-wrap.font-large { --fz: 1.18; }

.net { position: absolute; inset: 0; width: 100%; height: 100%; z-index: 0; }
.ecg {
  position: absolute; left: 0; right: 0; top: 14%; height: 180px; width: 100%;
  z-index: 0; opacity: .8; pointer-events: none;
  filter: drop-shadow(0 0 6px rgba(46,111,224,.45));
}

/* 漂浮光晕（保持原状） */
.blob { position: absolute; filter: blur(10px); opacity: .5; z-index: 0; animation: hdaBlob 12s ease-in-out infinite, hdaFloat 9s ease-in-out infinite; }
.blob-1 { width: 320px; height: 320px; background: #A9C6F5; top: -60px; left: -40px; }
.blob-2 { width: 260px; height: 260px; background: #A5DDEC; bottom: -50px; right: 6%; animation-delay: -3s; }
.blob-3 { width: 180px; height: 180px; background: #C4D8F8; top: 20%; right: 22%; animation-delay: -6s; }

/* ============ 顶栏 ============ */
.topbar {
  position: relative; z-index: 3;
  width: 100%; max-width: 1440px;
  display: flex; align-items: center; justify-content: space-between;
  padding: 22px 8px 0;
}
.logo-mark { display: flex; align-items: center; gap: 8px; }
.logo-icon { width: calc(40px * var(--fz)); height: auto; fill: #FF6A00; }
.logo-text {
  font-size: calc(28px * var(--fz)); font-weight: 700; color: #FF6A00;
  letter-spacing: .02em; line-height: 1;
}
.font-switch {
  display: flex; align-items: center; gap: 10px;
  font-size: calc(15px * var(--fz)); color: #333;
}
.font-switch button {
  background: none; border: none; cursor: pointer; padding: 4px 2px;
  font-size: inherit; color: #333; transition: color .2s;
}
.font-switch button.on { color: var(--el-color-primary); font-weight: 700; }
.font-switch .divider { width: 1px; height: 14px; background: rgba(0,0,0,.2); }

/* ============ 介绍区（登录框上方） ============ */
.hero { position: relative; z-index: 2; text-align: center; margin: clamp(20px, 5vh, 56px) 0 10px; }
.hero h1 {
  margin: 0; font-size: calc(46px * var(--fz)); font-weight: 800;
  color: #1a1a1a; letter-spacing: .02em;
}
.hero .quote { color: #b9bfd0; }
.hero .grad {
  background: linear-gradient(90deg, #2E6FE0 0%, #37B6D9 50%, #b06ae0 100%);
  -webkit-background-clip: text; background-clip: text; color: transparent;
}
.hero-sub { margin: 10px 0 0; font-size: calc(18px * var(--fz)); color: #4a5468; }
.hero-sub em { font-style: normal; color: #b06ae0; }

/* ============ 登录卡片（直角边） ============ */
.login-panel {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: .82fr 1.18fr;
  width: min(940px, 96vw); min-height: 480px;
  margin-top: clamp(16px, 4vh, 44px);
  background: rgba(235, 243, 250, .88);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,.7);
  border-radius: 0; /* 直角 */
  box-shadow: var(--hda-shadow-lg), inset 0 1px 0 rgba(255,255,255,.9);
  overflow: hidden;
  padding: 48px 56px;
  gap: 48px;
}

/* 左侧扫码区 */
.qr-side { display: flex; flex-direction: column; }
.qr-side h2 { margin: 0 0 18px; font-size: calc(34px * var(--fz)); color: #111; letter-spacing: .06em; }
.qr-tip { margin: 0 0 14px; font-size: calc(14px * var(--fz)); color: #555; }
.qr-tip a { color: var(--el-color-primary); cursor: pointer; }
.qr-box {
  width: calc(170px * var(--fz)); height: calc(170px * var(--fz));
  background: #fff; padding: 10px; border: 1px solid rgba(0,0,0,.06);
}
.qr-box svg { width: 100%; height: 100%; display: block; }
.qr-scan { margin: 12px 0 0; font-size: calc(13px * var(--fz)); color: #999; }

/* 右侧表单区 */
.form-side { display: flex; flex-direction: column; }
.tabs-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 30px; }
.tabs { display: flex; gap: 28px; }
.tabs button {
  background: none; border: none; cursor: pointer;
  font-size: calc(17px * var(--fz)); font-weight: 700; color: #333;
  padding: 6px 2px 10px; position: relative; transition: color .25s;
}
.tabs button.on { color: var(--el-color-primary); }
.tabs button.on::after {
  content: ""; position: absolute; left: 0; right: 0; bottom: 0;
  height: 3px; background: var(--el-color-primary);
}
.links { display: flex; align-items: center; gap: 18px; }
.go-reg {
  font-size: calc(15px * var(--fz)); font-weight: 700;
  color: var(--el-color-primary); cursor: pointer;
}
.forgot {
  font-size: calc(14px * var(--fz)); color: #8a93a6; cursor: pointer;
  transition: color .2s;
}
.forgot:hover { color: var(--el-color-primary); }
.phone-hint {
  margin: 10px 0 0; font-size: calc(13px * var(--fz));
  color: #8a93a6; text-align: center;
}

/* 直角输入框 */
.form-side :deep(.el-input__wrapper),
.form-side :deep(.el-input-group__prepend),
.form-side :deep(.el-input-group__append) { border-radius: 0; }
.form-side :deep(.el-input__inner) { font-size: calc(15px * var(--fz)); }
.form-side :deep(.el-input-group__prepend) { background: #fff; color: #333; font-size: calc(15px * var(--fz)); }
.form-side :deep(.el-form-item) { margin-bottom: 24px; }
.code-btn { font-size: calc(14px * var(--fz)); }

.submit {
  width: 100%; font-size: calc(17px * var(--fz)); height: calc(50px * var(--fz));
  letter-spacing: .1em; margin-top: 4px; border-radius: 0; /* 直角按钮 */
}
.tip { margin-top: auto; padding-top: 22px; font-size: calc(13px * var(--fz)); color: var(--hda-ink-soft); text-align: center; }

/* 表单切换动画 */
.swap-enter-active, .swap-leave-active { transition: all .28s cubic-bezier(.2,.8,.2,1); }
.swap-enter-from { opacity: 0; transform: translateX(24px); }
.swap-leave-to { opacity: 0; transform: translateX(-24px); }

@media (max-width: 820px) {
  .login-panel { grid-template-columns: 1fr; padding: 32px 24px; gap: 24px; }
  .qr-side { display: none; }
  .hero h1 { font-size: calc(32px * var(--fz)); }
}
</style>
