<template>
  <div class="app-layout">
    <div class="layout-sidebar">
      <Sidebar
        :is-open="sidebarOpen"
        :chats="allChats"
        :active-id="activeChatId"
        @close="sidebarOpen = false"
        @new-chat="handleNewChat"
        @select-chat="handleSelectChat"
        @delete-chat="handleDeleteChat"
      />
    </div>
    <div class="layout-main">
      <TopBar
        :is-dark="isDark"
        @toggle-sidebar="sidebarOpen = !sidebarOpen"
        @toggle-theme="toggleTheme"
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
import { ref, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import TopBar from './components/TopBar.vue'
import { useTheme } from './composables/useTheme'
import { useChatHistory } from './composables/useChatHistory'

const route = useRoute()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()
const { chats: allChats, createChat, deleteChat, getChat } = useChatHistory()

const sidebarOpen = ref(window.innerWidth >= 769)
const currentChatId = ref('')
const activeChatId = ref('')

provide('currentChatId', currentChatId)

function handleNewChat() {
  const chat = createChat()
  currentChatId.value = chat.id
  activeChatId.value = chat.id
  window.dispatchEvent(new CustomEvent('chat-selected', { detail: { chatId: chat.id, messages: [] } }))
}

function handleSelectChat(chat) {
  currentChatId.value = chat.id
  activeChatId.value = chat.id
  window.dispatchEvent(new CustomEvent('chat-selected', { detail: { chatId: chat.id, messages: chat.messages } }))
}

function handleDeleteChat(chatId) {
  deleteChat(chatId)
  if (currentChatId.value === chatId) {
    currentChatId.value = ''
    activeChatId.value = ''
  }
}
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
