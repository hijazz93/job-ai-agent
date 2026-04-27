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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobManus } from '../api/chat'

const messages = ref([])
const isStreaming = ref(false)
let currentStream = null

function handleSend(text) {
  if (isStreaming.value) return

  messages.value.push({ role: 'user', content: text })
  isStreaming.value = true
  const processSteps = []

  const aiMessage = reactive({ role: 'assistant', content: '', processSteps: [] })
  messages.value.push(aiMessage)

  currentStream = chatWithJobManus(text, {
    onStep: (stepData) => {
      processSteps.push(stepData)
    },
    onAnswer: (answerText) => {
      aiMessage.content = answerText
      aiMessage.processSteps = [...processSteps]
    },
    onError: (error) => {
      console.error('SSE Error:', error)
      aiMessage.content = '抱歉，发生了错误，请稍后重试。'
      aiMessage.processSteps = processSteps
      isStreaming.value = false
    },
    onComplete: () => {
      if (!aiMessage.content) {
        aiMessage.content = '任务执行完成。'
      }
      aiMessage.processSteps = processSteps
      isStreaming.value = false
      currentStream = null
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
  if (currentStream) {
    if (currentStream.abort) currentStream.abort()
    else if (currentStream.close) currentStream.close()
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
