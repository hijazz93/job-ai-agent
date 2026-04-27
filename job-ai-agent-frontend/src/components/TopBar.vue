<template>
  <header class="topbar">
    <button class="menu-toggle" @click="$emit('toggle-sidebar')" aria-label="Toggle sidebar">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="24" height="24">
        <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z"/>
      </svg>
    </button>
    <div class="topbar-title">
      <span class="brand-name">{{ routeName }}</span>
    </div>
    <div class="topbar-actions">
      <button class="action-btn theme-toggle" @click="$emit('toggle-theme')" :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
        <svg v-if="isDark" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0-.39.39-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0 .39-.39.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41-.39-.39-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9c0-.46-.04-.92-.1-1.36-.98 1.37-2.58 2.26-4.4 2.26-3.03 0-5.5-2.47-5.5-5.5 0-1.82.89-3.42 2.26-4.4-.44-.06-.9-.1-1.36-.1z"/>
        </svg>
      </button>

      <div v-if="isAuthenticated" class="user-menu" ref="menuRef">
        <button class="user-avatar-btn" @click="menuOpen = !menuOpen">
          <span class="user-avatar">{{ displayChar }}</span>
        </button>
        <div v-if="menuOpen" class="user-dropdown">
          <div class="dropdown-header">
            <span class="dropdown-username">{{ username }}</span>
            <span class="dropdown-role">{{ roleLabel }}</span>
          </div>
          <div class="dropdown-divider"></div>
          <button class="dropdown-item" @click="navigate('/user/center')">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
            </svg>
            <span>用户中心</span>
          </button>
          <button v-if="isAdmin" class="dropdown-item" @click="navigate('/admin/dashboard')">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/>
            </svg>
            <span>管理面板</span>
          </button>
          <div class="dropdown-divider"></div>
          <button class="dropdown-item dropdown-item-danger" @click="handleLogout">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z"/>
            </svg>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  isDark: { type: Boolean, default: true },
  isAuthenticated: { type: Boolean, default: false },
  isAdmin: { type: Boolean, default: false },
  username: { type: String, default: '' },
  userRole: { type: String, default: '' }
})

const emit = defineEmits(['toggle-sidebar', 'toggle-theme', 'logout', 'navigate'])

const router = useRouter()
const menuOpen = ref(false)
const menuRef = ref(null)

const routeName = 'AI 就业助手'

const displayChar = computed(() => {
  return props.username ? props.username.charAt(0).toUpperCase() : 'U'
})

const roleLabel = computed(() => {
  switch (props.userRole) {
    case 'ADMIN': return '管理员'
    case 'PREMIUM_USER': return '高级用户'
    default: return '普通用户'
  }
})

function navigate(path) {
  menuOpen.value = false
  router.push(path)
}

function handleLogout() {
  menuOpen.value = false
  emit('logout')
}

function handleClickOutside(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) {
    menuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.topbar {
  height: var(--topbar-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-primary);
  flex-shrink: 0;
  gap: 12px;
  transition: background var(--transition-normal), border-color var(--transition-normal);
}

.menu-toggle {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}

.menu-toggle:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.topbar-title {
  flex: 1;
  min-width: 0;
}

.brand-name {
  font-size: 18px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.action-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.action-btn:active {
  transform: scale(0.92);
}

.user-menu {
  position: relative;
}

.user-avatar-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity var(--transition-fast), transform var(--transition-fast);
  border: 2px solid transparent;
}

.user-avatar-btn:hover {
  opacity: 0.85;
  transform: scale(1.05);
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 200px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 200;
  overflow: hidden;
}

.dropdown-header {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dropdown-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.dropdown-role {
  font-size: 12px;
  color: var(--text-tertiary);
}

.dropdown-divider {
  height: 1px;
  background: var(--border-primary);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 16px;
  font-size: 14px;
  color: var(--text-primary);
  transition: background var(--transition-fast);
  cursor: pointer;
  border: none;
  background: transparent;
  text-align: left;
}

.dropdown-item svg {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.dropdown-item:hover {
  background: var(--bg-hover);
}

.dropdown-item-danger {
  color: #ea4335;
}

.dropdown-item-danger svg {
  color: #ea4335;
}

.dropdown-item-danger:hover {
  background: rgba(234, 67, 53, 0.08);
}

@media (max-width: 768px) {
  .topbar {
    padding: 0 12px;
  }
  .brand-name {
    font-size: 16px;
  }
}
</style>
