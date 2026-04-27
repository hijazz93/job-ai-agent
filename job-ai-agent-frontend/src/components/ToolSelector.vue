<template>
  <div class="tool-selector">
    <!-- 已选中的工具标签 -->
    <div class="selected-tools">
      <span
        v-for="toolId in selectedTools"
        :key="toolId"
        class="tool-chip"
        :class="{ 'tool-chip-parse': toolId === 'parseFile' }"
        @click.stop="toggleTool(toolId)"
      >
        {{ getToolName(toolId) }}
        <button class="tool-chip-close" @click.stop="deselectTool(toolId)" title="取消选择">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="12" height="12">
            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
        </button>
      </span>
    </div>

    <!-- 打开面板按钮 -->
    <button class="tool-toggle-btn" @click="togglePanel" title="选择工具">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
        <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
      </svg>
    </button>

    <!-- 工具选择面板 -->
    <transition name="slide-up">
      <div v-if="isOpen" class="tool-panel">
        <div class="tool-panel-header">
          <span class="tool-panel-title">选择工具</span>
          <span class="tool-panel-hint">指定本次对话使用的工具</span>
        </div>

        <div class="tool-list">
          <div
            v-for="tool in tools"
            :key="tool.id"
            class="tool-item"
            :class="{ selected: isToolSelected(tool.id) }"
            @click="toggleTool(tool.id)"
          >
            <div class="tool-info">
              <div class="tool-name">{{ tool.name }}</div>
              <div class="tool-desc">{{ tool.description }}</div>
            </div>
            <div v-if="isToolSelected(tool.id)" class="tool-selected-mark">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
              </svg>
            </div>
            <button
              v-if="isToolSelected(tool.id)"
              class="tool-deselect-btn"
              @click.stop="deselectTool(tool.id)"
              title="取消选择"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
                <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>
        </div>

        <div class="tool-panel-footer">
          <button class="tool-btn clear-btn" @click="clearAll">清除全部</button>
          <button class="tool-btn confirm-btn" @click="confirmSelection">确认</button>
        </div>
      </div>
    </transition>

    <!-- 文件上传弹窗（选择 parseFile 工具时弹出） -->
    <Teleport to="body">
      <transition name="modal-fade">
        <div v-if="showUploadModal" class="upload-modal-overlay" @click.self="cancelUpload">
          <div class="upload-modal">
            <div class="upload-modal-header">
              <h3 class="upload-modal-title">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                  <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z"/>
                </svg>
                上传文件并解析
              </h3>
              <button class="upload-modal-close" @click="cancelUpload" :disabled="uploading">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                  <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
              </button>
            </div>

            <!-- 未上传状态 -->
            <div v-if="!uploading && !parsedContent" class="upload-modal-body">
              <div
                class="upload-drop-zone"
                :class="{ 'drag-over': isDragOver }"
                @drop.prevent="handleDrop"
                @dragover.prevent="isDragOver = true"
                @dragleave.prevent="isDragOver = false"
              >
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".pdf,.doc,.docx,.txt,.md"
                  @change="handleFileSelect"
                  style="display: none"
                />
                <div class="upload-drop-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="40" height="40">
                    <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                  </svg>
                </div>
                <p class="upload-drop-text">拖拽文件到此处，或<span class="upload-link" @click.stop="triggerFileInput">点击选择文件</span></p>
                <p class="upload-drop-hint">支持 PDF、Word (.doc/.docx)、TXT、Markdown 格式，单个文件最大 10MB</p>
              </div>
            </div>

            <!-- 上传中状态 -->
            <div v-if="uploading" class="upload-modal-body">
              <div class="upload-progress-content">
                <div class="progress-spinner"></div>
                <p class="progress-file-name">{{ uploadingFileName }}</p>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
                </div>
                <p class="progress-percent">{{ uploadProgress }}%</p>
              </div>
              <button class="cancel-upload-btn" @click="cancelUpload">取消上传</button>
            </div>

            <!-- 解析完成状态 -->
            <div v-if="parsedContent && !uploading" class="upload-modal-body">
              <div class="parse-success">
                <div class="parse-success-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="36" height="36">
                    <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                  </svg>
                </div>
                <p class="parse-file-name">{{ parsedFileName }}</p>
                <p class="parse-char-count">{{ parsedCharCount }} 字符</p>
                <div class="parse-preview">
                  <pre>{{ parsedContent.substring(0, 500) }}{{ parsedContent.length > 500 ? '\n\n...' : '' }}</pre>
                </div>
              </div>
              <div class="parse-actions">
                <button class="btn-clear-file" @click="clearParsedFile">清除文件</button>
                <button class="btn-confirm-file" @click="confirmFileParsed">确认使用此文件</button>
              </div>
            </div>

            <!-- 错误状态 -->
            <div v-if="uploadError" class="upload-modal-body">
              <div class="upload-error">
                <div class="upload-error-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="36" height="36">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 11c-.55 0-1-.45-1-1V8c0-.55.45-1 1-1s1 .45 1 1v4c0 .55-.45 1-1 1zm1 4h-2v-2h2v2z"/>
                  </svg>
                </div>
                <p class="upload-error-text">{{ uploadError }}</p>
                <button class="btn-retry" @click="retryUpload">重试</button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  selectedTools: { type: Array, default: () => [] }
})

