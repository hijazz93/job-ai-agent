import { createRouter, createWebHistory } from 'vue-router'
import ChatPage from '../views/ChatPage.vue'
import LoginPage from '../views/LoginPage.vue'
import RegisterPage from '../views/RegisterPage.vue'
import UserCenterPage from '../views/UserCenterPage.vue'
import AdminDashboard from '../views/AdminDashboard.vue'

const routes = [
  {
    path: '/login',
    component: LoginPage,
    meta: { guest: true }
  },
  {
    path: '/register',
    component: RegisterPage,
    meta: { guest: true }
  },
  {
    path: '/',
    component: ChatPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/user/center',
    component: UserCenterPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/dashboard',
    component: AdminDashboard,
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('job-ai-auth-token')
  const user = JSON.parse(localStorage.getItem('job-ai-auth-user') || 'null')
  const isAuthenticated = !!token
  const isAdmin = user?.role === 'ADMIN'

  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  } else if (to.meta.guest && isAuthenticated) {
    next('/')
  } else if (to.meta.requiresAdmin && !isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
