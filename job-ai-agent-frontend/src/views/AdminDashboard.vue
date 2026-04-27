<template>
  <div class="admin-dashboard">
    <div class="admin-header">
      <h1 class="admin-title">管理面板</h1>
      <div class="admin-tabs">
        <button
            class="tab-btn"
            :class="{ active: activeTab === 'requests' }"
            @click="activeTab = 'requests'"
        >升级申请</button>
        <button
            class="tab-btn"
            :class="{ active: activeTab === 'users' }"
            @click="activeTab = 'users'"
        >用户管理</button>
        <button
            class="tab-btn"
            :class="{ active: activeTab === 'knowledge' }"
            @click="activeTab = 'knowledge'"
        >知识库管理</button>
      </div>
    </div>

    <!-- 升级申请 -->
    <div v-if="activeTab === 'requests'" class="tab-content">
      <div class="filter-bar">
        <button
            v-for="f in filters"
            :key="f.value"
            class="filter-btn"
            :class="{ active: currentFilter === f.value }"
            @click="currentFilter = f.value; loadRequests()"
        >{{ f.label }}</button>
      </div>

      <div v-if="requests.length === 0" class="empty-state">
        <p>暂无升级申请</p>
      </div>

      <div v-else class="request-list">
        <div v-for="req in requests" :key="req.id" class="request-card">
          <div class="request-info">
            <div class="request-user">
              <span class="request-avatar">{{ req.username.charAt(0).toUpperCase() }}</span>
              <div>
                <div class="request-name">{{ req.username }}</div>
                <div class="request-email">{{ req.email }}</div>
                <div class="request-time">{{ formatTime(req.createdAt) }}</div>
              </div>
            </div>
            <span class="request-status" :class="'status-' + req.status.toLowerCase()">
              {{ statusLabel(req.status) }}
            </span>
          </div>
          <div v-if="req.status === 'PENDING'" class="request-actions">
            <button class="action-btn approve-btn" @click="handleApprove(req.id)">批准</button>
            <button class="action-btn reject-btn" @click="handleReject(req.id)">拒绝</button>
          </div>
        </div>
      </div>

      <div v-if="actionMessage" class="alert" :class="'alert-' + actionMessageType">
        {{ actionMessage }}
      </div>
    </div>

    <!-- 知识库管理 -->
    <div v-if="activeTab === 'knowledge'" class="tab-content">
      <KnowledgeBaseManage></KnowledgeBaseManage>
    </div>

    <!-- 用户管理 -->
    <div v-if="activeTab === 'users'" class="tab-content">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="search-bar">
          <input v-model="userKeyword" placeholder="搜索用户名/邮箱..." @keyup.enter="searchUsers" />
          <button class="search-btn" @click="searchUsers">搜索</button>
          <button v-if="userKeyword" class="clear-btn" @click="userKeyword = ''; searchUsers()">清除</button>
        </div>
        <div class="toolbar-actions">
          <button class="btn btn-primary" @click="openCreateUser">新增用户</button>
        </div>
      </div>

      <!-- Alert -->
      <div v-if="userMessage" class="alert" :class="'alert-' + userMessageType">{{ userMessage }}</div>

      <!-- User Table -->
      <div class="table-wrapper">
        <table class="kb-table">
          <thead>
          <tr>
            <th style="width: 50px">ID</th>
            <th style="width: 130px">用户名</th>
            <th style="width: 180px">邮箱</th>
            <th style="width: 100px">角色</th>
            <th style="width: 70px">状态</th>
            <th style="width: 150px">注册时间</th>
            <th style="width: 100px">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="users.length === 0">
            <td colspan="7" class="empty-cell">暂无用户数据</td>
          </tr>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.id }}</td>
            <td>{{ u.username }}</td>
            <td>{{ u.email }}</td>
            <td>
                <span class="role-badge" :class="'role-' + u.role.toLowerCase()">
                  {{ roleLabel(u.role) }}
                </span>
            </td>
            <td>
              <span class="enabled-dot" :class="{ active: u.enabled }"></span>
              <span style="font-size: 12px; color: var(--text-tertiary); margin-left: 4px">{{ u.enabled ? '正常' : '禁用' }}</span>
            </td>
            <td class="text-muted">{{ formatTime(u.createdAt) }}</td>
            <td class="action-cell">
              <button class="action-link edit" @click="openEditUser(u)">编辑</button>
              <button class="action-link delete" @click="confirmDeleteUser(u)">删除</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="userTotalPages > 0" class="pagination">
        <button class="page-btn" :disabled="userCurrentPage === 0" @click="goToUserPage(userCurrentPage - 1)">上一页</button>
        <span class="page-info">第 {{ userCurrentPage + 1 }} / {{ userTotalPages }} 页（共 {{ userTotalElements }} 条）</span>
        <button class="page-btn" :disabled="userCurrentPage >= userTotalPages - 1" @click="goToUserPage(userCurrentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- Create/Edit User Modal -->
    <div v-if="showUserModal" class="modal-overlay" @click.self="closeUserModal">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editingUser ? '编辑用户' : '新增用户' }}</h3>
          <button class="close-btn" @click="closeUserModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>用户名 <span class="required">*</span></label>
            <input v-model="userForm.username" placeholder="用户名" :disabled="editingUser && !allowRename" />
          </div>
          <div class="form-group">
            <label>邮箱 <span class="required">*</span></label>
            <input v-model="userForm.email" placeholder="user@example.com" />
          </div>
          <div class="form-group" v-if="!editingUser">
            <label>密码 <span class="required">*</span></label>
            <input v-model="userForm.password" type="password" placeholder="至少6位密码" />
          </div>
          <div class="form-row">
            <div class="form-group flex-1">
              <label>角色</label>
              <select v-model="userForm.role" class="form-select">
                <option value="NORMAL_USER">普通用户</option>
                <option value="PREMIUM_USER">高级用户</option>
                <option value="ADMIN">管理员</option>
              </select>
            </div>
            <div class="form-group flex-1">
              <label>状态</label>
              <select v-model="userForm.enabled" class="form-select">
                <option :value="true">正常</option>
                <option :value="false">禁用</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="closeUserModal">取消</button>
          <button class="btn btn-primary" @click="saveUser" :disabled="savingUser">
            {{ savingUser ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteUserConfirm" class="modal-overlay" @click.self="showDeleteUserConfirm = false">
      <div class="modal confirm-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
        </div>
        <div class="modal-body">
          <p>确定要删除用户「{{ deletingUser?.username }}」吗？删除后不可恢复。</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="showDeleteUserConfirm = false">取消</button>
          <button class="btn btn-danger" @click="handleDeleteUser" :disabled="deletingUserLoading">
            {{ deletingUserLoading ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiClient } from '../api/chat'
import KnowledgeBaseManage from '../components/KnowledgeBaseManage.vue'

const activeTab = ref('requests')
const currentFilter = ref('PENDING')
const requests = ref([])
const actionMessage = ref('')
const actionMessageType = ref('success')

// User management state
const users = ref([])
const userKeyword = ref('')
const userCurrentPage = ref(0)
const userTotalPages = ref(0)
const userTotalElements = ref(0)
const userMessage = ref('')
const userMessageType = ref('success')
const showUserModal = ref(false)
const showDeleteUserConfirm = ref(false)
const editingUser = ref(null)
const deletingUser = ref(null)
const deletingUserLoading = ref(false)
const savingUser = ref(false)
const allowRename = ref(false)

const filters = [
  { label: '待审核', value: 'PENDING' },
  { label: '已批准', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '全部', value: '' }
]

function statusLabel(status) {
  switch (status) {
    case 'PENDING': return '待审核'
    case 'APPROVED': return '已批准'
    case 'REJECTED': return '已拒绝'
    default: return status
  }
}

function roleLabel(role) {
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'PREMIUM_USER': return '高级用户'
    default: return '普通用户'
  }
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function loadRequests() {
  try {
    const params = currentFilter.value ? `?status=${currentFilter.value}` : ''
    const res = await apiClient.get(`/admin/upgrade-requests${params}`)
    if (res.data.success) {
      requests.value = res.data.data || []
    }
  } catch (e) {
    console.error('加载申请失败:', e)
  }
}

async function loadUsers() {
  try {
    const params = `?page=${userCurrentPage.value}&size=10${userKeyword.value ? '&keyword=' + encodeURIComponent(userKeyword.value) : ''}`
    const res = await apiClient.get(`/admin/users${params}`)
    if (res.data.success) {
      const d = res.data.data
      users.value = d.content || []
      userTotalPages.value = d.totalPages || 0
      userTotalElements.value = d.totalElements || 0
    }
  } catch (e) {
    console.error('加载用户失败:', e)
  }
}

function searchUsers() {
  userCurrentPage.value = 0
  loadUsers()
}

function goToUserPage(page) {
  if (page < 0 || page >= userTotalPages.value) return
  userCurrentPage.value = page
  loadUsers()
}

const userForm = ref({
  username: '',
  email: '',
  password: '',
  role: 'NORMAL_USER',
  enabled: true
})

let userMessageTimer = null

function showUserMessage(msg, type = 'success') {
  userMessage.value = msg
  userMessageType.value = type
  if (userMessageTimer) clearTimeout(userMessageTimer)
  userMessageTimer = setTimeout(() => {
    userMessage.value = ''
  }, 4000)
}

function openCreateUser() {
  editingUser.value = null
  allowRename.value = false
  userForm.value = { username: '', email: '', password: '', role: 'NORMAL_USER', enabled: true }
  showUserModal.value = true
}

function openEditUser(u) {
  editingUser.value = u
  allowRename.value = true
  userForm.value = {
    username: u.username || '',
    email: u.email || '',
    password: '',
    role: u.role || 'NORMAL_USER',
    enabled: u.enabled !== false
  }
  showUserModal.value = true
}

function closeUserModal() {
  showUserModal.value = false
  editingUser.value = null
}

async function saveUser() {
  if (!userForm.value.username.trim()) {
    showUserMessage('用户名不能为空', 'error')
    return
  }
  if (!userForm.value.email.trim()) {
    showUserMessage('邮箱不能为空', 'error')
    return
  }
  if (!editingUser.value && (!userForm.value.password || userForm.value.password.length < 6)) {
    showUserMessage('密码至少6位', 'error')
    return
  }
  savingUser.value = true
  try {
    if (editingUser.value) {
      const body = {
        username: userForm.value.username,
        email: userForm.value.email,
        role: userForm.value.role,
        enabled: userForm.value.enabled
      }
      await apiClient.put(`/admin/users/${editingUser.value.id}`, body)
      showUserMessage('更新成功')
    } else {
      await apiClient.post('/admin/users', userForm.value)
      showUserMessage('创建成功')
    }
    closeUserModal()
    loadUsers()
  } catch (e) {
    showUserMessage(e.response?.data?.message || '操作失败', 'error')
  } finally {
    savingUser.value = false
  }
}

function confirmDeleteUser(u) {
  deletingUser.value = u
  showDeleteUserConfirm.value = true
}

async function handleDeleteUser() {
  if (!deletingUser.value) return
  deletingUserLoading.value = true
  try {
    await apiClient.delete(`/admin/users/${deletingUser.value.id}`)
    showUserMessage('删除成功')
    showDeleteUserConfirm.value = false
    deletingUser.value = null
    loadUsers()
  } catch (e) {
    showUserMessage(e.response?.data?.message || '删除失败', 'error')
  } finally {
    deletingUserLoading.value = false
  }
}

async function handleApprove(id) {
  try {
    const res = await apiClient.post(`/admin/upgrade-requests/${id}/approve`)
    actionMessage.value = res.data.message || '已批准'
    actionMessageType.value = 'success'
    loadRequests()
  } catch (e) {
    actionMessage.value = e.response?.data?.message || '操作失败'
    actionMessageType.value = 'error'
  }
}

async function handleReject(id) {
  try {
    const res = await apiClient.post(`/admin/upgrade-requests/${id}/reject`)
    actionMessage.value = res.data.message || '已拒绝'
    actionMessageType.value = 'success'
    loadRequests()
  } catch (e) {
    actionMessage.value = e.response?.data?.message || '操作失败'
    actionMessageType.value = 'error'
  }
}

onMounted(() => {
  loadRequests()
  loadUsers()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 24px;
  max-width: 960px;
  margin: 0 auto;
  background: var(--bg-primary);
  min-height: calc(100vh - var(--topbar-height));
}

.admin-header {
  margin-bottom: 24px;
}

.admin-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.admin-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 4px;
}

.tab-btn {
  flex: 1;
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-btn.active {
  background: var(--accent-primary);
  color: #fff;
}

.tab-btn:hover:not(.active) {
  background: var(--bg-hover);
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.filter-btn {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-primary);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-btn.active {
  background: var(--accent-primary);
  color: #fff;
  border-color: var(--accent-primary);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-tertiary);
  font-size: 15px;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.request-card:hover {
  background: var(--bg-card-hover);
}

.request-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.request-user {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.request-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--accent-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.request-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.request-email {
  font-size: 13px;
  color: var(--text-tertiary);
}

.request-time {
  font-size: 12px;
  color: var(--text-disabled);
  margin-top: 2px;
}

.request-status {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: rgba(251, 188, 4, 0.12);
  color: #fbbc04;
}

.status-approved {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.status-rejected {
  background: rgba(234, 67, 53, 0.12);
  color: #ea4335;
}

.request-actions {
  display: flex;
  gap: 8px;
  margin-left: 16px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 20px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.approve-btn {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.approve-btn:hover {
  background: rgba(52, 168, 83, 0.22);
}

.reject-btn {
  background: rgba(234, 67, 53, 0.12);
  color: #ea4335;
}

.reject-btn:hover {
  background: rgba(234, 67, 53, 0.22);
}

.role-badge {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}

.role-admin {
  background: rgba(234, 67, 53, 0.12);
  color: #ea4335;
}

.role-premium_user {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.role-normal_user {
  background: rgba(66, 133, 244, 0.12);
  color: #4285f4;
}

.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
}

.alert-success {
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
}

.alert-error {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
}

/* ====== User Management Styles ====== */

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 240px;
}

.search-bar input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  transition: border-color var(--transition-fast);
  box-sizing: border-box;
}

.search-bar input:focus {
  border-color: var(--accent-primary);
}

.search-btn, .clear-btn {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.search-btn:hover, .clear-btn:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.btn {
  padding: 8px 20px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--accent-primary);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-cancel {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border-primary);
}

.btn-cancel:hover {
  background: var(--bg-card-hover);
}

.btn-danger {
  background: #ea4335;
  color: #fff;
}

.btn-danger:hover:not(:disabled) {
  background: #d33426;
}

.table-wrapper {
  overflow-x: auto;
  margin-bottom: 16px;
}

.kb-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.kb-table th, .kb-table td {
  padding: 10px 14px;
  text-align: left;
  font-size: 13px;
  border-bottom: 1px solid var(--border-primary);
}

.kb-table th {
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 12px;
}

.kb-table td {
  color: var(--text-primary);
}

.kb-table tr:last-child td {
  border-bottom: none;
}

.kb-table tr:hover td {
  background: var(--bg-hover);
}

.empty-cell {
  text-align: center;
  padding: 40px 20px !important;
  color: var(--text-tertiary);
  font-size: 14px;
}

.text-muted {
  color: var(--text-tertiary);
  font-size: 12px;
}

.action-cell {
  white-space: nowrap;
}

.action-link {
  background: none;
  border: none;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.action-link.edit {
  color: var(--accent-primary);
}

.action-link.edit:hover {
  background: rgba(66, 133, 244, 0.1);
}

.action-link.delete {
  color: #ea4335;
}

.action-link.delete:hover {
  background: rgba(234, 67, 53, 0.1);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 8px 0;
}

.page-btn {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-tertiary);
}

.enabled-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-disabled);
}

.enabled-dot.active {
  background: #34a853;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 560px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.confirm-modal {
  max-width: 420px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: var(--text-primary);
}

.modal-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-primary);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.form-group input[type="text"],
.form-group input:not([type]),
.form-group input[type="password"] {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--transition-fast);
}

.form-group input:focus {
  border-color: var(--accent-primary);
}

.form-group input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 {
  flex: 1;
}

.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--transition-fast);
  cursor: pointer;
  appearance: auto;
}

.form-select:focus {
  border-color: var(--accent-primary);
}

.required {
  color: #ea4335;
}
</style>