const emit = defineEmits(['change', 'file-parsed'])

const isOpen = ref(false)
const fileInputRef = ref(null)
const abortController = ref(null)

const tools = ref([
  { id: 'readFile', name: '文件读取', description: '读取服务器上的文件内容' },
  { id: 'writeFile', name: '文件写入', description: '将内容写入到服务器文件' },
  { id: 'searchWeb', name: '网页搜索', description: '使用搜索引擎查找信息' },
  { id: 'scrapeWebPage', name: '网页抓取', description: '获取并解析网页内容' },
  { id: 'downloadResource', name: '资源下载', description: '从URL下载文件到本地' },
  { id: 'executeTerminalCommand', name: '终端操作', description: '在服务器上执行命令行指令' },
  { id: 'generatePDF', name: 'PDF生成', description: '将内容生成为PDF文档' },
  { id: 'parseFile', name: '文件解析', description: '解析并提取文件中的文本内容，支持 txt、md、pdf、doc、docx 格式' }
])

// 文件上传相关状态
const showUploadModal = ref(false)
const isDragOver = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadingFileName = ref('')
const uploadError = ref('')
const parsedFileName = ref('')
const savedFileName = ref('')
const savedFilePath = ref('')
const parsedContent = ref('')
const parsedCharCount = ref(0)
const pendingParseFile = ref(false)

// 上次选中的 parseFile 状态，用于 deselection 时清理
watch(() => props.selectedTools, (newTools, oldTools) => {
  const wasParseSelected = oldTools.includes('parseFile')
  const isParseSelected = newTools.includes('parseFile')

  if (isParseSelected && !wasParseSelected) {
    // parseFile 刚被选中 → 弹出上传弹窗
    if (!parsedContent.value) {
      pendingParseFile.value = true
      showUploadModal.value = true
    }
  } else if (!isParseSelected && wasParseSelected) {
    // parseFile 被取消选中 → 清理已解析的文件内容
    clearParsedFileSilent()
  }
})

function getToolName(toolId) {
  const tool = tools.value.find(t => t.id === toolId)
  return tool ? tool.name : toolId
}

function togglePanel() {
  isOpen.value = !isOpen.value
}

function isToolSelected(toolId) {
  return props.selectedTools.includes(toolId)
}

function toggleTool(toolId) {
  const current = [...props.selectedTools]
  const index = current.indexOf(toolId)
  if (index > -1) {
    current.splice(index, 1)
  } else {
    current.push(toolId)
  }
  emit('change', current)
}

function deselectTool(toolId) {
  const current = props.selectedTools.filter(id => id !== toolId)
  emit('change', current)
}

function clearAll() {
  clearParsedFileSilent()
  emit('change', [])
}

function confirmSelection() {
  isOpen.value = false
}

// ========== 文件上传相关 ==========

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(e) {
  const files = e.target.files
  if (files.length > 0) {
    startUpload(files[0])
  }
  e.target.value = ''
}

function handleDrop(e) {
  isDragOver.value = false
  const files = e.dataTransfer.files
  if (files.length > 0) {
    const file = files[0]
    const allowedExts = ['pdf', 'doc', 'docx', 'txt', 'md']
    const ext = file.name.split('.').pop().toLowerCase()
    if (!allowedExts.includes(ext)) {
      uploadError.value = `不支持的文件格式：.${ext}，请选择 PDF、Word、TXT、Markdown 文件`
      return
    }
    startUpload(file)
  }
}

