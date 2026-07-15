<template>
  <div class="app hda-ambient">
    <!-- 顶部导航栏（阿里云风格：悬停向下展开；滚动 >80px 收缩为毛玻璃悬浮条） -->
    <header class="topnav" :class="{ scrolled }" @mouseleave="openKey = null">
      <div class="nav-inner">
        <div class="logo" @click="router.push(homePath)">
          <svg class="logo-icon" viewBox="0 0 52 34" aria-hidden="true">
            <path d="M15.5 1.5 C7 1.5 1.5 7 1.5 15.5 v3 C1.5 27 7 32.5 15.5 32.5 h6.2 l-4.6-5.6 h-1.6 c-4.6 0-7-2.4-7-7 v-5.8 c0-4.6 2.4-7 7-7 h1.6 L21.7 1.5 Z" />
            <path d="M36.5 1.5 C45 1.5 50.5 7 50.5 15.5 v3 c0 8.5-5.5 14-14 14 h-6.2 l4.6-5.6 h1.6 c4.6 0 7-2.4 7-7 v-5.8 c0-4.6-2.4-7-7-7 h-1.6 L30.3 1.5 Z" />
            <rect x="19" y="14.6" width="14" height="4.8" rx="1" />
          </svg>
          <span class="logo-text">12组</span>
          <span class="logo-sub">智慧医养</span>
        </div>

        <nav class="nav-items">
          <template v-for="item in navList" :key="item.key">
            <router-link v-if="item.children" :to="item.path" class="nav-link"
                 :class="{ active: isActive(item), open: openKey === item.key }"
                 @mouseenter="openKey = item.key"
                 @click="openKey = null">
              {{ item.title }}
            </router-link>
            <router-link v-else :to="item.path" class="nav-link"
                         :class="{ active: isActive(item) }"
                         @mouseenter="openKey = null">
              {{ item.title }}
            </router-link>
          </template>
        </nav>

        <ApplicationAssistant v-if="showApplicationAssistant" />

        <div class="user-wrap" @mouseenter="showSummary" @mouseleave="sumVisible = false" @click="sumVisible = false">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user">
              <el-avatar class="ava" :size="36" :src="userAvatar">{{ userInitial }}</el-avatar>
              <span class="uname">{{ userDisplayName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="sq-dropdown">
                <el-dropdown-item command="account">账户管理</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 头像 hover：今日健康摘要（毛玻璃小卡片） -->
          <transition name="sumpop">
            <div v-if="userStore.isPatient && sumVisible" class="ava-pop" @click.stop>
              <p class="ap-title">今日健康摘要</p>
              <p class="ap-date">{{ sumDate }}</p>
              <template v-if="sumLoaded">
                <div class="ap-row"><span>今日体征记录</span><b>{{ summary.todayMetric }} 条</b></div>
                <div class="ap-row"><span>未处理预警</span><b :class="{ warn: summary.alert > 0 }">{{ summary.alert }} 条</b></div>
                <div class="ap-row"><span>当前积分</span><b class="pt">{{ summary.balance }}</b></div>
              </template>
              <p v-else class="ap-loading">正在读取…</p>
            </div>
          </transition>
        </div>
      </div>

      <!-- 向下展开的子菜单面板 -->
      <transition name="drop">
        <div v-if="openItem" class="mega">
          <div class="mega-inner">
            <router-link v-for="c in openItem.children" :key="c.path" :to="c.path"
                         class="mega-link" :class="{ on: $route.path === c.path }"
                         @click="openKey = null">
              <span class="t">{{ c.title }}</span>
              <span class="d">{{ c.desc }}</span>
            </router-link>
          </div>
        </div>
      </transition>
    </header>

    <main class="page-main">
      <router-view v-slot="{ Component }">
        <transition name="page">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElNotification } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getPointBalance, pageMetric, getAlertSummary, logoutApi } from '@/api'
