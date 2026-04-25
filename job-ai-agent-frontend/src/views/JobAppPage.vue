<template>
  <div class="job-app-page">
    <ChatContainer
      :messages="messages"
      :is-streaming="isStreaming"
      @send-message="handleSend"
    >
      <template #input-extra>
        <label class="agent-toggle" title="切换到 JobManus Agent 模式以使用工具调用功能">
          <input type="checkbox" :checked="useAgentMode" @change="toggleAgentMode" />
          <span class="toggle-text">使用Agent模式</span>
        </label>
      </template>
    </ChatContainer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobAppSSE } from '../api/chat'

const MESSAGE_STORE_KEY = 'job-ai-cached-messages'

const router = useRouter()
const messages = ref([])
const isStreaming = ref(false)
const useAgentMode = ref(false)
const chatId = ref('job-app-' + Date.now())
let currentEventSource = null

function toggleAgentMode() {
  useAgentMode.value = !useAgentMode.value
  if (useAgentMode.value) {
    sessionStorage.setItem(MESSAGE_STORE_KEY, JSON.stringify(messages.value))
    router.push('/manus')
  }
}

function handleSend(text) {
  if (isStreaming.value) return

  messages.value.push({ role: 'user', content: text })
  let aiContent = ''
  isStreaming.value = true

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
      isStreaming.value = false
    },
    onComplete: () => {
      isStreaming.value = false
      currentEventSource = null
    }
  })
}

function restoreMessages() {
  try {
    const cached = sessionStorage.getItem(MESSAGE_STORE_KEY)
    if (cached) {
      messages.value = JSON.parse(cached)
      sessionStorage.removeItem(MESSAGE_STORE_KEY)
    }
  } catch {}
}

onMounted(() => {
  restoreMessages()
})

onUnmounted(() => {
  if (currentEventSource) {
    currentEventSource.close()
  }
})
</script>

<style scoped>
.job-app-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.agent-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  transition: background var(--transition-fast), border-color var(--transition-fast);
  user-select: none;
}

.agent-toggle:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.agent-toggle input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--accent-primary);
  cursor: pointer;
}

.toggle-text {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
