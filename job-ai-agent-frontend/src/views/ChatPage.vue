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
      :selected-tools="selectedTools"
      @tools-change="handleToolsChange"
      :allowed-modes="allowedModes"
      :rag-enabled="ragEnabled"
      @toggle-rag="ragEnabled = !ragEnabled"
      @file-parsed="handleFileParsed"
      :session-files="sessionFiles"
      @remove-file="handleRemoveFile"
    />
  </div>
</template>

<script setup>
import { ref, reactive, inject, computed, onMounted, onUnmounted, watch } from 'vue'
import ChatContainer from '../components/ChatContainer.vue'
import { chatWithJobAppSSE, chatWithJobAppSSEWithRag } from '../api/chat'
import { chatWithJobManus } from '../api/chat'
import { useChatHistory } from '../composables/useChatHistory'
import { saveMessage, getSessionMessages } from '../api/chatHistory'
import { addSessionFile, getSessionFiles, removeSessionFile } from '../api/sessionFiles'

const isAuthenticated = inject('isAuthenticated', ref(false))
const userRole = inject('userRole', ref(''))

const allowedModes = computed(() => ['fast', 'agent'])

// 如果用户角色不允许 agent 模式，强制切换为 fast
watch(userRole, (role) => {
  if (role === 'NORMAL_USER' && currentMode.value === 'agent') {
    currentMode.value = 'fast'
  }
})

async function syncMessageToServer(msgChatId, msgRole, msgContent) {
  if (!isAuthenticated.value) return
  try {
    await saveMessage(msgChatId, msgRole, msgContent)
  } catch (e) {
    console.error('同步消息到服务器失败:', e)
  }
}

async function loadServerMessages(sessionId) {
  if (!isAuthenticated.value) return null
  try {
    const res = await getSessionMessages(sessionId)
    if (res.data.success && res.data.data) {
      return res.data.data.map(m => ({
        role: m.role,
        content: m.content
      }))
    }
  } catch (e) {
    console.error('加载远程消息失败:', e)
  }
  return null
}

const { chats, createChat, addMessage, getChat } = useChatHistory()

const SESSION_STORAGE_KEY = 'job-ai-active-chat-id'

const ragEnabled = ref(false)

const messages = ref([])
const isStreaming = ref(false)
const currentMode = ref('fast') // 默认模式为 fast
const isModeDropdownOpen = ref(false)
const chatId = ref('')
const selectedTools = inject('selectedTools') // 从 App.vue 注入的用户选择的工具列表
let currentStream = null // EventSource (fast) 或 AbortController (agent)

// 持久化当前聊天 ID 到 sessionStorage（刷新后可恢复）
watch(chatId, (newId) => {
  if (newId) {
    sessionStorage.setItem(SESSION_STORAGE_KEY, newId)
  }
})

// 收集工具执行结果
const toolResults = ref([])

// 当前会话关联的文件列表（持久化到后端，跨请求保持）
const sessionFiles = ref([])

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
  // 普通用户切换 Agent 模式时弹出提示并阻止
  if (mode === 'agent' && userRole.value === 'NORMAL_USER') {
    alert('该功能需要升级为高级用户才可使用')
    isModeDropdownOpen.value = false
    return
  }
  if (mode !== currentMode.value) {
    currentMode.value = mode
    // 清空当前对话，准备新的模式
    messages.value = []
    // 清空工具执行结果
    toolResults.value = []
  }
  isModeDropdownOpen.value = false
}

function handleSend(text) {
  if (isStreaming.value) return

  // 如果还没有聊天ID，创建一个新的聊天
  if (!chatId.value) {
    createNewChat()
  }

  // 清空工具执行结果
  toolResults.value = []

  const userMessage = { role: 'user', content: text }
  messages.value.push(userMessage)
  // 添加用户消息到聊天历史
  addMessage(chatId.value, userMessage)
  syncMessageToServer(chatId.value, 'user', text)
  console.log('添加用户消息到聊天历史:', chatId.value, userMessage)
  
  isStreaming.value = true

  if (currentMode.value === 'fast') {
    // Fast 模式 - JobApp（可选择开启 RAG 知识库检索）
    let aiContent = ''
    messages.value.push({ role: 'assistant', content: '' })

    const sseCallbacks = {
      onMessage: (_chunk, full) => {
        aiContent = full
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage) {
          lastMessage.content = full
        }
      },
      onError: (error) => {
        console.error('SSE Error:', error)
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage) {
          lastMessage.content = '抱歉，发生了错误，请稍后重试。'
          addMessage(chatId.value, lastMessage)
          syncMessageToServer(chatId.value, 'assistant', lastMessage.content)
        }
        isStreaming.value = false
      },
      onComplete: () => {
        const lastMessage = messages.value[messages.value.length - 1]
        if (lastMessage) {
          addMessage(chatId.value, lastMessage)
          syncMessageToServer(chatId.value, 'assistant', lastMessage.content)
        }
        isStreaming.value = false
        currentStream = null
      }
    }

    currentStream = ragEnabled.value
      ? chatWithJobAppSSEWithRag(text, chatId.value, sseCallbacks)
      : chatWithJobAppSSE(text, chatId.value, sseCallbacks)
  } else {
    // Agent 模式 - JobManus
    const processSteps = []

    // 文件上下文由后端自动注入 system prompt，前端只需传 sessionId
    const messageText = text

    // 预先创建一条 AI 消息占位，最终只显示这一条
    const aiMessage = reactive({ role: 'assistant', content: '', processSteps: [] })
    messages.value.push(aiMessage)

    currentStream = chatWithJobManus(messageText, {
      selectedTools: selectedTools.value,
      sessionId: chatId.value,
      onStep: (stepData) => {
        processSteps.push(stepData)
        if (!stepData.includes('思考') && !stepData.includes('观察')) {
          toolResults.value.push({
            step: processSteps.length,
            content: stepData
          })
        }
      },
      onAnswer: (answerText) => {
        aiMessage.content = answerText
        aiMessage.processSteps = [...processSteps]
      },
      onError: (error) => {
        console.error('SSE Error:', error)
        aiMessage.content = '抱歉，发生了错误，请稍后重试。'
        aiMessage.processSteps = processSteps
        addMessage(chatId.value, aiMessage)
        syncMessageToServer(chatId.value, 'assistant', aiMessage.content)
        isStreaming.value = false
      },
      onComplete: () => {
        if (!aiMessage.content) {
          aiMessage.content = '任务执行完成。'
        }
        aiMessage.processSteps = processSteps
        addMessage(chatId.value, aiMessage)
        syncMessageToServer(chatId.value, 'assistant', aiMessage.content)
        isStreaming.value = false
        currentStream = null
      }
    })
  }
}