import ApplicationAssistant from '@/components/ApplicationAssistant.vue'
import { resolveServerUrl } from '@/config/server'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const openKey = ref(null)
const userDisplayName = computed(() => userStore.userInfo.nickname || userStore.userInfo.username || '用户')
const userInitial = computed(() => userDisplayName.value.charAt(0))
const userAvatar = computed(() => resolveServerUrl(userStore.userInfo.avatar))
const homePath = computed(() => userStore.isAdmin && !userStore.isPatient ? '/admin' : '/dashboard')
const showApplicationAssistant = computed(() => userStore.isPatient && !route.path.startsWith('/admin'))

/* —— 导航滚动收缩 —— */
const scrolled = ref(false)
const onWinScroll = () => { scrolled.value = window.scrollY > 80 }
onMounted(() => { window.addEventListener('scroll', onWinScroll, { passive: true }); onWinScroll() })
onBeforeUnmount(() => window.removeEventListener('scroll', onWinScroll))

/* —— 被动预警轮询：接收由体征事件自动生成或更新的预警 —— */
let alertPollTimer = null
let latestAlertTrigger = null
async function pollHealthAlerts() {
  if (!userStore.isPatient) return
  try {
    const response = await getAlertSummary()
    const current = response.data || {}
    const trigger = current.latestCreateTime || null
    summary.value.alert = current.active || 0
    if (latestAlertTrigger && trigger && trigger !== latestAlertTrigger) {
      ElNotification({
        title: current.highActive > 0 ? '收到高危健康预警' : '健康预警已更新',
        message: `当前有 ${current.active || 0} 条预警待处理，点击查看详情`,
        type: current.highActive > 0 ? 'error' : 'warning',
        duration: 8000,
        onClick: () => router.push('/alert')
      })
    }
    latestAlertTrigger = trigger
  } catch { /* 后台轮询失败不干扰当前页面 */ }
}
onMounted(() => {
  if (!userStore.isPatient) return
  pollHealthAlerts()
  alertPollTimer = window.setInterval(pollHealthAlerts, 30000)
})
onBeforeUnmount(() => { if (alertPollTimer) window.clearInterval(alertPollTimer) })

/* —— 头像 hover：今日健康摘要 —— */
const sumVisible = ref(false)
const sumLoaded = ref(false)
const summary = ref({ balance: 0, todayMetric: 0, alert: 0 })
const sumDate = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
async function showSummary() {
  if (!userStore.isPatient) return
  sumVisible.value = true
  if (sumLoaded.value) return
  try {
    const [bal, met, al] = await Promise.allSettled([
      getPointBalance(),
      pageMetric({ pageNum: 1, pageSize: 30 }),
      getAlertSummary(),
    ])
    const today = new Date()
    const key = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    if (bal.status === 'fulfilled') summary.value.balance = bal.value.data ?? 0
    if (met.status === 'fulfilled')
      summary.value.todayMetric = (met.value.data.records || []).filter(r => String(r.measureTime || '').slice(0, 10) === key).length
    if (al.status === 'fulfilled') summary.value.alert = al.value.data.active
    sumLoaded.value = true
  } catch (e) { /* 静默失败，卡片保持加载态 */ }
}

