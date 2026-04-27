<template>
  <div class="chat-input-wrapper">
    <div class="input-container">
      <ToolSelector
        v-if="currentMode === 'agent' && allowedModes.includes('agent')"
        :selected-tools="selectedTools"
        @change="$emit('tools-change', $event)"
        @file-parsed="$emit('file-parsed', $event)"
      />
      <div v-if="currentMode === 'fast'" class="rag-toggle-btn" :class="{ active: ragEnabled }" @click.stop="$emit('toggle-rag')" title="知识库检索">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
          <path d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H8V4h12v12zM10 9h8v2h-8V9zm0 3h4v2h-4v-2zm0-6h8v2h-8V6z"/>
        </svg>
        <span>知识库</span>
      </div>
      <textarea
        ref="textareaRef"
        v-model="inputText"
        @keydown="handleKeydown"
        @input="autoResize"
        :placeholder="placeholder"
        rows="1"
        :disabled="disabled"
      ></textarea>
      <div class="input-actions">
        <div v-if="visibleOptions.length > 1" class="mode-selector" @click.stop="$emit('toggle-mode-dropdown')">
          <div class="mode-dropdown" :class="{ 'open': isModeDropdownOpen }">
            <span class="mode-label">{{ currentModeLabel }}</span>
            <span class="mode-arrow">▼</span>
            <div class="mode-options" v-if="isModeDropdownOpen">
              <div
                v-for="opt in visibleOptions"
                :key="opt.value"
                class="mode-option"
                :class="{ 'active': currentMode === opt.value }"
                @click="$emit('set-mode', opt.value)"
              >
                <span class="mode-title">{{ opt.title }}</span>
                <span class="mode-desc">{{ opt.desc }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="mode-label-static">
          <span class="mode-label">{{ currentModeLabel }}</span>
        </div>
        <button
          class="send-btn"
          @click="handleSend"
          :disabled="disabled || !inputText.trim()"
          :class="{ active: inputText.trim() }"
          title="发送消息"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import ToolSelector from './ToolSelector.vue'

const modeOptions = [
  { value: 'fast', title: 'Fast', desc: 'Answering quickly' },
  { value: 'agent', title: 'Agent', desc: 'Solving complex problems with tools' }
]

const props = defineProps({
  disabled: { type: Boolean, default: false },
  placeholder: { type: String, default: '输入消息...' },
  currentMode: { type: String, default: 'fast' },
  isModeDropdownOpen: { type: Boolean, default: false },
  selectedTools: { type: Array, default: () => [] },
  allowedModes: { type: Array, default: () => ['fast', 'agent'] },
  ragEnabled: { type: Boolean, default: false }
})

const visibleOptions = computed(() => {
  return modeOptions.filter(m => props.allowedModes.includes(m.value))
})

const currentModeLabel = computed(() => {
  const opt = modeOptions.find(m => m.value === props.currentMode)
  return opt ? opt.title : props.currentMode
})

const emit = defineEmits(['send', 'toggle-mode-dropdown', 'set-mode', 'tools-change', 'toggle-rag', 'file-parsed'])

const inputText = ref('')
const textareaRef = ref(null)

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleSend() {
  if (props.disabled || !inputText.value.trim()) return
  emit('send', inputText.value.trim())
  inputText.value = ''
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  })
}

function autoResize() {
  nextTick(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
      textareaRef.value.style.height = Math.min(textareaRef.value.scrollHeight, 160) + 'px'
    }
  })
}
</script>

<style scoped>
.chat-input-wrapper {
  max-width: 780px;
  margin: 0 auto;
  width: 100%;
}

.input-container {
  display: flex;
  align-items: center;
  background: var(--bg-input);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-xl);
  padding: 8px 8px 8px 20px;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), background var(--transition-normal);
  box-shadow: var(--shadow-sm);
}

.input-container:focus-within {
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 1px var(--border-input-focus);
}

textarea {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  max-height: 160px;
  padding: 8px 0;
  font-family: inherit;
}

textarea::placeholder {
  color: var(--text-disabled);
}

textarea:disabled {
  opacity: 0.6;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-left: 4px;
}

.mode-selector {
  position: relative;
  z-index: 100;
}

.mode-dropdown {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
  user-select: none;
  font-size: 13px;
}

.mode-dropdown:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-secondary);
}

.mode-label {
  font-weight: 500;
  color: var(--text-secondary);
}

.mode-arrow {
  font-size: 8px;
  color: var(--text-tertiary);
  transition: transform var(--transition-fast);
}

.mode-dropdown.open .mode-arrow {
  transform: rotate(180deg);
}

.mode-options {
  position: absolute;
  bottom: 100%;
  right: 0;
  margin-bottom: 4px;
  min-width: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  z-index: 101;
}

.mode-option {
  padding: 10px 14px;
  cursor: pointer;
  transition: background var(--transition-fast);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mode-option:hover {
  background: var(--bg-card-hover);
}

.mode-option.active {
  background: var(--bg-active);
  border-left: 3px solid var(--accent-primary);
}

.mode-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.mode-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.3;
}

.rag-toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-tertiary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
  white-space: nowrap;
}

.rag-toggle-btn:hover {
  border-color: var(--border-secondary);
  color: var(--text-secondary);
}

.rag-toggle-btn.active {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
  background: var(--bg-active);
}

.rag-toggle-btn svg {
  flex-shrink: 0;
}

.mode-label-static {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  font-size: 13px;
}

.send-btn {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  transition: background var(--transition-fast), color var(--transition-fast), transform var(--transition-fast);
}

.send-btn.active {
  color: var(--accent-primary);
}

.send-btn:hover:not(:disabled) {
  background: var(--bg-hover);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.92);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .input-container {
    border-radius: var(--radius-lg);
    padding: 6px 6px 6px 14px;
  }
  textarea {
    font-size: 14px;
  }
  .mode-dropdown {
    padding: 4px 8px;
    font-size: 12px;
  }
  .send-btn {
    width: 36px;
    height: 36px;
  }
  .input-actions {
    gap: 4px;
  }
}
</style>
