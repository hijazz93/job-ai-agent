import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, getProfile as apiGetProfile } from '../api/auth'
import { apiClient } from '../api/chat'

const TOKEN_KEY = 'job-ai-auth-token'
const USER_KEY = 'job-ai-auth-user'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

const isAuthenticated = computed(() => !!token.value)
const isAdmin = computed(() => user.value?.role === 'ADMIN')
const isPremium = computed(() => user.value?.role === 'PREMIUM_USER' || user.value?.role === 'ADMIN')
const userRole = computed(() => user.value?.role || '')
const username = computed(() => user.value?.username || '')

function saveAuth(t, u) {
  token.value = t
  user.value = u
  localStorage.setItem(TOKEN_KEY, t)
  localStorage.setItem(USER_KEY, JSON.stringify(u))
  setupAuthHeader(t)
}

function clearAuth() {
  token.value = ''
  user.value = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  delete apiClient.defaults.headers.common['Authorization']
}

function setupAuthHeader(t) {
  if (t) {
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${t}`
  } else {
    delete apiClient.defaults.headers.common['Authorization']
  }
}

// 初始化时设置请求头
if (token.value) {
  setupAuthHeader(token.value)
}

// 响应拦截器：处理 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function useAuth() {
  async function login(username, password) {
    const response = await apiLogin(username, password)
    const data = response.data
    if (data.success) {
      saveAuth(data.data.token, data.data.user)
      return data.data
    }
    throw new Error(data.message || '登录失败')
  }

  async function register(username, email, password) {
    const response = await apiRegister(username, email, password)
    const data = response.data
    if (data.success) {
      saveAuth(data.data.token, data.data.user)
      return data.data
    }
    throw new Error(data.message || '注册失败')
  }

  function logout() {
    clearAuth()
    // 退出登录时清空本地聊天记录
    localStorage.removeItem('job-ai-chat-history')
    window.location.href = '/login'
  }

  async function fetchProfile() {
    try {
      const response = await apiGetProfile()
      const data = response.data
      if (data.success && data.data) {
        user.value = data.data
        localStorage.setItem(USER_KEY, JSON.stringify(data.data))
      }
    } catch (e) {
      console.error('Failed to fetch profile:', e)
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    isPremium,
    userRole,
    username,
    login,
    register,
    logout,
    fetchProfile
  }
}
