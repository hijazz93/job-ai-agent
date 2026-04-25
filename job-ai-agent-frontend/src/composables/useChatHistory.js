import { ref } from 'vue'

const HISTORY_KEY = 'job-ai-chat-history'
const MAX_HISTORY = 50

const chats = ref(loadHistory())

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveHistory() {
  localStorage.setItem(HISTORY_KEY, JSON.stringify(chats.value))
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
  saveHistory()
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
  if (chat) {
    chat.messages.push(message)
    chat.updatedAt = Date.now()
    if (!chat.title || chat.title === '新对话') {
      const firstUser = chat.messages.find(m => m.role === 'user')
      if (firstUser) {
        chat.title = firstUser.content.slice(0, 30)
      }
    }
    saveHistory()
  }
}

function deleteChat(id) {
  chats.value = chats.value.filter(c => c.id !== id)
  saveHistory()
}

function getChat(id) {
  return chats.value.find(c => c.id === id) || null
}

function getChatMessages(id) {
  const chat = getChat(id)
  return chat ? chat.messages : []
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
    getChatMessages
  }
}