const navList = computed(() => {
  if (userStore.isAdmin && !userStore.isPatient) {
    return [
      { key: 'adminHome', title: '系统管理', path: '/admin' },
      { key: 'adminUser', title: '用户管理', path: '/admin/user' },
      { key: 'adminProduct', title: '商品管理', path: '/admin/product' },
      { key: 'adminMedicine', title: '药品库', path: '/admin/medicine' },
      { key: 'adminDoctor', title: '专家管理', path: '/admin/doctor' },
      { key: 'adminKnowledge', title: '智能体知识库', path: '/admin/knowledge' },
      { key: 'account', title: '账户管理', path: '/account' },
    ]
  }
  const list = [
    { key: 'dashboard', title: '首页', path: '/dashboard' },
    {
      key: 'health', title: '健康档案', path: '/health', base: '/health',
      children: [
        { title: '基本信息', path: '/health/profile', desc: '个人资料与健康标签' },
        { title: '体征数据', path: '/health/metric', desc: '血压、血糖等日常记录' },
        { title: '就诊记录', path: '/health/medical', desc: '历次就诊与用药信息' },
        { title: '健康报告', path: '/health/report', desc: '周期性健康分析报告' },
        { title: '健康时间轴', path: '/health/timeline', desc: '串联体征、预警、咨询与用药闭环' },
      ],
    },
    {
      key: 'point', title: '积分中心', path: '/point', base: '/point',
      children: [
        { title: '积分商城', path: '/point/mall', desc: '积分好礼免费兑换' },
        { title: '积分明细', path: '/point/record', desc: '积分获取与消耗记录' },
      ],
    },
    { key: 'doctor', title: '医生专家库', path: '/doctor' },
    { key: 'consult', title: '健康助手', path: '/consult' },
    { key: 'doctorConsult', title: '医生咨询', path: '/doctor-consult' },
    { key: 'alert', title: '健康预警', path: '/alert' },
    { key: 'account', title: '账户管理', path: '/account' },
  ]
  if (userStore.isAdmin) {
    list.push({
      key: 'admin', title: '系统管理', path: '/admin', base: '/admin',
      children: [
        { title: '用户管理', path: '/admin/user', desc: '平台用户与权限管理' },
        { title: '商品管理', path: '/admin/product', desc: '积分商城商品维护' },
        { title: '药品库', path: '/admin/medicine', desc: '医生用药建议可选药品维护' },
        { title: '专家管理', path: '/admin/doctor', desc: '医生专家信息维护' },
        { title: '智能体知识库', path: '/admin/knowledge', desc: '健康助手与应用助手资料分库管理' },
      ],
    })
  }
  return list
})

const openItem = computed(() => navList.value.find(i => i.key === openKey.value && i.children))

function isActive(item) {
  if (item.path) return route.path === item.path
  return item.base && route.path.startsWith(item.base)
}
async function onCommand(cmd) {
  if (cmd === 'logout') {
    // 通知后端登出：当前 token 拉入黑名单、清除登录态
    try { await logoutApi() } catch (e) { /* 忽略：本地仍会清除 */ }
    userStore.logout(); router.push('/login')
  }
  if (cmd === 'account') router.push('/account')
}
</script>

<style scoped>
.app { min-height: 100vh; display: flex; flex-direction: column; }

