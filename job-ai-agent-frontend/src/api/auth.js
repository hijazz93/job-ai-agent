import { apiClient } from './chat'

export function login(username, password) {
  return apiClient.post('/auth/login', { username, password })
}

export function register(username, email, password) {
  return apiClient.post('/auth/register', { username, email, password })
}

export function getProfile() {
  return apiClient.get('/user/profile')
}

export function submitUpgradeRequest() {
  return apiClient.post('/user/upgrade-request')
}

export function getUpgradeRequestStatus() {
  return apiClient.get('/user/upgrade-request/status')
}
