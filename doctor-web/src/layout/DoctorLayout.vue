<template>
  <div class="app hda-ambient">
    <header class="topnav" :class="{ scrolled }">
      <div class="nav-inner">
        <BrandLogo subtitle="智慧医养医生端" compact class="logo" @click="$router.push('/dashboard')" />

        <nav class="nav-items" aria-label="医生端主导航">
          <router-link v-for="item in nav" :key="item.path" :to="item.path" class="nav-link">
            {{ item.name }}
          </router-link>
        </nav>

        <div class="user-wrap" @mouseenter="showSummary" @mouseleave="summaryVisible = false">
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user">
              <el-avatar class="avatar" :size="36" :src="userAvatar">{{ initial }}</el-avatar>
              <span class="user-name">{{ displayName }}</span>
              <el-icon class="chevron"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="doctor-dropdown">
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <transition name="summary-pop">
            <div v-if="summaryVisible" class="summary-pop" @click.stop>
              <p class="summary-title">今日工作摘要</p>
              <p class="summary-date">{{ currentDate }}</p>
              <template v-if="summaryLoaded">
                <div class="summary-row"><span>平台患者</span><b>{{ summary.patientCount }} 位</b></div>
                <div class="summary-row"><span>进行中会话</span><b>{{ summary.openSessionCount }} 个</b></div>
                <div class="summary-row"><span>未读消息</span><b :class="{ warn: summary.unreadCount > 0 }">{{ summary.unreadCount }} 条</b></div>
              </template>
              <el-skeleton v-else :rows="3" animated />
            </div>
          </transition>
        </div>
      </div>
    </header>

    <main class="page-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { getMe, getStats } from '@/api'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const doctor = ref({})
const scrolled = ref(false)
const summaryVisible = ref(false)
const summaryLoaded = ref(false)
const summary = ref({ patientCount: 0, openSessionCount: 0, unreadCount: 0 })
const nav = [
  { name: '工作台', path: '/dashboard' },
  { name: '患者列表', path: '/patients' },
  { name: '咨询会话', path: '/sessions' },
  { name: '个人资料', path: '/profile' }
]
const displayName = computed(() => doctor.value.name || userStore.userInfo.nickname || userStore.userInfo.username || '医生')
const initial = computed(() => displayName.value.charAt(0))
const userAvatar = computed(() => doctor.value.avatar || userStore.userInfo.avatar || '')
const currentDate = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const onScroll = () => { scrolled.value = window.scrollY > 60 }

onMounted(async () => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
  const res = await getMe()
  doctor.value = res.data || {}
  userStore.updateUserInfo({ nickname: doctor.value.name, avatar: doctor.value.avatar })
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

async function showSummary() {
  summaryVisible.value = true
  if (summaryLoaded.value) return
  try {
    summary.value = (await getStats()).data || summary.value
    summaryLoaded.value = true
  } catch {
    summaryLoaded.value = true
  }
}

function onCommand(command) {
  summaryVisible.value = false
  if (command === 'profile') router.push('/profile')
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.app { min-height: 100dvh; display: flex; flex-direction: column; }
.topnav {
  position: sticky; top: 0; z-index: 100;
  border-bottom: 1px solid rgba(28,63,120,.08); background: #fff;
  box-shadow: 0 2px 10px rgba(28,63,120,.06);
  transition: background-color .5s var(--ease-out), box-shadow .5s var(--ease-out);
}
.topnav.scrolled { border-bottom-color: rgba(255,255,255,.65); background: rgba(255,255,255,.76); box-shadow: 0 4px 16px rgba(64,128,255,.08); backdrop-filter: blur(16px); }
.nav-inner { width: 100%; max-width: 1440px; height: 64px; margin: 0 auto; padding: 0 28px; display: flex; align-items: center; gap: 36px; transition: height .5s var(--ease-out); }
.scrolled .nav-inner { height: 54px; }
.logo { flex: 0 0 auto; cursor: pointer; }
.nav-items { flex: 1; height: 100%; display: flex; align-items: center; gap: 4px; }
.nav-link { position: relative; height: 100%; padding: 0 18px; display: flex; align-items: center; color: #333; text-decoration: none; font-size: 16px; transition: color .2s; }
.nav-link:hover { color: var(--el-color-primary); }
.nav-link.router-link-active { color: var(--el-color-primary); font-weight: 700; }
.nav-link.router-link-active::after { content: ""; position: absolute; right: 12px; bottom: 0; left: 12px; height: 3px; background: var(--el-color-primary); animation: underline-in .35s var(--ease-out); }
@keyframes underline-in { from { opacity: 0; transform: scaleX(.25); } }
.user-wrap { position: relative; flex: 0 0 auto; }
.user { min-height: 48px; padding: 6px 10px; display: flex; align-items: center; gap: 9px; cursor: pointer; transition: background-color .2s; }
.user:hover { background: var(--el-color-primary-light-9); }
.avatar { color: #fff; background: linear-gradient(135deg, #3E86EC, #2E6FE0); font-weight: 700; }
.user-name { max-width: 100px; overflow: hidden; color: var(--hda-ink); font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.chevron { color: #8494A7; font-size: 13px; transition: transform .25s var(--ease-out); }
.user:hover .chevron { transform: translateY(2px); }
.summary-pop { position: absolute; top: calc(100% + 8px); right: 0; z-index: 200; width: 268px; padding: 17px; border: var(--hda-glass-border); background: rgba(255,255,255,.86); box-shadow: var(--hda-shadow); backdrop-filter: blur(16px); }
.summary-title { margin: 0; color: var(--hda-ink); font-size: 16px; font-weight: 700; }
.summary-date { margin: 2px 0 12px; color: #93A2B5; font-size: 13px; }
.summary-row { padding: 7px 0; display: flex; justify-content: space-between; color: var(--hda-ink-soft); font-size: 14px; }
.summary-row + .summary-row { border-top: 1px solid rgba(223,231,242,.75); }
.summary-row b { color: var(--hda-ink); font-size: 15px; font-variant-numeric: tabular-nums; }
.summary-row b.warn { color: var(--el-color-danger); }
.summary-pop :deep(.el-skeleton__item) { border-radius: 0; }
.summary-pop :deep(.el-skeleton__p) { margin-top: 10px; }
.summary-pop-enter-active, .summary-pop-leave-active { transition: opacity .3s var(--ease-out), transform .3s var(--ease-out); }
.summary-pop-enter-from, .summary-pop-leave-to { opacity: 0; transform: translateY(-8px); }
@media (max-width: 900px) {
  .nav-inner { gap: 14px; padding: 0 14px; }
  .nav-items { min-width: 0; overflow-x: auto; scrollbar-width: none; }
  .nav-items::-webkit-scrollbar { display: none; }
  .nav-link { flex: 0 0 auto; padding: 0 12px; font-size: 15px; }
  .user-name, .chevron { display: none; }
}
@media (max-width: 620px) {
  .nav-inner { height: auto; min-height: 62px; flex-wrap: wrap; padding-top: 8px; }
  .nav-items { order: 3; flex-basis: 100%; height: 44px; }
  .nav-link { height: 44px; }
  .user-wrap { margin-left: auto; }
}
</style>

<style>
.doctor-dropdown.el-dropdown-menu { padding: 4px 0; border-radius: 0; }
.doctor-dropdown .el-dropdown-menu__item { padding: 10px 22px; border-radius: 0; font-size: 15px; }
</style>
