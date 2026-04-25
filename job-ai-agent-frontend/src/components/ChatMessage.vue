<template>
  <div :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
    <div class="message-inner">
      <div class="message-avatar">
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
        <div v-if="!message.isStep || !isCollapsed" class="message-text" v-html="formattedContent"></div>
        <div v-if="message.isStep && isCollapsed" class="collapsed-hint">点击展开查看详情</div>

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

        <div v-if="pdfFiles.length > 0" class="pdf-actions">
          <div v-for="pdf in pdfFiles" :key="pdf.path" class="pdf-item">
            <div class="pdf-info">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
                <path d="M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-1.5v2H13V7h1.5c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z"/>
              </svg>
              <span class="pdf-name">{{ pdf.name }}</span>
            </div>
            <div class="pdf-buttons">
              <a :href="pdf.previewUrl" target="_blank" class="pdf-btn preview">预览</a>
              <a :href="pdf.downloadUrl" class="pdf-btn download">下载</a>
            </div>
          </div>
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
const copied = ref(false)

const showActions = computed(() => {
  return props.message.role === 'assistant' && props.message.content && !props.message.isStep
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

const pdfFiles = computed(() => {
  const content = props.message.content || ''
  const pdfRegex = /PDF generated successfully to:.*?(tmp[/\\]pdf[/\\][^\s]+\.pdf)/g
  const matches = [...content.matchAll(pdfRegex)]
  return matches.map(match => {
    const path = match[1].replace(/\\/g, '/')
    const name = path.split('/').pop()
    return {
      name, path,
      previewUrl: `/api/files/pdf/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/pdf/${encodeURIComponent(name)}/download`
    }
  })
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

.pdf-actions {
  margin-top: 12px;
  padding: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
}

.pdf-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.pdf-item:not(:last-child) {
  border-bottom: 1px solid var(--border-primary);
}

.pdf-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-secondary);
}

.pdf-name {
  font-size: 14px;
  font-weight: 500;
}

.pdf-buttons {
  display: flex;
  gap: 8px;
}

.pdf-btn {
  padding: 6px 14px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all var(--transition-fast);
}

.pdf-btn.preview {
  background: rgba(52, 168, 83, 0.12);
  color: #34a853;
}

.pdf-btn.preview:hover {
  background: rgba(52, 168, 83, 0.22);
}

.pdf-btn.download {
  background: rgba(26, 115, 232, 0.12);
  color: var(--accent-primary);
}

.pdf-btn.download:hover {
  background: rgba(26, 115, 232, 0.22);
}

@media (max-width: 768px) {
  .message-inner {
    gap: 10px;
  }
  .message-text {
    font-size: 14px;
  }
}
</style>
