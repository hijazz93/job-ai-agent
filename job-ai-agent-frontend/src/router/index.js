import { createRouter, createWebHistory } from 'vue-router'
import JobManusPage from '../views/JobManusPage.vue'
import JobAppPage from '../views/JobAppPage.vue'

const routes = [
  {
    path: '/',
    redirect: '/manus'
  },
  {
    path: '/manus',
    name: 'JobManus',
    component: JobManusPage
  },
  {
    path: '/job-app',
    name: 'JobApp',
    component: JobAppPage
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
