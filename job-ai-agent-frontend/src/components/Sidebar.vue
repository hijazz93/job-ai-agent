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
          <button class="history-item-delete" @click.stop="$emit('delete-chat', chat.id)" title="删除对话">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </nav>

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
defineProps({
  isOpen: { type: Boolean, default: true },
  chats: { type: Array, default: () => [] },
  activeId: { type: String, default: '' }
})

defineEmits(['close', 'new-chat', 'select-chat', 'delete-chat'])
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
