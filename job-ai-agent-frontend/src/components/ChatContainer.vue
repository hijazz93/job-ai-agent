<template>
  <div class="chat-container" ref="containerRef">
    <div class="messages-wrapper">
      <div v-if="messages.length === 0" class="welcome-screen">
        <div class="welcome-greeting">
          <h1 class="welcome-title">{{ welcomeTitle }}</h1>
        </div>

        <div class="suggestion-grid">
          <div
            v-for="(suggestion, index) in suggestions"
            :key="index"
            class="suggestion-card"
            @click="$emit('send-message', suggestion.text)"
          >
            <div class="suggestion-text">{{ suggestion.text }}</div>
            <div class="suggestion-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <ChatMessage
        v-for="(message, index) in messages"
        :key="index"
        :message="message"
      />

      <div v-if="isStreaming && hasAiMessage" class="typing-indicator">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>

    <div class="input-area">
      <FileAttachmentList
        :files="sessionFiles"
        @remove="$emit('remove-file', $event)"
      />
      <div class="input-disclaimer" v-if="messages.length > 0">
        AI 生成的内容仅供参考，请验证重要信息
      </div>
      <ChatInput
        @send="$emit('send-message', $event)"
        :disabled="isStreaming"
        :current-mode="currentMode"
        :is-mode-dropdown-open="isModeDropdownOpen"
        :selected-tools="selectedTools"
        :allowed-modes="allowedModes"
        :rag-enabled="ragEnabled"
        @toggle-mode-dropdown="$emit('toggle-mode-dropdown')"
        @set-mode="$emit('set-mode', $event)"
        @tools-change="$emit('tools-change', $event)"
        @toggle-rag="$emit('toggle-rag')"
        @file-parsed="$emit('file-parsed', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import ChatMessage from './ChatMessage.vue'
import ChatInput from './ChatInput.vue'
import FileAttachmentList from './FileAttachmentList.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  isStreaming: { type: Boolean, default: false },
  currentMode: { type: String, default: 'fast' },
  isModeDropdownOpen: { type: Boolean, default: false },
  selectedTools: { type: Array, default: () => [] },
  allowedModes: { type: Array, default: () => ['fast', 'agent'] },
  ragEnabled: { type: Boolean, default: false },
  sessionFiles: { type: Array, default: () => [] }
})

defineEmits(['send-message', 'toggle-mode-dropdown', 'set-mode', 'tools-change', 'toggle-rag', 'file-parsed', 'remove-file'])

const containerRef = ref(null)
const scrollIntervalRef = ref(null)

const hasAiMessage = computed(() => {
  return props.messages.length > 0 && props.messages[props.messages.length - 1].role === 'assistant'
})

const welcomeTitle = computed(() => {
  return '你好，我是 AI 就业助手'
})

const suggestions = [
  { text: '软件工程应届生如何选择岗位方向？' },
  { text: '校招面试中常见的技术问题有哪些？' },
  { text: '如何准备一份合格的技术简历？' },
  { text: '应届生第一份工作应该怎么选？' },
  { text: '帮我规划软件工程校招求职时间线' },
  { text: '分析2026年互联网行业就业趋势' },
  { text: '帮我生成一份技术岗位简历模板' },
  { text: '搜索最近的IT行业招聘会信息' },
]

const scrollToBottom = () => {
  nextTick(() => {
    if (containerRef.value) {
      containerRef.value.scrollTop = containerRef.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.isStreaming, (streaming) => {
  if (streaming) {
    scrollIntervalRef.value = setInterval(() => {
      if (containerRef.value) {
        containerRef.value.scrollTop = containerRef.value.scrollHeight
      }
    }, 100)
  } else {
    if (scrollIntervalRef.value) {
      clearInterval(scrollIntervalRef.value)
      scrollIntervalRef.value = null
    }
  }
})

onUnmounted(() => {
  if (scrollIntervalRef.value) {
    clearInterval(scrollIntervalRef.value)
    scrollIntervalRef.value = null
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
}

.messages-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 0;
  scroll-behavior: smooth;
}

.welcome-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  min-height: 100%;
}

.welcome-greeting {
  text-align: center;
  margin-bottom: 48px;
}

.welcome-title {
  font-size: 36px;
  font-weight: 400;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.3;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  max-width: 740px;
  width: 100%;
}

.suggestion-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.suggestion-card:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
  transform: translateY(-1px);
}

.suggestion-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
  flex: 1;
  margin-right: 12px;
}

.suggestion-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--bg-active);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.input-area {
  padding: 0 16px 16px;
  flex-shrink: 0;
}

.input-disclaimer {
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
  padding: 8px 0;
}

.input-extra {
  display: flex;
  justify-content: center;
  padding: 0 0 8px;
}

.typing-indicator {
  display: flex;
  justify-content: center;
  gap: 5px;
  padding: 24px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: var(--text-tertiary);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

@media (max-width: 768px) {
  .welcome-title {
    font-size: 28px;
  }
  .suggestion-grid {
    grid-template-columns: 1fr;
  }
  .suggestion-card {
    padding: 12px 16px;
  }
}
</style>
