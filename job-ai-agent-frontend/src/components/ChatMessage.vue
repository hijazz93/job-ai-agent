<template>
  <div :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
    <div class="message-inner">
      <div v-if="!message.isStep || message.step === 1" class="message-avatar">
        <div v-if="message.role === 'user'" class="avatar user-avatar">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </div>
        <div v-else class="avatar ai-avatar">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"/>
          </svg>
        </div>
      </div>
      <div class="message-body">
        <div v-if="message.isStep" class="step-header" @click="toggleCollapse">
          <div class="step-info">
            <span class="step-badge">步骤 {{ message.step }}</span>
            <span class="step-summary">{{ stepSummary }}</span>
          </div>
          <div class="step-toggle">
            <svg v-if="isCollapsed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
              <path d="M7 10l5 5 5-5z"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
              <path d="M7 14l5-5 5 5z"/>
            </svg>
          </div>
        </div>
        <div v-if="!message.isStep && !message.isFinal" class="message-text" v-html="formattedContent"></div>

        <!-- 执行过程日志（可折叠） -->
        <div v-if="message.processSteps && message.processSteps.length > 0" class="process-log">
          <div class="process-log-header" @click="toggleProcessLog">
            <div class="process-log-title">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
              </svg>
              <span>查看执行过程（{{ message.processSteps.length }} 步）</span>
            </div>
            <svg v-if="!processLogExpanded" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18" class="process-log-arrow">
              <path d="M7 10l5 5 5-5z"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18" class="process-log-arrow">
              <path d="M7 14l5-5 5 5z"/>
            </svg>
          </div>
          <div v-if="processLogExpanded" class="process-log-steps">
            <div v-for="(step, index) in message.processSteps" :key="index" class="process-log-step">
              <span class="process-log-step-num">{{ index + 1 }}</span>
              <pre class="process-log-step-content">{{ step }}</pre>
            </div>
          </div>
        </div>

        <div v-if="showActions && message.role === 'assistant'" class="message-actions">
          <button class="msg-action-btn" @click="copyContent" :title="copied ? '已复制' : '复制'">
            <svg v-if="!copied" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </button>
        </div>

        <div v-if="files.length > 0" class="file-actions">
          <div v-for="file in files" :key="file.path" class="file-item">
            <div class="file-info">
              <svg v-if="file.type === 'pdf'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-1.5v2H13V7h1.5c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z"/>
              </svg>
              <svg v-else-if="file.type === 'txt'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h8c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
              </svg>
              <svg v-else-if="file.type === 'image'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
              </svg>
              <span class="file-name">{{ file.name }}</span>
            </div>
            <div class="file-buttons">
              <a v-if="file.type === 'pdf'" :href="file.previewUrl" target="_blank" class="file-btn preview">预览</a>
              <a v-else-if="file.type === 'txt'" :href="file.previewUrl" target="_blank" class="file-btn preview">预览</a>
              <a v-else-if="file.type === 'image'" :href="file.previewUrl" target="_blank" class="file-btn preview">预览</a>
              <a :href="file.downloadUrl" class="file-btn download">下载</a>
            </div>
          </div>
        </div>

        <div v-if="message.isFinal" class="final-answer">
          <div class="final-answer-title">最终回答</div>
          <div class="final-answer-content" v-html="formattedContent"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  message: { type: Object, required: true }
})

const isCollapsed = ref(true)
const processLogExpanded = ref(false)
const copied = ref(false)

function toggleProcessLog() {
  processLogExpanded.value = !processLogExpanded.value
}

const showActions = computed(() => {
  return props.message.role === 'assistant' && props.message.content && !props.message.isStep && !props.message.isFinal
})

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value
}

function copyContent() {
  const text = props.message.content || ''
  navigator.clipboard.writeText(text).then(() => {
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }).catch(() => {})
}

const formattedContent = computed(() => {
  let content = props.message.content || ''
  content = content.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  content = content.replace(/```(\w+)?\n?([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>')
  content = content.replace(/`([^`]+)`/g, '<code>$1</code>')
  content = content.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  content = content.replace(/\*(.+?)\*/g, '<em>$1</em>')
  content = content.replace(/\n/g, '<br>')
  return content
})

const stepSummary = computed(() => {
  const content = props.message.content || ''
  const patterns = [
    /(?:使用|调用|执行)([\w]+)工具/i,
    /(?:正在搜索|搜索关键词):\s*(.+)/i,
    /(?:正在生成|生成)(?:PDF|文件|结果)/i,
    /(?:思考|观察|结果):\s*(.+)/i,
    /(?:读取|写入|下载)(?:到了|成功):\s*(.+)/i,
  ]
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match) {
      const summary = match[1] || match[0]
      return summary.length > 40 ? summary.substring(0, 40) + '...' : summary
    }
  }
  const firstLine = content.split('\n')[0]
  return firstLine.length > 50 ? firstLine.substring(0, 50) + '...' : firstLine || '执行中...'
})

