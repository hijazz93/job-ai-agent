import axios from 'axios'

const API_BASE_URL = 'http://localhost:8123/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export { apiClient }

/**
 * JobManus SSE 流式聊天 (SseEmitter)
 * @param {string} message - 用户消息
 * @param {function} onMessage - 消息回调
 * @param {function} onError - 错误回调
 * @param {function} onComplete - 完成回调
 * @returns {EventSource}
 */
export function chatWithJobManus(message, { onMessage, onError, onComplete }) {
  const encodedMessage = encodeURIComponent(message)
  const eventSource = new EventSource(`${API_BASE_URL}/ai/manus/chat?message=${encodedMessage}`)

  let fullResponse = ''
  let isDone = false

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      isDone = true
      onComplete && onComplete(fullResponse)
      eventSource.close()
      return
    }
    fullResponse += data
    onMessage && onMessage(data, fullResponse)
  }

  eventSource.onerror = (error) => {
    if (isDone) return
    isDone = true
    eventSource.close()
    if (fullResponse) {
      onComplete && onComplete(fullResponse)
    } else {
      onError && onError(error)
    }
  }

  eventSource.onclose = () => {
    if (isDone) return
    isDone = true
    if (fullResponse) {
      onComplete && onComplete(fullResponse)
    }
  }

  return eventSource
}

/**
 * JobApp SSE 流式聊天 (Flux<String> text/event-stream)
 * @param {string} message - 用户消息
 * @param {string} chatId - 聊天ID
 * @param {function} onMessage - 消息回调
 * @param {function} onError - 错误回调
 * @param {function} onComplete - 完成回调
 * @returns {EventSource}
 */
export function chatWithJobAppSSE(message, chatId = 'default', { onMessage, onError, onComplete }) {
  const encodedMessage = encodeURIComponent(message)
  const encodedChatId = encodeURIComponent(chatId)
  const eventSource = new EventSource(`${API_BASE_URL}/ai/job_app/chat/sse?message=${encodedMessage}&chatId=${encodedChatId}`)

  let fullResponse = ''
  let isDone = false

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      isDone = true
      onComplete && onComplete(fullResponse)
      eventSource.close()
      return
    }
    fullResponse += data
    onMessage && onMessage(data, fullResponse)
  }

  eventSource.onerror = (error) => {
    if (isDone) return
    isDone = true
    eventSource.close()
    if (fullResponse) {
      onComplete && onComplete(fullResponse)
    } else {
      onError && onError(error)
    }
  }

  eventSource.onclose = () => {
    if (isDone) return
    isDone = true
    if (fullResponse) {
      onComplete && onComplete(fullResponse)
    }
  }

  return eventSource
}
