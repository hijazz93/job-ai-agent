import { apiClient } from './chat'

export function addSessionFile(sessionId, fileData) {
  return apiClient.post(`/chat/sessions/${encodeURIComponent(sessionId)}/files`, fileData)
}

export function getSessionFiles(sessionId) {
  return apiClient.get(`/chat/sessions/${encodeURIComponent(sessionId)}/files`)
}

export function removeSessionFile(sessionId, fileId) {
  return apiClient.delete(`/chat/sessions/${encodeURIComponent(sessionId)}/files/${fileId}`)
}