/* ============ 顶部导航栏 ============ */
.topnav {
  position: sticky; top: 0; z-index: 100;
  background: #fff;
  border-bottom: 1px solid rgba(28,63,120,.08);
  box-shadow: 0 2px 10px rgba(28,63,120,.06);
  transition: background-color .5s cubic-bezier(.22,1,.36,1), box-shadow .5s cubic-bezier(.22,1,.36,1);
}
/* 滚动 >80px：毛玻璃悬浮条 + 高度收缩 */
.topnav.scrolled {
  background: rgba(255,255,255,.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom-color: rgba(255,255,255,.65);
  box-shadow: 0 4px 16px rgba(64,128,255,.08);
}
.nav-inner {
  height: 64px; max-width: 1440px; margin: 0 auto;
  display: flex; align-items: center; gap: 36px;
  padding: 0 28px;
  transition: height .5s cubic-bezier(.22,1,.36,1);
}
.topnav.scrolled .nav-inner { height: 52px; }
.topnav.scrolled .nav-link { height: 52px; }

/* Logo（与登录页一致） */
.logo { display: flex; align-items: center; gap: 8px; cursor: pointer; flex-shrink: 0; }
.logo-icon { width: 34px; height: auto; fill: #FF6A00; }
.logo-text { font-size: 24px; font-weight: 700; color: #FF6A00; line-height: 1; }
.logo-sub { font-size: 14px; color: #999; margin-left: 2px; padding-top: 6px; }

/* 导航项（无图标） */
.nav-items { display: flex; align-items: center; gap: 4px; flex: 1; min-width: 0; height: 100%; overflow: hidden; }
.nav-link {
  position: relative; display: flex; align-items: center; height: 64px;
  padding: 0 16px; font-size: 16px; color: #333; cursor: pointer;
  text-decoration: none; user-select: none;
  transition: color .2s, height .5s cubic-bezier(.22,1,.36,1);
}
.nav-link:hover, .nav-link.open { color: var(--el-color-primary); }
.nav-link.active { color: var(--el-color-primary); font-weight: 700; }
.nav-link.active::after, .nav-link.open::after {
  content: ""; position: absolute; left: 12px; right: 12px; bottom: 0;
  height: 3px; background: var(--el-color-primary);
}

/* 用户区（直角） */
.user {
  display: flex; align-items: center; gap: 10px; cursor: pointer;
  padding: 6px 12px; transition: background .2s;
}
.user:hover { background: var(--el-color-primary-light-9); }
.ava {
  width: 36px; height: 36px; display: grid; place-items: center;
  background: linear-gradient(135deg, #3E86EC, #2E6FE0);
  color: #fff; font-weight: 700; font-size: 16px;
}
.uname { font-size: 15px; font-weight: 600; color: var(--hda-ink); }

@media (max-width: 1280px) {
  .nav-inner { gap: 16px; padding-inline: 18px; }
  .nav-link { padding-inline: 10px; font-size: 15px; }
}
@media (max-width: 1050px) {
  .logo-sub, .uname { display: none; }
  .user { padding-inline: 4px; }
}

/* ============ 头像 hover：今日健康摘要卡 ============ */
.user-wrap { position: relative; }
.ava-pop {
  position: absolute; right: 0; top: calc(100% + 8px); z-index: 200;
  width: 264px; padding: 16px;
  background: rgba(255,255,255,.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,.65);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(64,128,255,.08);
}
.ap-title { margin: 0; font-size: 16px; font-weight: 700; color: var(--hda-ink); }
.ap-date { margin: 2px 0 12px; font-size: 13px; color: #93a2b5; }
.ap-row {
  display: flex; justify-content: space-between; align-items: baseline;
  padding: 6px 0; font-size: 15px; color: var(--hda-ink-soft);
}
.ap-row + .ap-row { border-top: 1px solid rgba(223,231,242,.7); }
.ap-row b { font-size: 16px; color: var(--hda-ink); font-variant-numeric: tabular-nums; }
.ap-row b.warn { color: #E5654B; }
.ap-row b.pt { color: #FF6A00; }
.ap-loading { margin: 8px 0 0; font-size: 14px; color: #93a2b5; }
.sumpop-enter-active, .sumpop-leave-active { transition: opacity .4s cubic-bezier(.22,1,.36,1), transform .4s cubic-bezier(.22,1,.36,1); }
.sumpop-enter-from, .sumpop-leave-to { opacity: 0; transform: translateY(-6px); }
@media (prefers-reduced-motion: reduce) {
  .topnav, .nav-inner, .nav-link, .sumpop-enter-active, .sumpop-leave-active { transition: none !important; }
}

/* ============ 悬停向下展开的子菜单 ============ */
.mega {
  position: absolute; left: 0; right: 0; top: 100%;
  background: #fff;
  border-top: 1px solid rgba(28,63,120,.06);
  box-shadow: 0 18px 36px rgba(28,63,120,.14);
  transform-origin: top;
  overflow: hidden;
}
.mega-inner {
  max-width: 1440px; margin: 0 auto; padding: 26px 28px 30px;
  display: flex; flex-wrap: wrap; gap: 8px 56px;
}
.mega-link {
  display: flex; flex-direction: column; gap: 5px;
  min-width: 220px; padding: 12px 14px;
  text-decoration: none; transition: background .2s;
}
.mega-link:hover { background: #F4F8FE; }
.mega-link .t { font-size: 16px; font-weight: 700; color: #222; }
.mega-link:hover .t, .mega-link.on .t { color: var(--el-color-primary); }
.mega-link .d { font-size: 13px; color: #8a93a6; }

/* 展开动画：向下滑出 */
.drop-enter-active, .drop-leave-active { transition: all .28s cubic-bezier(.2,.8,.2,1); }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: scaleY(.6); }

/* ============ 内容区 ============ */
.page-main { flex: 1; position: relative; z-index: 1; max-width: 100%; overflow-x: clip; padding: 28px; }
</style>

<style>
/* 用户下拉菜单：直角、无图标 */
.sq-dropdown.el-dropdown-menu { border-radius: 0; padding: 4px 0; }
.sq-dropdown .el-dropdown-menu__item { border-radius: 0; font-size: 15px; padding: 10px 22px; }
.el-popper.is-light:has(.sq-dropdown) { border-radius: 0; }
</style>
