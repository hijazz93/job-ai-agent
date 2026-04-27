<template>
  <div v-if="files.length > 0" class="file-attachment-list">
    <div v-for="file in files" :key="file.id" class="file-chip">
      <div class="file-chip-icon">
        <svg v-if="isPdf(file.originalFileName)" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
          <path d="M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-1.5v2H13V7h1.5c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z"/>
        </svg>
        <svg v-else-if="isDoc(file.originalFileName)" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>
        </svg>
        <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="14" height="14">
          <path d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        </svg>
      </div>
      <span class="file-chip-name">{{ file.originalFileName }}</span>
      <span class="file-chip-size" v-if="file.charCount">{{ formatSize(file.charCount) }}</span>
      <button class="file-chip-remove" @click="$emit('remove', file.id)" title="移除文件">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="12" height="12">
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  files: { type: Array, default: () => [] }
})

defineEmits(['remove'])

function isPdf(name) {
  return name && name.toLowerCase().endsWith('.pdf')
}

function isDoc(name) {
  return name && /\.(doc|docx)$/i.test(name)
}

function formatSize(chars) {
  if (chars >= 1000) return (chars / 1000).toFixed(1) + 'k 字符'
  return chars + ' 字符'
}
</script>

<style scoped>
.file-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 4px 10px;
  justify-content: center;
}

.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-full);
  font-size: 12px;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.file-chip-icon {
  color: var(--accent-primary);
  display: flex;
  align-items: center;
}

.file-chip-name {
  color: var(--text-primary);
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-chip-size {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.file-chip-remove {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}

.file-chip-remove:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}
</style>
