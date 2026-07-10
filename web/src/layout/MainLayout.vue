<template>
  <el-container class="app">
    <el-aside width="248px" class="aside">
      <div class="logo">
        <div class="logo-badge"><el-icon :size="24"><FirstAidKit /></el-icon></div>
        <span>智慧医养</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="$route.path" router class="menu">
          <el-menu-item index="/dashboard"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
          <el-sub-menu index="health">
            <template #title><el-icon><Notebook /></el-icon><span>健康档案</span></template>
            <el-menu-item index="/health/profile">基本信息</el-menu-item>
            <el-menu-item index="/health/metric">体征数据</el-menu-item>
            <el-menu-item index="/health/medical">就诊记录</el-menu-item>
            <el-menu-item index="/health/report">健康报告</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="point">
            <template #title><el-icon><GoldMedal /></el-icon><span>积分中心</span></template>
            <el-menu-item index="/point/mall">积分商城</el-menu-item>
            <el-menu-item index="/point/record">积分明细</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/doctor"><el-icon><Avatar /></el-icon><span>医生专家库</span></el-menu-item>
          <el-menu-item index="/consult"><el-icon><ChatDotRound /></el-icon><span>健康咨询</span></el-menu-item>
          <el-menu-item index="/alert"><el-icon><BellFilled /></el-icon><span>健康预警</span></el-menu-item>
          <el-menu-item index="/account"><el-icon><Setting /></el-icon><span>账户管理</span></el-menu-item>
          <el-sub-menu index="admin" v-if="userStore.isAdmin">
            <template #title><el-icon><Management /></el-icon><span>系统管理</span></template>
            <el-menu-item index="/admin/user">用户管理</el-menu-item>
            <el-menu-item index="/admin/product">商品管理</el-menu-item>
            <el-menu-item index="/admin/doctor">专家管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="crumb">
          <span class="dot"></span>
          <h2>{{ $route.name }}</h2>
        </div>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-avatar :size="40" :src="userStore.userInfo.avatar" class="ava">
              {{ (userStore.userInfo.nickname || userStore.userInfo.username || 'U').charAt(0) }}
            </el-avatar>
            <span class="uname">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account" :icon="Setting">账户管理</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { FirstAidKit, HomeFilled, Notebook, GoldMedal, Avatar, ChatDotRound,
  BellFilled, Setting, Management, ArrowDown, SwitchButton } from '@element-plus/icons-vue'
const router = useRouter()
const userStore = useUserStore()
function onCommand(cmd) {
  if (cmd === 'logout') { userStore.logout(); router.push('/login') }
  if (cmd === 'account') router.push('/account')
}
</script>

<style scoped>
.app { height: 100vh; }

/* 侧边栏 */
.aside {
  background: linear-gradient(180deg, #21806A 0%, #1C6E5B 100%);
  display: flex; flex-direction: column;
  box-shadow: 4px 0 24px rgba(28,110,91,.18);
}
.logo {
  height: 76px; display: flex; align-items: center; gap: 12px;
  padding: 0 22px; color: #fff; font-size: 22px; font-weight: 700; letter-spacing: .04em;
}
.logo-badge {
  width: 42px; height: 42px; border-radius: 13px; display: grid; place-items: center;
  background: rgba(255,255,255,.18); animation: hdaFloat 5s ease-in-out infinite;
}
.menu-scroll { flex: 1; }
.menu {
  padding: 8px 12px;
  /* 关键：让内嵌子菜单也用透明背景、浅色文字，避免展开后一片白 */
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: transparent;
  --el-menu-text-color: rgba(255,255,255,.82);
  --el-menu-hover-text-color: #fff;
  --el-menu-active-color: #fff;
}
.menu, .menu :deep(.el-menu--inline) { background: transparent !important; }

/* 菜单项：暖色高亮 + 悬浮位移 */
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  color: rgba(255,255,255,.82); height: 52px; border-radius: 14px; margin: 4px 0;
  font-size: 17px; transition: all .25s cubic-bezier(.2,.8,.2,1);
  cursor: pointer; user-select: none;
}
.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255,255,255,.12); color: #fff; transform: translateX(4px);
}
.menu :deep(.el-menu-item.is-active) {
  background: #fff; color: var(--el-color-primary); font-weight: 700;
  box-shadow: 0 8px 20px rgba(0,0,0,.14);
}
.menu :deep(.el-sub-menu .el-menu-item) { background: transparent; min-width: auto; }
/* 子菜单选中项：与顶级菜单保持一致的白色圆角高亮 */
.menu :deep(.el-sub-menu .el-menu-item.is-active) {
  background: #fff !important; color: var(--el-color-primary) !important; font-weight: 700;
  box-shadow: 0 8px 20px rgba(0,0,0,.14);
}
.menu :deep(.el-sub-menu.is-active .el-sub-menu__title) { color: #fff; }
.menu :deep(.el-icon) { font-size: 20px; }

/* 顶栏 */
.header {
  height: 76px; display: flex; justify-content: space-between; align-items: center;
  background: rgba(255,255,255,.78); backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--hda-line); padding: 0 28px;
}
.crumb { display: flex; align-items: center; gap: 12px; }
.crumb .dot { width: 10px; height: 10px; border-radius: 50%; background: var(--hda-accent); box-shadow: 0 0 0 4px rgba(242,149,94,.2); }
.crumb h2 { margin: 0; font-size: 22px; font-weight: 700; color: var(--hda-ink); }
.user { display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 6px 10px; border-radius: 999px; transition: background .25s; }
.user:hover { background: var(--el-color-primary-light-9); }
.ava { background: var(--el-color-primary); color: #fff; font-weight: 700; }
.uname { font-size: 17px; font-weight: 600; color: var(--hda-ink); }

.el-main { position: relative; padding: 28px; background: var(--hda-bg); }
</style>
