import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '首页', component: () => import('@/views/Dashboard.vue') },
      { path: 'health', name: '健康档案总览', component: () => import('@/views/health/Overview.vue') },
      { path: 'health/profile', name: '健康档案', component: () => import('@/views/health/Profile.vue') },
      { path: 'health/metric', name: '体征数据', component: () => import('@/views/health/Metric.vue') },
      { path: 'health/medical', name: '就诊记录', component: () => import('@/views/health/Medical.vue') },
      { path: 'health/report', name: '健康报告', component: () => import('@/views/health/Report.vue') },
      { path: 'point', name: '积分中心总览', component: () => import('@/views/point/Overview.vue') },
      { path: 'point/mall', name: '积分商城', component: () => import('@/views/point/Mall.vue') },
      { path: 'point/record', name: '积分明细', component: () => import('@/views/point/Record.vue') },
      { path: 'doctor', name: '医生专家库', component: () => import('@/views/Doctor.vue') },
      { path: 'consult', name: '健康助手', component: () => import('@/views/Consult.vue') },
      { path: 'doctor-consult', name: '医生咨询', component: () => import('@/views/DoctorConsult.vue') },
      { path: 'alert', name: '健康预警', component: () => import('@/views/Alert.vue') },
      { path: 'account', name: '账户管理', component: () => import('@/views/account/Account.vue') },
      { path: 'admin', name: '系统管理总览', component: () => import('@/views/admin/Overview.vue'), meta: { admin: true } },
      { path: 'admin/user', name: '用户管理', component: () => import('@/views/admin/UserManage.vue'), meta: { admin: true } },
      { path: 'admin/product', name: '商品管理', component: () => import('@/views/admin/ProductManage.vue'), meta: { admin: true } },
      { path: 'admin/doctor', name: '专家管理', component: () => import('@/views/admin/DoctorManage.vue'), meta: { admin: true } },
      { path: 'admin/knowledge', name: '健康知识库', component: () => import('@/views/admin/KnowledgeManage.vue'), meta: { admin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) return next()
  if (!userStore.isLogin) return next('/login')
  if (to.meta.admin && !userStore.isAdmin) return next('/dashboard')
  next()
})

export default router
