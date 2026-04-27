<template>
  <div class="document-upload-container">
    <div class="upload-header">
      <h3 class="upload-title">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 12z"/>
        </svg>
        文档知识库
      </h3>
      <p class="upload-subtitle">上传文档以丰富AI的知识库，支持多种格式</p>
    </div>

    <div 
      class="upload-zone"
      :class="{ 'drag-over': isDragOver, 'uploading': isUploading }"
      @drop.prevent="handleDrop"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @click="triggerFileInput"
    >
      <input
        ref="fileInput"
        type="file"
        multiple
        accept=".doc,.docx,.pdf,.txt,.md,.markdown,.xls,.xlsx,.ppt,.pptx,.csv,.json,.xml,.html,.jpg,.jpeg,.png,.gif"
        @change="handleFileSelect"
        style="display: none"
      />
      
      <div v-if="!isUploading" class="upload-content">
        <div class="upload-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="48" height="48">
            <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
          </svg>
        </div>
        <div class="upload-text">
          <p class="upload-main-text">拖拽文件到此处，或<span class="link">点击选择文件</span></p>
          <p class="upload-hint">支持 .pdf, .doc, .docx, .txt, .md 等格式，单个文件最大 10MB</p>
        </div>
      </div>

      <div v-else class="upload-progress-content">
        <div class="progress-animation">
          <div class="spinner"></div>
        </div>
        <p class="progress-text">{{ currentFileName }}</p>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
        </div>
        <p class="progress-percent">{{ uploadProgress }}%</p>
      </div>
    </div>

    <div v-if="uploadMessage" class="upload-message" :class="uploadMessageType">
      {{ uploadMessage }}
    </div>

    <div v-if="uploadedFiles.length > 0" class="recent-uploads">
      <div class="recent-header">
        <span class="recent-title">最近上传</span>
        <button class="refresh-btn" @click="loadUploadHistory" title="刷新列表">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
        </button>
      </div>
      
      <div class="file-list">
        <div 
          v-for="file in uploadedFiles.slice(0, 5)" 
          :key="file.id" 
          class="file-item"
        >
          <div class="file-icon" :class="getFileIconClass(file.fileType)">
            {{ getFileTypeLetter(file.fileType) }}
          </div>
          <div class="file-info">
            <div class="file-name" :title="file.originalName">{{ file.originalName }}</div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
              <span class="file-time">{{ formatTime(file.uploadTime) }}</span>
            </div>
          </div>
          <div class="file-actions">
            <button 
              class="file-action-btn preview-btn" 
              @click="previewFile(file)"
              title="预览"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
              </svg>
            </button>
            <button 
              class="file-action-btn delete-btn" 
              @click="deleteFile(file.id)"
              title="删除"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
              </svg>
            </button>
          </div>
        </div>
        
        <div v-if="uploadedFiles.length > 5" class="show-more">
          还有 {{ uploadedFiles.length - 5 }} 个文件...
        </div>
      </div>
    </div>

    <div class="supported-formats">
      <div class="formats-label">支持的格式：</div>
      <div class="formats-list">
        <span class="format-tag">PDF</span>
        <span class="format-tag">Word</span>
        <span class="format-tag">Excel</span>
        <span class="format-tag">PPT</span>
        <span class="format-tag">TXT</span>
        <span class="format-tag">MD</span>
        <span class="format-tag">图片</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const fileInput = ref(null)
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)
const currentFileName = ref('')
const uploadMessage = ref('')
const uploadMessageType = ref('success')
const uploadedFiles = ref([])

const emit = defineEmits(['upload-success', 'upload-error'])

function triggerFileInput() {
  if (!isUploading.value) {
    fileInput.value?.click()
  }
}

function handleDragOver(e) {
  isDragOver.value = true
}

function handleDragLeave(e) {
  isDragOver.value = false
}

function handleDrop(e) {
  isDragOver.value = false
  const files = e.dataTransfer.files
  if (files.length > 0) {
    uploadFiles(Array.from(files))
  }
}

function handleFileSelect(e) {
  const files = e.target.files
  if (files.length > 0) {
    uploadFiles(Array.from(files))
  }
  e.target.value = ''
}

async function uploadFiles(files) {
  for (let i = 0; i < files.length; i++) {
    await uploadSingleFile(files[i])
  }
  
  loadUploadHistory()
}

async function uploadSingleFile(file) {
  const maxSize = 10 * 1024 * 1024
  
  if (file.size > maxSize) {
    showMessage(`文件 "${file.name}" 超过大小限制（最大10MB）`, 'error')
    return
  }

  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'text/markdown'
  ]
  
  const ext = file.name.split('.').pop().toLowerCase()
  const allowedExts = ['doc', 'docx', 'pdf', 'txt', 'md', 'markdown', 
                      'xls', 'xlsx', 'ppt', 'pptx', 'csv', 'json', 
                      'xml', 'html', 'jpg', 'jpeg', 'png', 'gif']
  
  if (!allowedExts.includes(ext)) {
    showMessage(`不支持的文件格式：.${ext}`, 'error')
    return
  }

  isUploading.value = true
  currentFileName.value = file.name
  uploadProgress.value = 0
  uploadMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('file', file)

    const xhr = new XMLHttpRequest()

    await new Promise((resolve, reject) => {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          uploadProgress.value = Math.round((e.loaded / e.total) * 100)
        }
      })

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          const response = JSON.parse(xhr.responseText)
          if (response.success) {
            showMessage(`"${file.name}" 上传成功！`, 'success')
            emit('upload-success', response.data)
            resolve()
          } else {
            throw new Error(response.message || '上传失败')
          }
        } else {
          throw new Error(`HTTP ${xhr.status}: ${xhr.statusText}`)
        }
      })

      xhr.addEventListener('error', () => {
        reject(new Error('网络错误'))
      })

      xhr.open('POST', '/api/documents/upload')
      xhr.send(formData)
    })

  } catch (error) {
    console.error('上传失败:', error)
    showMessage(`上传失败：${error.message}`, 'error')
    emit('upload-error', { file, error })
  } finally {
    isUploading.value = false
    currentFileName.value = ''
    setTimeout(() => {
      uploadMessage.value = ''
    }, 5000)
  }
}