async function handleChatSelected(e) {
  const { chatId: selectedChatId, messages: historyMessages } = e.detail
  if (selectedChatId) {
    chatId.value = selectedChatId
    sessionFiles.value = []
    if (isAuthenticated.value) {
      const serverMessages = await loadServerMessages(selectedChatId)
      // 防止快速切换会话时异步响应覆盖
      if (chatId.value !== selectedChatId) return
      if (serverMessages && serverMessages.length > 0) {
        messages.value = serverMessages
        loadSessionFilesForChat(selectedChatId)
        return
      }
      loadSessionFilesForChat(selectedChatId)
    }
    if (chatId.value !== selectedChatId) return
    if (historyMessages && historyMessages.length > 0) {
      messages.value = [...historyMessages]
    } else {
      messages.value = []
    }
  }
}

async function loadSessionFilesForChat(sessionId) {
  try {
    const res = await getSessionFiles(sessionId)
    if (res.data && res.data.success && res.data.data) {
      sessionFiles.value = res.data.data
    }
  } catch (e) {
    // 未登录或网络错误时忽略
  }
}

// 恢复上次活跃会话
function restoreLastSession() {
  const lastChatId = sessionStorage.getItem(SESSION_STORAGE_KEY)
  if (!lastChatId) return

  chatId.value = lastChatId
  // 先加载本地缓存的消息（即时显示）
  const chat = getChat(lastChatId)
  if (chat && chat.messages.length > 0) {
    messages.value = [...chat.messages]
  }
  // 已认证用户从服务器加载完整消息
  if (isAuthenticated.value) {
    loadServerMessages(lastChatId).then(serverMessages => {
      if (serverMessages && serverMessages.length > 0) {
        messages.value = serverMessages
      }
    })
    loadSessionFilesForChat(lastChatId)
  }
  // 通知 App.vue 更新侧边栏高亮
  window.dispatchEvent(new CustomEvent('chat-selected', {
    detail: { chatId: lastChatId, messages: chat?.messages || [] }
  }))
}

// 点击外部关闭下拉菜单
function handleClickOutside(event) {
  const dropdown = document.querySelector('.mode-dropdown')
  if (dropdown && !dropdown.contains(event.target)) {
    isModeDropdownOpen.value = false
  }
}

// 处理工具选择变化
function handleToolsChange(tools) {
  window.dispatchEvent(new CustomEvent('tools-changed', { detail: { tools } }))
}

// 处理文件解析结果 — 持久化到后端并在本地展示文件卡片
async function handleFileParsed(info) {
  const fileEntry = {
    originalFileName: info.fileName,
    uniqueFileName: info.fileName,
    fullPath: info.fullPath || '',
    content: info.content,
    charCount: info.charCount
  }

  // 优先添加到本地列表（即时响应）
  const localId = Date.now()
  sessionFiles.value.push({ ...fileEntry, id: localId, pending: true })

  // 已认证用户持久化到后端
  if (chatId.value) {
    try {
      const res = await addSessionFile(chatId.value, fileEntry)
      if (res.data && res.data.success) {
        const serverData = res.data.data
        const idx = sessionFiles.value.findIndex(f => f.id === localId)
        if (idx >= 0) {
          sessionFiles.value[idx] = { ...serverData, pending: false }
        }
      }
    } catch (e) {
      console.error('持久化文件上下文失败:', e)
      // 标记为非 pending，保留在列表中
      const idx = sessionFiles.value.findIndex(f => f.id === localId)
      if (idx >= 0) {
        sessionFiles.value[idx].pending = false
      }
    }
  }
}

// 移除会话关联文件
async function handleRemoveFile(fileId) {
  sessionFiles.value = sessionFiles.value.filter(f => f.id !== fileId)
  if (chatId.value) {
    try {
      await removeSessionFile(chatId.value, fileId)
    } catch (e) {
      console.error('移除文件失败:', e)
    }
  }
}

onMounted(() => {
  window.addEventListener('chat-selected', handleChatSelected)
  document.addEventListener('click', handleClickOutside)
  restoreLastSession()
})

onUnmounted(() => {
  window.removeEventListener('chat-selected', handleChatSelected)
  document.removeEventListener('click', handleClickOutside)
  if (currentStream) {
    if (currentStream.abort) currentStream.abort()
    else if (currentStream.close) currentStream.close()
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