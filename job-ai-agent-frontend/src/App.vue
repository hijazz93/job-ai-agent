<template>
  <div class="app-layout">
    <div class="layout-sidebar">
      <Sidebar
        :is-open="sidebarOpen"
        :chats="allChats"
        :active-id="activeChatId"
        :is-authenticated="isAuthenticated"
        :username="username"
        :user-role="userRole"
        @close="sidebarOpen = false"
        @new-chat="handleNewChat"
        @select-chat="handleSelectChat"
        @delete-chat="handleDeleteChat"
        @tools-change="handleToolsChange"
      />
    </div>
    <div class="layout-main">
      <TopBar
        :is-dark="isDark"
        :is-authenticated="isAuthenticated"
        :is-admin="isAdmin"
        :username="username"
        :user-role="userRole"
        @toggle-sidebar="sidebarOpen = !sidebarOpen"
        @toggle-theme="toggleTheme"
        @logout="handleLogout"
      />
      <main class="layout-content">
        <router-view
          :sidebar-open="sidebarOpen"
          @toggle-sidebar="sidebarOpen = !sidebarOpen"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, provide, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import TopBar from './components/TopBar.vue'
import { useTheme } from './composables/useTheme'
import { useChatHistory } from './composables/useChatHistory'
import { useAuth } from './composables/useAuth'
import { createSession, deleteSession, getSessions } from './api/chatHistory'

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()
const { chats: allChats, createChat, deleteChat, getChat, clearAllChats } = useChatHistory()
const {
  isAuthenticated, isAdmin, userRole, username, fetchProfile, logout, user
} = useAuth()

const sidebarOpen = ref(window.innerWidth >= 769)
const currentChatId = ref('')
const activeChatId = ref('')
const selectedTools = ref([])

provide('currentChatId', currentChatId)
provide('selectedTools', selectedTools)
provide('isAuthenticated', isAuthenticated)
provide('userRole', userRole)
provide('username', username)

// 登录后自动加载远程会话
watch(isAuthenticated, (newVal, oldVal) => {
  if (newVal && !oldVal) {
    fetchProfile()
    loadServerSessions()
  }
})

// 从服务器加载会话列表
async function loadServerSessions() {
  if (!isAuthenticated.value) return
  try {
    const res = await getSessions()
    if (res.data.success && res.data.data) {
      for (const session of res.data.data) {
        const exists = allChats.value.find(c => c.id === session.id)
        if (!exists) {
          allChats.value.push({
            id: session.id,
            title: session.title || '新对话',
            createdAt: new Date(session.createdAt).getTime(),
            updatedAt: new Date(session.updatedAt).getTime(),
            messages: [],
            isServer: true
          })
        }
      }
    }
  } catch (e) {
    console.error('加载远程会话失败:', e)
  }
}

async function handleNewChat() {
  let serverId = ''
  if (isAuthenticated.value) {
    try {
      const res = await createSession('', '新对话')
      if (res.data.success && res.data.data) {
        serverId = res.data.data.id
      }
    } catch (e) {
      console.error('创建远程会话失败:', e)
    }
  }

  const chat = createChat()
  if (serverId) {
    chat.id = serverId
    chat.isServer = true
  }
  currentChatId.value = chat.id
  activeChatId.value = chat.id
  if (route.path !== '/') {
    await router.push('/')
  }
  window.dispatchEvent(new CustomEvent('chat-selected', { detail: { chatId: chat.id, messages: [] } }))
}

async function handleSelectChat(chat) {
  currentChatId.value = chat.id
  activeChatId.value = chat.id
  if (route.path !== '/') {
    await router.push('/')
  }
  window.dispatchEvent(new CustomEvent('chat-selected', { detail: { chatId: chat.id, messages: chat.messages } }))
}

async function handleDeleteChat(chatId) {
  const chat = getChat(chatId)
  if (chat?.isServer) {
    try {
      await deleteSession(chatId)
    } catch (e) {
      console.error('删除远程会话失败:', e)
    }
  }
  deleteChat(chatId)
  if (currentChatId.value === chatId) {
    currentChatId.value = ''
    activeChatId.value = ''
  }
}

function handleLogout() {
  clearAllChats()
  logout()
}

function handleToolsChange(tools) {
  selectedTools.value = tools
}

function handleToolsChangedEvent(e) {
  selectedTools.value = e.detail.tools
}

onMounted(async () => {
  window.addEventListener('tools-changed', handleToolsChangedEvent)
  window.addEventListener('chat-selected', (e) => {
    activeChatId.value = e.detail.chatId
    currentChatId.value = e.detail.chatId
  })
  // 已登录用户：拉取最新个人资料并加载远程会话
  if (isAuthenticated.value) {
    await fetchProfile()
    await loadServerSessions()
  }
})

onUnmounted(() => {
  window.removeEventListener('tools-changed', handleToolsChangedEvent)
})
</script>

<style scoped>
.app-layout {
  display: flex;
  width: 100%;
  height: 100vh;
}

.layout-sidebar {
  flex-shrink: 0;
  display: none;
}

.layout-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100vh;
}

.layout-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  transition: background var(--transition-normal);
}

@media (min-width: 769px) {
  .layout-sidebar {
    display: block;
  }
}
</style>
