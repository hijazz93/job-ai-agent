import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export { apiClient }

// 从 localStorage 获取 JWT 令牌，用于 SSE 连接认证
function getTokenParam() {
  const token = localStorage.getItem('job-ai-auth-token')
  return token ? `&token=${encodeURIComponent(token)}` : ''
}

/**
 * 创建 SSE 连接（公共函数，消除重复代码）
 * @param {string} url - SSE 端点 URL
 * @param {Object} callbacks - 回调函数 { onMessage, onError, onComplete }
 * @returns {EventSource}
 */
function createSSEConnection(url, { onMessage, onError, onComplete }) {
  const eventSource = new EventSource(url)

  let fullResponse = ''
  let isDone = false
  let isClosed = false

  const safeComplete = (data) => {
    if (!isDone && !isClosed) {
      isDone = true
      try { eventSource.close() } catch(e) {}
      if (onComplete) onComplete(data || fullResponse)
    }
  }

  const safeError = (error) => {
    if (!isDone && !isClosed) {
      isDone = true
      isClosed = true
      try { eventSource.close() } catch(e) {}
      if (fullResponse && onComplete) {
        onComplete(fullResponse)
      } else if (onError) {
        onError(error)
      }
    }
  }

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      safeComplete(fullResponse)
      return
    }
    fullResponse += data
    if (onMessage) onMessage(data, fullResponse)
  }

  eventSource.onerror = (error) => {
    console.error('SSE connection error:', error)
    safeError(error)
  }

  // onclose 在 EventSource 中不是标准事件，但某些实现支持
  // 这里通过 onerror 统一处理关闭场景
  eventSource.onclose = () => {
    if (!isDone) {
      safeComplete(fullResponse)
    }
  }

  return eventSource
}

/**
 * JobApp SSE 流式聊天 + RAG 知识库检索 (Flux<String> text/event-stream)
 * @param {string} message - 用户消息
 * @param {string} chatId - 聊天ID
 * @param {Object} options - 回调函数 { onMessage, onError, onComplete }
 * @returns {EventSource}
 */
export function chatWithJobAppSSEWithRag(message, chatId = 'default', { onMessage, onError, onComplete }) {
  const encodedMessage = encodeURIComponent(message)
  const encodedChatId = encodeURIComponent(chatId)
  const url = `${API_BASE_URL}/ai/job_app/chat/sse_rag?message=${encodedMessage}&chatId=${encodedChatId}${getTokenParam()}`
  return createSSEConnection(url, { onMessage, onError, onComplete })
}

/**
 * JobManus SSE 流式聊天 — POST + fetch ReadableStream (避免 GET URL 过长)
 * @param {string} message - 用户消息
 * @param {Object} options - 回调函数 { selectedTools, onStep, onAnswer, onError, onComplete }
 *   onStep(data)   — 中间步骤日志
 *   onAnswer(data) — 最终 AI 回复文本
 *   onComplete()   — 流结束
 * @returns {AbortController} 用于取消请求
 */
export function chatWithJobManus(message, { selectedTools = [], sessionId = '', onStep, onAnswer, onError, onComplete }) {
  const abortController = new AbortController()
  const token = localStorage.getItem('job-ai-auth-token')

  const body = { message }
  if (selectedTools && selectedTools.length > 0) {
    body.tools = selectedTools.join(',')
  }
  if (sessionId) {
    body.sessionId = sessionId
  }

  const headers = { 'Content-Type': 'application/json' }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  fetch(`${API_BASE_URL}/ai/manus/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
    signal: abortController.signal
  }).then(async response => {
    if (!response.ok) {
      const text = await response.text().catch(() => '')
      throw new Error(text || `HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let finalAnswer = ''

    const unescape = (s) => s.replace(/__NL__/g, '\n').replace(/__CR__/g, '\r')

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        if (onComplete) onComplete(finalAnswer)
        return
      }

      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = unescape(line.substring(5).trim())

          if (data === '[DONE]') {
            if (onComplete) onComplete(finalAnswer)
            return
          }

          if (data.startsWith('[ANSWER]')) {
            finalAnswer = data.substring(8)
            if (onAnswer) onAnswer(finalAnswer)
          } else if (data.startsWith('[STEP]')) {
            const stepData = data.substring(6)
            if (onStep) onStep(stepData)
          }
        }
      }
    }
  }).catch(error => {
    if (error.name === 'AbortError') return
    if (onError) onError(error)
    else console.error('SSE fetch error:', error)
  })

  return abortController
}

/**
 * JobApp SSE 流式聊天 (Flux<String> text/event-stream)
 * @param {string} message - 用户消息
 * @param {string} chatId - 聊天ID
 * @param {Object} options - 回调函数 { onMessage, onError, onComplete }
 * @returns {EventSource}
 */
export function chatWithJobAppSSE(message, chatId = 'default', { onMessage, onError, onComplete }) {
  const encodedMessage = encodeURIComponent(message)
  const encodedChatId = encodeURIComponent(chatId)
  const url = `${API_BASE_URL}/ai/job_app/chat/sse?message=${encodedMessage}&chatId=${encodedChatId}${getTokenParam()}`
  return createSSEConnection(url, { onMessage, onError, onComplete })
}
