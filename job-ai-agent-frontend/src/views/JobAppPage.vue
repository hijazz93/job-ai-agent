<template>
  <div class="job-app-page">
    <div class="page-header">
      <h1>AI 就业助手</h1>
      <p>专业的求职问答助手 - 解答就业相关问题</p>
    </div>
    <ChatContainer
      :messages="messages"
      :is-streaming="isStreaming"
      @send-message="handleSend"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobAppSSE } from '../api/chat'

const messages = ref([])
const isStreaming = ref(false)
const chatId = ref('job-app-' + Date.now())
let currentEventSource = null

const handleSend = (text) => {
  if (isStreaming.value) return

  messages.value.push({
    role: 'user',
    content: text
  })

  let aiContent = ''
  isStreaming.value = true

  messages.value.push({
    role: 'assistant',
    content: ''
  })

  currentEventSource = chatWithJobAppSSE(text, chatId.value, {
    onMessage: (chunk, full) => {
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
</script>

<style scoped>
.job-app-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.page-header {
  padding: 24px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(20, 20, 35, 0.5);
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}
</style>
