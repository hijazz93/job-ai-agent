<template>
  <div class="job-manus-page">
    <div class="page-header">
      <h1>JobManus</h1>
      <p>智能规划助手 - 帮你规划求职路径</p>
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
import { chatWithJobManus } from '../api/chat'

const messages = ref([])
const isStreaming = ref(false)
let currentEventSource = null

const handleSend = (text) => {
  if (isStreaming.value) return

  messages.value.push({
    role: 'user',
    content: text
  })

  isStreaming.value = true

  let stepCount = 0

  currentEventSource = chatWithJobManus(text, {
    onMessage: (chunk, full) => {
      stepCount++
      messages.value.push({
        role: 'assistant',
        content: chunk,
        step: stepCount,
        isStep: true
      })
    },
    onError: (error) => {
      console.error('SSE Error:', error)
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
.job-manus-page {
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
