import { apiClient } from './chat'

export function getSessions() {
  return apiClient.get('/chat/sessions')
}

export function createSession(id, title) {
  return apiClient.post('/chat/sessions', { id, title })
}

export function deleteSession(id) {
  return apiClient.delete(`/chat/sessions/${id}`)
}

export function updateSessionTitle(id, title) {
  return apiClient.put(`/chat/sessions/${id}`, { title })
}

export function getSessionMessages(sessionId) {
  return apiClient.get(`/chat/sessions/${sessionId}/messages`)
}

export function saveMessage(sessionId, role, content) {
  return apiClient.post(`/chat/sessions/${sessionId}/messages`, { role, content })
}
