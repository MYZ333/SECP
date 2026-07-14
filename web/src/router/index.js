import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const patientMeta = { roles: ['PATIENT'] }
const adminMeta = { roles: ['ADMIN'] }

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: to => {
      const userStore = useUserStore()
      return userStore.isAdmin && !userStore.isPatient ? '/admin' : '/dashboard'
    },
    children: [
      { path: 'dashboard', name: '首页', component: () => import('@/views/Dashboard.vue'), meta: patientMeta },
      { path: 'health', name: '健康档案总览', component: () => import('@/views/health/Overview.vue'), meta: patientMeta },
      { path: 'health/profile', name: '健康档案', component: () => import('@/views/health/Profile.vue'), meta: patientMeta },
      { path: 'health/metric', name: '体征数据', component: () => import('@/views/health/Metric.vue'), meta: patientMeta },
      { path: 'health/medical', name: '就诊记录', component: () => import('@/views/health/Medical.vue'), meta: patientMeta },
      { path: 'health/report', name: '健康报告', component: () => import('@/views/health/Report.vue'), meta: patientMeta },
      { path: 'point', name: '积分中心总览', component: () => import('@/views/point/Overview.vue'), meta: patientMeta },
      { path: 'point/mall', name: '积分商城', component: () => import('@/views/point/Mall.vue'), meta: patientMeta },
      { path: 'point/record', name: '积分明细', component: () => import('@/views/point/Record.vue'), meta: patientMeta },
      { path: 'doctor', name: '医生专家库', component: () => import('@/views/Doctor.vue'), meta: patientMeta },
      { path: 'consult', name: '健康助手', component: () => import('@/views/Consult.vue'), meta: patientMeta },
      { path: 'doctor-consult', name: '医生咨询', component: () => import('@/views/DoctorConsult.vue'), meta: patientMeta },
      { path: 'alert', name: '健康预警', component: () => import('@/views/Alert.vue'), meta: patientMeta },
      { path: 'account', name: '账户管理', component: () => import('@/views/account/Account.vue') },
      { path: 'admin', name: '系统管理总览', component: () => import('@/views/admin/Overview.vue'), meta: adminMeta },
      { path: 'admin/user', name: '用户管理', component: () => import('@/views/admin/UserManage.vue'), meta: adminMeta },
      { path: 'admin/product', name: '商品管理', component: () => import('@/views/admin/ProductManage.vue'), meta: adminMeta },
      { path: 'admin/doctor', name: '专家管理', component: () => import('@/views/admin/DoctorManage.vue'), meta: adminMeta },
      { path: 'admin/knowledge', name: '健康知识库', component: () => import('@/views/admin/KnowledgeManage.vue'), meta: adminMeta }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) return next()
  if (!userStore.isLogin) return next('/login')

  const requiredRoles = to.meta.roles
  if (requiredRoles?.length) {
    const allowed = requiredRoles.some(role => userStore.roles.includes(role))
    if (!allowed) return next(firstAllowedPath(userStore))
  }
  next()
})

function firstAllowedPath(userStore) {
  if (userStore.isAdmin) return '/admin'
  if (userStore.isPatient) return '/dashboard'
  return '/account'
}

export default router
