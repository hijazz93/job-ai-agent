<template>
  <div class="job-manus-page">
    <ChatContainer
      :messages="messages"
      :is-streaming="isStreaming"
      @send-message="handleSend"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobManus } from '../api/chat'

const messages = ref([])
const isStreaming = ref(false)
let currentEventSource = null

function handleSend(text) {
  if (isStreaming.value) return

  messages.value.push({ role: 'user', content: text })
  isStreaming.value = true
  let stepCount = 0

  currentEventSource = chatWithJobManus(text, {
    onMessage: (chunk) => {
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
      messages.value.push({
        role: 'assistant',
        content: '抱歉，发生了错误，请稍后重试。'
      })
      isStreaming.value = false
    },
    onComplete: () => {
      isStreaming.value = false
      currentEventSource = null
    }
  })
}

function handleChatSelected(e) {
  const { messages: historyMessages } = e.detail
  if (historyMessages && historyMessages.length > 0) {
    messages.value = [...historyMessages]
  } else {
    messages.value = []
  }
}

onMounted(() => {
  window.addEventListener('chat-selected', handleChatSelected)
})

onUnmounted(() => {
  window.removeEventListener('chat-selected', handleChatSelected)
  if (currentEventSource) {
    currentEventSource.close()
  }
})
</script>

<style scoped>
.job-manus-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}
</style>
