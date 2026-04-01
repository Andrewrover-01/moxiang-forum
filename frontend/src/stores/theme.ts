import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ThemeMode = 'light' | 'warm' | 'dark'

const STORAGE_KEY = 'mxf-theme'

function applyTheme(mode: ThemeMode) {
  const html = document.documentElement
  html.setAttribute('data-theme', mode)
  if (mode === 'dark') {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(
    (localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'light'
  )

  function setMode(m: ThemeMode) {
    mode.value = m
    localStorage.setItem(STORAGE_KEY, m)
    applyTheme(m)
  }

  // Initialize theme immediately on store creation
  applyTheme(mode.value)

  return { mode, setMode }
})
