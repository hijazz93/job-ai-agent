<template>
  <div class="sidebar-wrapper">
    <div v-if="isOpen" class="sidebar-overlay" @click="$emit('close')"></div>
    <aside class="sidebar" :class="{ open: isOpen }">
      <div class="sidebar-top">
        <button class="new-chat-btn" @click="$emit('new-chat')">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
          </svg>
          <span>新对话</span>
        </button>
      </div>

      <nav class="history-list">
        <div class="history-label">最近对话</div>
        <div v-if="chats.length === 0" class="history-empty">
          <span>暂无对话记录</span>
        </div>
        <div
          v-for="chat in chats"
          :key="chat.id"
          class="history-item"
          :class="{ active: activeId === chat.id }"
          @click="$emit('select-chat', chat)"
        >
          <div class="history-item-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
              <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/>
            </svg>
          </div>
          <span class="history-item-title">{{ chat.title }}</span>
          <button class="history-item-delete" @click.stop="confirmDelete(chat.id, chat.title)" title="删除对话">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </nav>

      <div class="knowledge-base-section">
        <button 
          class="knowledge-toggle-btn" 
          :class="{ active: showKnowledgeBase }"
          @click="showKnowledgeBase = !showKnowledgeBase"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
            <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11zM8 15.01l1.41 1.41L11 14.84V19h2v-4.16l1.59 1.59L16 15.01 12.01 12z"/>
          </svg>
          <span>知识库管理</span>
          <svg class="toggle-arrow" :class="{ rotated: showKnowledgeBase }" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M7 10l5 5 5-5z"/>
          </svg>
        </button>
        
        <transition name="slide">
          <div v-if="showKnowledgeBase" class="knowledge-content">
            <DocumentUpload 
              @upload-success="handleUploadSuccess"
              @upload-error="handleUploadError"
            />
          </div>
        </transition>
      </div>

      <div class="sidebar-footer">
        <div class="footer-links">
          <a href="https://gitee.com/hao-jiazhang" target="_blank" class="footer-link">关于</a>
          <span class="footer-dot">·</span>
          <span class="footer-link">帮助</span>
        </div>
        <div class="footer-info">
          <span class="location-indicator">
            <span class="location-dot"></span>
            服务运行中
          </span>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DocumentUpload from './DocumentUpload.vue'

const props = defineProps({
  isOpen: { type: Boolean, default: true },
  chats: { type: Array, default: () => [] },
  activeId: { type: String, default: '' },
  isAuthenticated: { type: Boolean, default: false },
  username: { type: String, default: '' },
  userRole: { type: String, default: '' }
})

const emit = defineEmits(['close', 'new-chat', 'select-chat', 'delete-chat'])

const displayName = computed(() => props.username || '用户')
const roleLabel = computed(() => {
  switch (props.userRole) {
    case 'ADMIN': return '管理员'
    case 'PREMIUM_USER': return '高级用户'
    default: return '普通用户'
  }
})

const showKnowledgeBase = ref(false)

function confirmDelete(chatId, chatTitle) {
  const title = chatTitle || '该对话'
  if (window.confirm(`确定要删除对话「${title}」吗？此操作不可恢复。`)) {
    emit('delete-chat', chatId)
  }
}

function handleUploadSuccess(data) {
  console.log('文档上传成功:', data)
}

function handleUploadError(error) {
  console.error('文档上传失败:', error)
}
</script>

<style scoped>
.sidebar-wrapper {
  position: relative;
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: var(--overlay-bg);
  z-index: 90;
}

.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-primary);
  display: flex;
  flex-direction: column;
  z-index: 100;
  transition: transform var(--transition-slow), background var(--transition-normal), border-color var(--transition-normal);
  transform: translateX(-100%);
}

.sidebar.open {
  transform: translateX(0);
}

.sidebar-top {
  padding: 12px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-primary);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--sidebar-hover);
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.history-label {
  padding: 16px 12px 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.history-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--text-tertiary);
}

.history-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background var(--transition-fast);
  position: relative;
  margin-bottom: 2px;
}

.history-item:hover {
  background: var(--sidebar-hover);
}

.history-item.active {
  background: var(--sidebar-active);
}

.history-item.active .history-item-title {
  color: var(--accent-primary);
}

.history-item-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.history-item-title {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-delete {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  opacity: 0;
  transition: opacity var(--transition-fast), background var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}

.history-item:hover .history-item-delete {
  opacity: 1;
}

.history-item-delete:hover {
  background: rgba(234, 67, 53, 0.12);
  color: #ea4335;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-primary);
}

.knowledge-base-section {
  border-top: 1px solid var(--border-primary);
  padding: 8px;
}

.knowledge-toggle-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border-radius: var(--radius-lg);
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.knowledge-toggle-btn:hover {
  background: var(--sidebar-hover);
  color: var(--text-primary);
}

.knowledge-toggle-btn.active {
  background: rgba(26, 115, 232, 0.08);
  color: var(--accent-primary);
}

.knowledge-toggle-btn svg:first-child {
  color: inherit;
}

.toggle-arrow {
  margin-left: auto;
  transition: transform var(--transition-fast);
  color: var(--text-tertiary);
}

.toggle-arrow.rotated {
  transform: rotate(180deg);
}

.knowledge-content {
  padding: 8px 4px 0;
  overflow-y: auto;
  max-height: calc(100vh - 400px);
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
}

.slide-enter-to,
.slide-leave-from {
  opacity: 1;
  max-height: 600px;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.footer-link {
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--text-secondary);
}

.footer-dot {
  color: var(--text-disabled);
  font-size: 12px;
}

.footer-info {
  display: flex;
  align-items: center;
}

.location-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.location-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34a853;
  box-shadow: 0 0 6px rgba(52, 168, 83, 0.5);
}

@media (max-width: 768px) {
  .sidebar-overlay {
    display: block;
  }
  .sidebar {
    width: 280px;
  }
  .sidebar:not(.open) {
    transform: translateX(-100%);
  }
}

@media (min-width: 769px) {
  .sidebar {
    position: relative;
    height: 100vh;
    width: var(--sidebar-width);
    transition: width var(--transition-slow), border-color var(--transition-normal);
  }
  .sidebar:not(.open) {
    width: 0;
    overflow: hidden;
    border-right: none;
  }
}
</style>