function showMessage(message, type = 'info') {
  uploadMessage.value = message
  uploadMessageType.value = type
}

async function loadUploadHistory() {
  try {
    const response = await fetch('/api/documents/history')
    const data = await response.json()
    
    if (data.success) {
      uploadedFiles.value = data.data || []
    }
  } catch (error) {
    console.error('加载上传历史失败:', error)
  }
}

function previewFile(file) {
  window.open(file.url, '_blank')
}

async function deleteFile(documentId) {
  if (!confirm('确定要删除这个文档吗？')) {
    return
  }

  try {
    const response = await fetch(`/api/documents/${documentId}`, {
      method: 'DELETE'
    })
    
    const data = await response.json()
    
    if (data.success) {
      showMessage('文档已删除', 'success')
      loadUploadHistory()
    } else {
      showMessage(data.message || '删除失败', 'error')
    }
  } catch (error) {
    console.error('删除失败:', error)
    showMessage('删除失败：' + error.message, 'error')
  }
}

function getFileIconClass(fileType) {
  const type = fileType?.toLowerCase() || ''
  if (type === 'pdf') return 'icon-pdf'
  if (['doc', 'docx'].includes(type)) return 'icon-word'
  if (['xls', 'xlsx'].includes(type)) return 'icon-excel'
  if (['ppt', 'pptx'].includes(type)) return 'icon-ppt'
  if (['jpg', 'jpeg', 'png', 'gif'].includes(type)) return 'icon-image'
  if (['txt', 'md', 'markdown'].includes(type)) return 'icon-text'
  return 'icon-default'
}

function getFileTypeLetter(fileType) {
  const type = fileType?.toUpperCase() || 'F'
  return type.substring(0, 1)
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTime(dateString) {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadUploadHistory()
})
</script>

<style scoped>
.document-upload-container {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: 20px;
  max-width: 500px;
}

.upload-header {
  margin-bottom: 20px;
}

.upload-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px 0;
}

.upload-title svg {
  color: var(--accent-primary);
}

.upload-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.upload-zone {
  border: 2px dashed var(--border-secondary);
  border-radius: var(--radius-md);
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--bg-secondary);
}

.upload-zone:hover {
  border-color: var(--accent-primary);
  background: rgba(26, 115, 232, 0.04);
}

.upload-zone.drag-over {
  border-color: var(--accent-primary);
  background: rgba(26, 115, 232, 0.08);
  transform: scale(1.02);
}

.upload-zone.uploading {
  pointer-events: none;
  opacity: 0.8;
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.upload-icon {
  color: var(--text-tertiary);
  transition: color var(--transition-fast);
}

.upload-zone:hover .upload-icon {
  color: var(--accent-primary);
}

.upload-main-text {
  font-size: 15px;
  color: var(--text-primary);
  margin: 0;
}

.link {
  color: var(--accent-primary);
  font-weight: 500;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 6px 0 0 0;
}

.upload-progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.progress-animation {
  width: 40px;
  height: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-primary);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.progress-text {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  margin: 0;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-bar {
  width: 100%;
  max-width: 280px;
  height: 6px;
  background: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), #58a6ff);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-percent {
  font-size: 13px;
  color: var(--accent-primary);
  font-weight: 600;
  margin: 0;
}

.upload-message {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 500;
}

.upload-message.success {
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
  border: 1px solid rgba(52, 168, 83, 0.2);
}

.upload-message.error {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
  border: 1px solid rgba(234, 67, 53, 0.2);
}

.recent-uploads {
  margin-top: 20px;
  border-top: 1px solid var(--border-primary);
  padding-top: 16px;
}

.recent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.recent-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.refresh-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.file-item:hover {
  background: var(--bg-hover);
}

.file-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.icon-pdf { background: #ea4335; }
.icon-word { background: #4285f4; }
.icon-excel { background: #34a853; }
.icon-ppt { background: #fbbc04; }
.icon-image { background: #ab47bc; }
.icon-text { background: #607d8b; }
.icon-default { background: #78909c; }

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.file-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.file-item:hover .file-actions {
  opacity: 1;
}

.file-action-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.file-action-btn:hover {
  background: var(--bg-active);
  color: var(--text-primary);
}

.delete-btn:hover {
  color: #ea4335;
  background: rgba(234, 67, 53, 0.1);
}

.show-more {
  text-align: center;
  font-size: 12px;
  color: var(--text-tertiary);
  padding: 8px;
  cursor: pointer;
}

.show-more:hover {
  color: var(--accent-primary);
}

.supported-formats {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-primary);
}

.formats-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 8px;
}

.formats-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.format-tag {
  padding: 3px 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  font-size: 11px;
  color: var(--text-secondary);
  font-weight: 500;
}
</style>