const files = computed(() => {
  const content = props.message.content || ''
  const files = []
  
  // PDF files
  const pdfRegex = /PDF generated successfully to:.*?(tmp[/\\]pdf[/\\][^\s]+\.pdf)/g
  const pdfMatches = [...content.matchAll(pdfRegex)]
  pdfMatches.forEach(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    files.push({
      name, path,
      type: 'pdf',
      previewUrl: `/api/files/pdf/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/pdf/${encodeURIComponent(name)}/download`
    })
  })
  
  // Text files (from txt tool)
  const txtRegex = /File written successfully to:.*?(tmp[/\\]txt[/\\][^\s]+\.(txt|md|json|csv|xml|html|css|js|py|java|log))/gi
  const txtMatches = [...content.matchAll(txtRegex)]
  txtMatches.forEach(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    files.push({
      name, path,
      type: 'txt',
      previewUrl: `/api/files/txt/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/txt/${encodeURIComponent(name)}/download`
    })
  })
  
  // Text files (from writeFile tool - /tmp/file/ directory)
  const fileRegex = /File written successfully to:.*?(tmp[/\\]file[/\\][^\s]+\.(txt|md|json|csv|xml|html|css|js|py|java|log|markdown))/gi
  const fileMatches = [...content.matchAll(fileRegex)]
  fileMatches.forEach(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    files.push({
      name, path,
      type: 'txt',
      previewUrl: `/api/files/file/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/file/${encodeURIComponent(name)}/download`
    })
  })
  
  // Image files from download tool
  const downloadImgRegex = /Resource downloaded successfully to:.*?(tmp[/\\]download[/\\][^\s]+\.(jpg|jpeg|png|gif|webp))/g
  const downloadImgMatches = [...content.matchAll(downloadImgRegex)]
  downloadImgMatches.forEach(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    files.push({
      name, path,
      type: 'image',
      previewUrl: `/api/files/download/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/download/${encodeURIComponent(name)}/download`
    })
  })
  
  // Image files from other tools
  const imgRegex = /File written successfully to:.*?(tmp[/\\]images[/\\][^\s]+\.(jpg|jpeg|png|gif|webp))/g
  const imgMatches = [...content.matchAll(imgRegex)]
  imgMatches.forEach(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    files.push({
      name, path,
      type: 'image',
      previewUrl: `/api/files/images/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/images/${encodeURIComponent(name)}/download`
    })
  })
  
  return files
})
</script>

<style scoped>
.message {
  padding: 16px 0;
}

.message-inner {
  display: flex;
  gap: 16px;
  max-width: 780px;
  margin: 0 auto;
  width: 100%;
  padding: 0 8px;
}

.user-message .message-inner {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  padding-top: 2px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar {
  background: var(--accent-primary);
  color: #fff;
}

.ai-avatar {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.message-body {
  flex: 1;
  min-width: 0;
}

.user-message .message-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-text {
  color: var(--text-primary);
  line-height: 1.7;
  font-size: 15px;
  word-wrap: break-word;
}

.user-message .message-text {
  background: var(--user-bubble-bg);
  color: var(--user-bubble-text);
  padding: 12px 18px;
  border-radius: var(--radius-lg);
  border-top-right-radius: 4px;
  max-width: fit-content;
}

.message-text :deep(pre) {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  padding: 16px;
  overflow-x: auto;
  margin: 12px 0;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.message-text :deep(code) {
  background: var(--bg-active);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Cascadia Code', monospace;
  font-size: 13px;
}

.message-text :deep(pre code) {
  background: transparent;
  padding: 0;
}

.message-text :deep(strong) {
  font-weight: 600;
}

.message-text :deep(em) {
  font-style: italic;
}

.message-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.msg-action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.msg-action-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 10px 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  margin-bottom: 10px;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.step-header:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.step-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.step-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  background: var(--accent-primary);
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.step-summary {
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-toggle {
  flex-shrink: 0;
  color: var(--text-tertiary);
}

.collapsed-hint {
  font-size: 12px;
  color: var(--text-disabled);
  padding: 4px 0;
}

.file-actions {
  margin-top: 12px;
  padding: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.file-item:not(:last-child) {
  border-bottom: 1px solid var(--border-primary);
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
}

.file-name {
  font-size: 14px;
  font-weight: 500;
}

.file-buttons {
  display: flex;
  gap: 8px;
}

.file-btn {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--transition-fast);
}

.file-btn.preview {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.file-btn.preview:hover {
  background: rgba(52, 168, 83, 0.22);
}

.file-btn.download {
  background: rgba(26, 115, 232, 0.12);
  color: var(--accent-primary);
}

.file-btn.download:hover {
  background: rgba(26, 115, 232, 0.22);
}

.final-answer {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
}

.final-answer-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.final-answer-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* 执行过程日志 */
.process-log {
  margin-top: 16px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.process-log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--bg-card);
  cursor: pointer;
  transition: background var(--transition-fast);
  user-select: none;
}

.process-log-header:hover {
  background: var(--bg-card-hover);
}

.process-log-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.process-log-arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.process-log-steps {
  border-top: 1px solid var(--border-primary);
  max-height: 360px;
  overflow-y: auto;
}

.process-log-step {
  display: flex;
  gap: 10px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--border-primary);
}

.process-log-step:last-child {
  border-bottom: none;
}

.process-log-step-num {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.process-log-step-content {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .message-inner {
    gap: 10px;
  }
  .message-text {
    font-size: 14px;
  }
  .final-answer {
    padding: 12px;
  }
}
</style>
