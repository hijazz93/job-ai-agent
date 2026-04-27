<template>
  <div class="knowledge-manage">
    <!-- Alert -->
    <div v-if="message" class="alert" :class="'alert-' + messageType">{{ message }}</div>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-bar">
        <input v-model="keyword" placeholder="搜索标题/描述..." @keyup.enter="search" />
        <button class="search-btn" @click="search">搜索</button>
        <button v-if="keyword" class="clear-btn" @click="keyword = ''; search()">清除</button>
      </div>
      <div class="toolbar-actions">
        <button class="btn btn-primary" @click="openCreate">新建文档</button>
        <button class="btn btn-secondary" @click="rebuildIndex" :disabled="rebuilding">
          {{ rebuilding ? '重建中...' : '重建索引' }}
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="table-wrapper">
      <table class="kb-table">
        <thead>
          <tr>
            <th style="width: 60px">ID</th>
            <th style="width: 180px">标题</th>
            <th>描述</th>
            <th style="width: 100px">标签</th>
            <th style="width: 60px">启用</th>
            <th style="width: 160px">创建时间</th>
            <th style="width: 120px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="documents.length === 0">
            <td colspan="7" class="empty-cell">暂无知识库文档</td>
          </tr>
          <tr v-for="doc in documents" :key="doc.id">
            <td>{{ doc.id }}</td>
            <td class="title-cell" :title="doc.title">{{ doc.title }}</td>
            <td class="desc-cell" :title="doc.description">{{ doc.description || '-' }}</td>
            <td>
              <span v-if="doc.status" class="status-tag">{{ doc.status }}</span>
              <span v-else class="text-muted">-</span>
            </td>
            <td>
              <span class="enabled-dot" :class="{ active: doc.enabled }"></span>
            </td>
            <td class="text-muted">{{ formatTime(doc.createdAt) }}</td>
            <td class="action-cell">
              <button class="action-link edit" @click="openEdit(doc)">编辑</button>
              <button class="action-link delete" @click="confirmDelete(doc)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 0" class="pagination">
      <button class="page-btn" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">上一页</button>
      <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页（共 {{ totalElements }} 条）</span>
      <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="goToPage(currentPage + 1)">下一页</button>
    </div>

    <!-- Create/Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editingDoc ? '编辑知识库文档' : '新建知识库文档' }}</h3>
          <button class="close-btn" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>标题 <span class="required">*</span></label>
            <input v-model="form.title" placeholder="文档标题" />
          </div>
          <div class="form-row">
            <div class="form-group flex-1">
              <label>描述</label>
              <input v-model="form.description" placeholder="简要描述" />
            </div>
            <div class="form-group flex-1">
              <label>标签</label>
              <input v-model="form.status" placeholder="如：校招、简历" />
            </div>
          </div>
          <div class="form-group">
            <label>内容（Markdown）</label>
            <textarea v-model="form.content" rows="12" placeholder="文档内容（支持 Markdown 格式）"></textarea>
          </div>
          <div class="form-group checkbox-group" v-if="editingDoc">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.enabled" />
              启用（纳入知识库检索）
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="closeModal">取消</button>
          <button class="btn btn-primary" @click="saveDocument" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click.self="showDeleteConfirm = false">
      <div class="modal confirm-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
        </div>
        <div class="modal-body">
          <p>确定要删除「{{ deletingDoc?.title }}」吗？删除后不可恢复。</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-cancel" @click="showDeleteConfirm = false">取消</button>
          <button class="btn btn-danger" @click="handleDelete" :disabled="deleting">
            {{ deleting ? '删除中...' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiClient } from '../api/chat'

const documents = ref([])
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const keyword = ref('')
const showModal = ref(false)
const showDeleteConfirm = ref(false)
const editingDoc = ref(null)
const deletingDoc = ref(null)
const saving = ref(false)
const deleting = ref(false)
const rebuilding = ref(false)
const message = ref('')
const messageType = ref('success')

const form = ref({
  title: '',
  description: '',
  status: '',
  content: '',
  enabled: true
})

let messageTimer = null

function showMessage(msg, type = 'success') {
  message.value = msg
  messageType.value = type
  if (messageTimer) clearTimeout(messageTimer)
  messageTimer = setTimeout(() => {
    message.value = ''
  }, 4000)
}

async function loadData() {
  try {
    const params = `?page=${currentPage.value}&size=10${keyword.value ? '&keyword=' + encodeURIComponent(keyword.value) : ''}`
    const res = await apiClient.get(`/admin/knowledge${params}`)
    if (res.data.success) {
      const d = res.data.data
      documents.value = d.content || []
      totalPages.value = d.totalPages || 0
      totalElements.value = d.totalElements || 0
    }
  } catch (e) {
    console.error('加载知识库失败:', e)
  }
}

function search() {
  currentPage.value = 0
  loadData()
}

function goToPage(page) {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  loadData()
}

function openCreate() {
  editingDoc.value = null
  form.value = { title: '', description: '', status: '', content: '', enabled: true }
  showModal.value = true
}

function openEdit(doc) {
  editingDoc.value = doc
  form.value = {
    title: doc.title || '',
    description: doc.description || '',
    status: doc.status || '',
    content: doc.content || '',
    enabled: doc.enabled !== false
  }
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingDoc.value = null
}

async function saveDocument() {
  if (!form.value.title.trim()) {
    showMessage('标题不能为空', 'error')
    return
  }
  saving.value = true
  try {
    if (editingDoc.value) {
      await apiClient.put(`/admin/knowledge/${editingDoc.value.id}`, form.value)
      showMessage('更新成功')
    } else {
      await apiClient.post('/admin/knowledge', form.value)
      showMessage('创建成功')
    }
    closeModal()
    loadData()
  } catch (e) {
    showMessage(e.response?.data?.message || '操作失败', 'error')
  } finally {
    saving.value = false
  }
}

function confirmDelete(doc) {
  deletingDoc.value = doc
  showDeleteConfirm.value = true
}

async function handleDelete() {
  if (!deletingDoc.value) return
  deleting.value = true
  try {
    await apiClient.delete(`/admin/knowledge/${deletingDoc.value.id}`)
    showMessage('删除成功')
    showDeleteConfirm.value = false
    deletingDoc.value = null
    loadData()
  } catch (e) {
    showMessage(e.response?.data?.message || '删除失败', 'error')
  } finally {
    deleting.value = false
  }
}

async function rebuildIndex() {
  rebuilding.value = true
  try {
    const res = await apiClient.post('/admin/knowledge/rebuild')
    showMessage(res.data?.message || '索引重建成功')
  } catch (e) {
    showMessage(e.response?.data?.message || '重建失败', 'error')
  } finally {
    rebuilding.value = false
  }
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN') + ' ' + d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.knowledge-manage {
  padding: 0;
}

.alert {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  margin-bottom: 16px;
}

.alert-success {
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
}

.alert-error {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
}

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

.btn-secondary {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border-primary);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
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

.title-cell {
  font-weight: 500;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.desc-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-tertiary);
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

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: rgba(66, 133, 244, 0.1);
  color: #4285f4;
  font-size: 12px;
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
  max-width: 680px;
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
.form-group input:not([type]) {
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

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--accent-primary);
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 {
  flex: 1;
}

.form-group textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  resize: vertical;
  min-height: 120px;
  font-family: inherit;
  line-height: 1.6;
  box-sizing: border-box;
  transition: border-color var(--transition-fast);
}

.required {
  color: #ea4335;
}

.checkbox-group label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-primary);
}

.checkbox-group input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--accent-primary);
}
</style>