async function startUpload(file) {
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    uploadError.value = '文件大小超过限制（最大 10MB）'
    return
  }

  uploadError.value = ''
  uploading.value = true
  uploadingFileName.value = file.name
  uploadProgress.value = 0

  const formData = new FormData()
  formData.append('file', file)

  abortController.value = new AbortController()

  try {
    const xhr = new XMLHttpRequest()

    await new Promise((resolve, reject) => {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          uploadProgress.value = Math.round((e.loaded / e.total) * 90) // 90% for upload
        }
      })

      xhr.addEventListener('load', () => {
        if (xhr.status === 200) {
          resolve(JSON.parse(xhr.responseText))
        } else {
          try {
            const err = JSON.parse(xhr.responseText)
            reject(new Error(err.message || `HTTP ${xhr.status}`))
          } catch {
            reject(new Error(`上传失败 (HTTP ${xhr.status})`))
          }
        }
      })

      xhr.addEventListener('error', () => reject(new Error('网络连接失败')))
      xhr.addEventListener('abort', () => reject(new Error('已取消上传')))

      abortController.value.signal.addEventListener('abort', () => {
        xhr.abort()
        reject(new Error('已取消上传'))
      })

      xhr.open('POST', '/api/documents/upload')
      xhr.send(formData)
    })

    if (!xhr.responseText) {
      throw new Error('服务器返回空响应')
    }

    const uploadResult = JSON.parse(xhr.responseText)
    if (!uploadResult.success) {
      throw new Error(uploadResult.message || '上传失败')
    }

    const uniqueFileName = uploadResult.data?.uniqueFileName
    const fullPath = uploadResult.data?.fullPath

    if (!uniqueFileName) {
      throw new Error('无法获取上传后的文件名')
    }

    savedFileName.value = uniqueFileName
    savedFilePath.value = fullPath || uniqueFileName

    // 阶段2: 调用解析接口
    uploadProgress.value = 95
    const parseUrl = `/api/documents/parse/${encodeURIComponent(uniqueFileName)}`
    const parseResponse = await fetch(parseUrl)

    if (!parseResponse.ok) {
      throw new Error('文件解析请求失败 (HTTP ' + parseResponse.status + ')')
    }

    const parseResult = await parseResponse.json()
    if (!parseResult.success) {
      throw new Error(parseResult.message || '文件解析失败')
    }

    uploadProgress.value = 100
    parsedFileName.value = file.name
    parsedContent.value = parseResult.data.content || ''
    parsedCharCount.value = parseResult.data.charCount || 0

  } catch (error) {
    if (error.message === '已取消上传') {
      uploadError.value = ''
    } else {
      uploadError.value = error.message || '上传失败，请重试'
    }
  } finally {
    uploading.value = false
    uploadProgress.value = uploadProgress.value === 100 ? 100 : 0
    uploadingFileName.value = ''
    abortController.value = null
  }
}

function cancelUpload() {
  if (uploading.value && abortController.value) {
    abortController.value.abort()
  }
  showUploadModal.value = false
  uploadError.value = ''

  if (pendingParseFile.value && !parsedContent.value) {
    // 用户取消但还没有解析内容 → 取消选中 parseFile 工具
    pendingParseFile.value = false
    const current = props.selectedTools.filter(id => id !== 'parseFile')
    emit('change', current)
  }
}

function clearParsedFile() {
  clearParsedFileSilent()
  uploadError.value = ''
  showUploadModal.value = false
}

function clearParsedFileSilent() {
  parsedFileName.value = ''
  savedFileName.value = ''
  savedFilePath.value = ''
  parsedContent.value = ''
  parsedCharCount.value = 0
  uploadError.value = ''
  pendingParseFile.value = false
}

function confirmFileParsed() {
  showUploadModal.value = false
  pendingParseFile.value = false
  emit('file-parsed', {
    fileName: savedFileName.value || parsedFileName.value,
    fullPath: savedFilePath.value || '',
    content: parsedContent.value,
    charCount: parsedCharCount.value
  })
}

function retryUpload() {
  uploadError.value = ''
}
</script>

<style scoped>
.tool-selector {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ====== 已选工具标签 ====== */
.selected-tools {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  max-width: 260px;
  overflow-x: auto;
  scrollbar-width: none;
}

.selected-tools::-webkit-scrollbar {
  display: none;
}

.tool-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.tool-chip:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.tool-chip-parse {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
  background: var(--bg-active);
}

.tool-chip-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
}

.tool-chip-close:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

/* ====== 打开面板按钮 ====== */
.tool-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.tool-toggle-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.tool-toggle-btn:active {
  transform: scale(0.9);
}

/* ====== 工具选择面板（下拉） ====== */
.tool-panel {
  position: absolute;
  bottom: calc(100% + 8px);
  left: -8px;
  width: 300px;
  max-height: 420px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 300;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.tool-panel-header {
  padding: 12px 14px 8px;
  border-bottom: 1px solid var(--border-primary);
  flex-shrink: 0;
}

.tool-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  display: block;
}

.tool-panel-hint {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
  display: block;
}

.tool-list {
  padding: 6px;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.tool-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  margin-bottom: 2px;
  border: 1px solid transparent;
}

.tool-item:last-child {
  margin-bottom: 0;
}

.tool-item:hover {
  background: var(--bg-hover);
}

.tool-item.selected {
  background: var(--bg-active);
  border-color: var(--accent-primary);
}

.tool-info {
  flex: 1;
  min-width: 0;
}

.tool-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
}

.tool-item.selected .tool-name {
  color: var(--accent-primary);
}

