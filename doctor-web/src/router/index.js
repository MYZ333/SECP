import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/DoctorLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: '工作台', component: () => import('@/views/Dashboard.vue') },
      { path: 'patients', name: '患者列表', component: () => import('@/views/Patients.vue') },
      { path: 'patients/:id', name: '患者详情', component: () => import('@/views/PatientDetail.vue') },
      { path: 'sessions', name: '咨询会话', component: () => import('@/views/Sessions.vue') },
      { path: 'profile', name: '个人资料', component: () => import('@/views/Profile.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.public) return next()
  if (!userStore.isLogin) return next('/login')
  if (!userStore.isDoctor) return next('/login')
  next()
})

export default router
