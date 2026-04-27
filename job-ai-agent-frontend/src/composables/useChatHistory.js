import { ref } from 'vue'

const HISTORY_KEY = 'job-ai-chat-history'
const MAX_HISTORY = 50
const DEBOUNCE_MS = 500

const chats = ref(loadHistory())

let saveTimer = null
let pendingSave = false

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveHistory() {
  if (saveTimer) return
  saveTimer = setTimeout(() => {
    try {
      localStorage.setItem(HISTORY_KEY, JSON.stringify(chats.value))
    } catch (e) {
      console.error('Failed to save chat history:', e)
    }
    saveTimer = null
    if (pendingSave) {
      pendingSave = false
      saveHistory()
    }
  }, DEBOUNCE_MS)
}

function saveHistoryImmediate() {
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(chats.value))
  } catch (e) {
    console.error('Failed to save chat history immediately:', e)
  }
}

function createChat(title = '新对话') {
  const chat = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 8),
    title,
    createdAt: Date.now(),
    updatedAt: Date.now(),
    messages: []
  }
  chats.value.unshift(chat)
  if (chats.value.length > MAX_HISTORY) {
    chats.value = chats.value.slice(0, MAX_HISTORY)
  }
  saveHistoryImmediate()
  return chat
}

function updateChatTitle(id, title) {
  const chat = chats.value.find(c => c.id === id)
  if (chat) {
    chat.title = title
    chat.updatedAt = Date.now()
    saveHistory()
  }
}

function addMessage(id, message) {
  const chat = chats.value.find(c => c.id === id)
  if (chat && message) {
    chat.messages.push(message)
    chat.updatedAt = Date.now()
    if (!chat.title || chat.title === '新对话') {
      const firstUser = chat.messages.find(m => m && m.role === 'user')
      if (firstUser) {
        chat.title = firstUser.content ? firstUser.content.slice(0, 30) : '新对话'
      }
    }
    saveHistory()
  }
}

function deleteChat(id) {
  chats.value = chats.value.filter(c => c.id !== id)
  saveHistoryImmediate()
}

function getChat(id) {
  return chats.value.find(c => c.id === id) || null
}

function getChatMessages(id) {
  const chat = getChat(id)
  return chat ? chat.messages : []
}

function clearAllChats() {
  chats.value = []
  if (saveTimer) {
    clearTimeout(saveTimer)
    saveTimer = null
  }
  localStorage.removeItem(HISTORY_KEY)
}

export function useChatHistory() {
  return {
    chats,
    loadHistory,
    createChat,
    updateChatTitle,
    addMessage,
    deleteChat,
    getChat,
    getChatMessages,
    clearAllChats
  }
}
