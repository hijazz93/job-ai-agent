import { ref, watch } from 'vue'

const THEME_KEY = 'job-ai-theme'
const theme = ref(localStorage.getItem(THEME_KEY) || 'dark')

export function useTheme() {
  const isDark = ref(theme.value === 'dark')

  function applyTheme(t) {
    theme.value = t
    isDark.value = t === 'dark'
    document.documentElement.setAttribute('data-theme', t)
    localStorage.setItem(THEME_KEY, t)
  }

  function toggleTheme() {
    applyTheme(isDark.value ? 'light' : 'dark')
  }

  document.documentElement.setAttribute('data-theme', theme.value)

  return { theme, isDark, applyTheme, toggleTheme }
}
