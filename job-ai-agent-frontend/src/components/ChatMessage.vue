<template>
  <div :class="['message', message.role === 'user' ? 'user-message' : 'ai-message']">
    <div class="message-avatar">
      <div v-if="message.role === 'user'" class="avatar user-avatar">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
        </svg>
      </div>
      <div v-else class="avatar ai-avatar">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <path d="M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-.1-2.73 2.71-2.73 7.08 0 9.79s7.15 2.71 9.88 0C18.32 15.65 19 14.08 19 12.1h2c0 1.98-.88 4.55-2.64 6.29-3.51 3.48-9.21 3.48-12.72 0-3.5-3.47-3.53-9.11-.02-12.58s9.14-3.47 12.65 0L21 3v7.12z"/>
        </svg>
      </div>
    </div>
    <div class="message-content">
      <div v-if="message.isStep" class="step-header" @click="toggleCollapse">
        <div class="step-info">
          <span class="step-badge">步骤 {{ message.step }}</span>
          <span class="step-summary">{{ stepSummary }}</span>
        </div>
        <div class="step-toggle">
          <svg v-if="isCollapsed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
            <path d="M7 10l5 5 5-5z"/>
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
            <path d="M7 14l5-5 5 5z"/>
          </svg>
        </div>
      </div>
      <div v-if="!message.isStep || !isCollapsed" class="message-text" v-html="formattedContent"></div>
      <div v-if="message.isStep && isCollapsed" class="collapsed-hint">点击展开查看详情</div>
      <div v-if="pdfFiles.length > 0" class="pdf-actions">
        <div v-for="pdf in pdfFiles" :key="pdf.path" class="pdf-item">
          <span class="pdf-name">{{ pdf.name }}</span>
          <div class="pdf-buttons">
            <a :href="pdf.previewUrl" target="_blank" class="pdf-btn preview">预览</a>
            <a :href="pdf.downloadUrl" class="pdf-btn download">下载</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const isCollapsed = ref(true)

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const formattedContent = computed(() => {
  let content = props.message.content || ''
  content = content.replace(/\n/g, '<br>')
  content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
  content = content.replace(/\*(.*?)\*/g, '<em>$1</em>')
  content = content.replace(/`(.*?)`/g, '<code>$1</code>')
  return content
})

const stepSummary = computed(() => {
  const content = props.message.content || ''
  // 提取工具调用或关键动作的摘要
  // 匹配模式: 使用 [工具], 调用 [工具], 搜索..., 生成...
  const toolPatterns = [
    /(?:使用|调用|执行)([\w]+)工具/i,
    /(?:正在搜索|搜索关键词):\s*(.+)/i,
    /(?:正在生成|生成)(?:PDF|文件|结果)/i,
    /(?:思考|观察|结果):\s*(.+)/i,
    /(?:读取|写入|下载)(?:到了|成功):\s*(.+)/i,
  ]
  for (const pattern of toolPatterns) {
    const match = content.match(pattern)
    if (match) {
      const summary = match[1] || match[0]
      return summary.length > 40 ? summary.substring(0, 40) + '...' : summary
    }
  }
  // 如果没有匹配到模式，返回内容的前50个字符
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
      name,
      path,
      previewUrl: `/api/files/pdf/${encodeURIComponent(name)}`,
      downloadUrl: `/api/files/pdf/${encodeURIComponent(name)}/download`
    }
  })
})
</script>

<style scoped>
.message {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-radius: 16px;
  max-width: 100%;
}

.user-message {
  flex-direction: row-reverse;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.ai-message {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar svg {
  width: 24px;
  height: 24px;
}

.user-avatar {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  color: #e0e0e0;
  line-height: 1.6;
  font-size: 15px;
  word-wrap: break-word;
}

.user-message .message-text {
  color: white;
}

.message-text :deep(code) {
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.message-text :deep(strong) {
  color: #fff;
}

.step-badge {
  display: inline-block;
  padding: 4px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;
  margin-bottom: 8px;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 8px 12px;
  background: rgba(102, 126, 234, 0.15);
  border-radius: 10px;
  margin-bottom: 10px;
  transition: background 0.2s;
}

.step-header:hover {
  background: rgba(102, 126, 234, 0.25);
}

.step-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.step-summary {
  font-size: 13px;
  color: #b0b0b0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-toggle {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #888;
}

.step-toggle svg {
  width: 20px;
  height: 20px;
}

.collapsed-hint {
  font-size: 12px;
  color: #666;
  text-align: center;
  padding: 4px;
  margin-bottom: 8px;
}

.pdf-actions {
  margin-top: 14px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.07);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(6px);
}

.pdf-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.pdf-item:not(:last-child) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.pdf-name {
  font-size: 14px;
  color: #e0e0e0;
  font-weight: 500;
}

.pdf-buttons {
  display: flex;
  gap: 10px;
}

/* 高级按钮样式 */
.pdf-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.25s ease;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}

/* 预览按钮 */
.pdf-btn.preview {
  background: rgba(46, 204, 113, 0.15);
  color: #2ecc71;
  border: 1px solid rgba(46, 204, 113, 0.3);
}

/* 下载按钮 */
.pdf-btn.download {
  background: rgba(52, 152, 219, 0.15);
  color: #3498db;
  border: 1px solid rgba(52, 152, 219, 0.3);
}

/* 悬停效果 */
.pdf-btn.preview:hover {
  background: rgba(46, 204, 113, 0.25);
  transform: translateY(-1px);
}

.pdf-btn.download:hover {
  background: rgba(52, 152, 219, 0.25);
  transform: translateY(-1px);
}
</style>