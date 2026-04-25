<template>
  <div class="chat-page">
    <ChatContainer
      :messages="messages"
      :is-streaming="isStreaming"
      @send-message="handleSend"
      :current-mode="currentMode"
      @toggle-mode-dropdown="toggleModeDropdown"
      :is-mode-dropdown-open="isModeDropdownOpen"
      @set-mode="setMode"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobAppSSE } from '../api/chat'
import { chatWithJobManus } from '../api/chat'
import { useChatHistory } from '../composables/useChatHistory'

const { chats, createChat, addMessage, getChat } = useChatHistory()

const messages = ref([])
const isStreaming = ref(false)
const currentMode = ref('fast') // 默认模式为 fast
const isModeDropdownOpen = ref(false)
const chatId = ref('')
let currentEventSource = null

// 创建新的聊天
function createNewChat() {
  const newChat = createChat()
  chatId.value = newChat.id
  console.log('创建新聊天:', chatId.value)
  return newChat
}

function toggleModeDropdown() {
  isModeDropdownOpen.value = !isModeDropdownOpen.value
}

function setMode(mode) {
  if (mode !== currentMode.value) {
    currentMode.value = mode
    // 清空当前对话，准备新的模式
    messages.value = []
  }
  isModeDropdownOpen.value = false
}

function handleSend(text) {
  if (isStreaming.value) return

  // 如果还没有聊天ID，创建一个新的聊天
  if (!chatId.value) {
    createNewChat()
  }

  const userMessage = { role: 'user', content: text }
  messages.value.push(userMessage)
  // 添加用户消息到聊天历史
  addMessage(chatId.value, userMessage)
  console.log('添加用户消息到聊天历史:', chatId.value, userMessage)
  
  isStreaming.value = true

  if (currentMode.value === 'fast') {
    // Fast 模式 - JobApp
    let aiContent = ''
    messages.value.push({ role: 'assistant', content: '' })

    currentEventSource = chatWithJobAppSSE(text, chatId.value, {
      onMessage: (_chunk, full) => {
        aiContent = full
        const lastMessage = messages.value[messages.value.length - 1]
        lastMessage.content = full
      },
      onError: (error) => {
        console.error('SSE Error:', error)
        const lastMessage = messages.value[messages.value.length - 1]
        lastMessage.content = '抱歉，发生了错误，请稍后重试。'
        // 添加错误消息到聊天历史
        addMessage(chatId.value, lastMessage)
        console.log('添加错误消息到聊天历史:', chatId.value, lastMessage)
        isStreaming.value = false
      },
      onComplete: () => {
        const lastMessage = messages.value[messages.value.length - 1]
        // 添加AI回复到聊天历史
        addMessage(chatId.value, lastMessage)
        console.log('添加AI回复到聊天历史:', chatId.value, lastMessage)
        isStreaming.value = false
        currentEventSource = null
      }
    })
  } else {
    // Agent 模式 - JobManus
    let stepCount = 0

    currentEventSource = chatWithJobManus(text, {
      onMessage: (chunk) => {
        stepCount++
        const aiMessage = {
          role: 'assistant',
          content: chunk,
          step: stepCount,
          isStep: true
        }
        messages.value.push(aiMessage)
        // 添加AI步骤消息到聊天历史
        addMessage(chatId.value, aiMessage)
        console.log('添加AI步骤消息到聊天历史:', chatId.value, aiMessage)
      },
      onError: (error) => {
        console.error('SSE Error:', error)
        const errorMessage = {
          role: 'assistant',
          content: '抱歉，发生了错误，请稍后重试。'
        }
        messages.value.push(errorMessage)
        // 添加错误消息到聊天历史
        addMessage(chatId.value, errorMessage)
        console.log('添加错误消息到聊天历史:', chatId.value, errorMessage)
        isStreaming.value = false
      },
      onComplete: () => {
        isStreaming.value = false
        currentEventSource = null
      }
    })
  }
}

function handleChatSelected(e) {
  const { chatId: selectedChatId, messages: historyMessages } = e.detail
  console.log('选择聊天:', selectedChatId, historyMessages)
  if (selectedChatId) {
    chatId.value = selectedChatId
    if (historyMessages && historyMessages.length > 0) {
      messages.value = [...historyMessages]
    } else {
      messages.value = []
    }
  }
}

// 点击外部关闭下拉菜单
function handleClickOutside(event) {
  const dropdown = document.querySelector('.mode-dropdown')
  if (dropdown && !dropdown.contains(event.target)) {
    isModeDropdownOpen.value = false
  }
}

onMounted(() => {
  window.addEventListener('chat-selected', handleChatSelected)
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('chat-selected', handleChatSelected)
  document.removeEventListener('click', handleClickOutside)
  if (currentEventSource) {
    currentEventSource.close()
  }
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mode-selector {
  display: flex;
  justify-content: center;
  padding: 0 0 8px;
}

.mode-dropdown {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
  user-select: none;
  z-index: 100;
}

.mode-dropdown:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.mode-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.mode-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
  transition: transform var(--transition-fast);
}

.mode-dropdown.open .mode-arrow {
  transform: rotate(180deg);
}

.mode-options {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  min-width: 240px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  z-index: 101;
}

.mode-option {
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-option:hover {
  background: var(--bg-card-hover);
}

.mode-option.active {
  background: var(--bg-active);
  border-left: 3px solid var(--accent-primary);
}

.mode-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.mode-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.4;
}
</style>