.tool-desc {
  font-size: 10px;
  color: var(--text-tertiary);
  margin-top: 1px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-selected-mark {
  color: var(--accent-primary);
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.tool-deselect-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.tool-deselect-btn:hover {
  color: #ea4335;
  background: rgba(234, 67, 53, 0.1);
}

.tool-panel-footer {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding: 8px 12px;
  border-top: 1px solid var(--border-primary);
  background: var(--bg-secondary);
  flex-shrink: 0;
}

.tool-btn {
  padding: 5px 14px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.clear-btn {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-primary);
}

.clear-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.confirm-btn {
  background: var(--accent-primary);
  color: #fff;
}

.confirm-btn:hover {
  opacity: 0.9;
}

/* ====== 过渡动画 ====== */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.2s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

/* ====== 文件上传弹窗 ====== */
.upload-modal-overlay {
  position: fixed;
  inset: 0;
  background: var(--overlay-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.upload-modal {
  background: var(--bg-elevated);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg), 0 0 0 1px var(--border-primary);
  width: 460px;
  max-width: 92vw;
  max-height: 88vh;
  overflow-y: auto;
  position: relative;
  z-index: 10000;
}

.upload-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 14px;
  border-bottom: 1px solid var(--border-secondary);
  background: var(--bg-elevated);
  position: sticky;
  top: 0;
}

.upload-modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.upload-modal-title svg {
  color: var(--accent-primary);
}

.upload-modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.upload-modal-close:hover:not(:disabled) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.upload-modal-close:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.upload-modal-body {
  padding: 28px 24px;
}

/* 拖拽区域 */
.upload-drop-zone {
  border: 2px dashed var(--border-secondary);
  border-radius: var(--radius-md);
  padding: 44px 28px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--bg-secondary);
}

.upload-drop-zone:hover,
.upload-drop-zone.drag-over {
  border-color: var(--accent-primary);
  background: var(--bg-active);
}

.upload-drop-zone.drag-over {
  transform: scale(1.02);
}

.upload-drop-icon {
  color: var(--text-tertiary);
  margin-bottom: 14px;
  transition: color var(--transition-fast);
}

.upload-drop-zone:hover .upload-drop-icon,
.upload-drop-zone.drag-over .upload-drop-icon {
  color: var(--accent-primary);
}

.upload-drop-text {
  font-size: 15px;
  color: var(--text-primary);
  margin: 0 0 10px;
  line-height: 1.5;
}

.upload-link {
  color: var(--accent-primary);
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.upload-link:hover {
  color: var(--accent-hover);
}

.upload-drop-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  margin: 0;
  line-height: 1.4;
}

/* 上传进度 */
.upload-progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;
}

.progress-spinner {
  width: 48px;
  height: 48px;
  border: 3px solid var(--border-secondary);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.progress-file-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
  margin: 0;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-bar {
  width: 100%;
  max-width: 320px;
  height: 6px;
  background: var(--bg-tertiary);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), var(--accent-hover));
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-percent {
  font-size: 14px;
  color: var(--accent-primary);
  font-weight: 600;
  margin: 0;
}

.cancel-upload-btn {
  display: block;
  margin: 20px auto 0;
  padding: 8px 22px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.cancel-upload-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
  border-color: var(--accent-primary);
}

/* 解析成功 */
.parse-success {
  text-align: center;
}

.parse-success-icon {
  color: #34a853;
  margin-bottom: 10px;
}

.parse-file-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.parse-char-count {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0 0 14px;
}

.parse-preview {
  max-height: 180px;
  overflow-y: auto;
  background: var(--bg-secondary);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-md);
  padding: 12px 14px;
  text-align: left;
}

.parse-preview pre {
  margin: 0;
  font-size: 13px;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  line-height: 1.6;
}

.parse-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.btn-clear-file {
  padding: 8px 20px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-clear-file:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.btn-confirm-file {
  padding: 8px 22px;
  border-radius: var(--radius-full);
  border: none;
  background: var(--accent-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-confirm-file:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

/* 错误状态 */
.upload-error {
  text-align: center;
  padding: 20px 0;
}

.upload-error-icon {
  color: #ea4335;
  margin-bottom: 10px;
}

.upload-error-text {
  font-size: 14px;
  color: var(--text-primary);
  margin: 0 0 18px;
  line-height: 1.5;
}

.btn-retry {
  padding: 8px 24px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-secondary);
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-retry:hover {
  background: var(--bg-tertiary);
  border-color: var(--accent-primary);
}

/* 弹窗过渡 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-active .upload-modal,
.modal-fade-leave-active .upload-modal {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .upload-modal,
.modal-fade-leave-to .upload-modal {
  transform: scale(0.95);
  opacity: 0;
}
</style>
