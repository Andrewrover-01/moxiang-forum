<template>
  <div class="captcha-widget">
    <!-- Invisible mode: auto-verifies on mount, shows a subtle verified badge -->
    <div v-if="state === 'invisible-ok'" class="captcha-invisible-ok">
      <el-icon class="captcha-check-icon"><Check /></el-icon>
      <span>验证通过</span>
    </div>

    <!-- Slider mode: pending user interaction -->
    <template v-else-if="captchaType === 'SLIDER'">
      <div class="captcha-slider-container" :class="{ success: state === 'slider-ok', error: state === 'error' }">
        <!-- Track bar -->
        <div class="captcha-track" ref="trackRef">
          <!-- Gap marker (shows where the piece should go) -->
          <div
            class="captcha-gap"
            :style="{ left: (sliderGapPercent ?? 50) + '%' }"
          />
          <!-- Filled portion -->
          <div class="captcha-fill" :style="{ width: fillPercent + '%' }" />
          <!-- Hint text inside the track -->
          <span class="captcha-hint" v-if="state !== 'slider-ok'">向右拖动滑块完成验证</span>
          <span class="captcha-hint success-text" v-else>验证成功</span>
          <!-- Slider handle -->
          <div
            class="captcha-handle"
            :style="{ left: fillPercent + '%' }"
            @mousedown="onDragStart"
            @touchstart.prevent="onTouchStart"
          >
            <el-icon v-if="state !== 'slider-ok'"><ArrowRight /></el-icon>
            <el-icon v-else class="captcha-check-icon"><Check /></el-icon>
          </div>
        </div>
        <div v-if="state === 'error'" class="captcha-error-msg">验证失败，请重试</div>
      </div>
      <el-button
        v-if="state === 'error'"
        size="small"
        text
        class="captcha-refresh"
        @click="loadChallenge"
      >
        刷新验证码
      </el-button>
    </template>

    <!-- Loading state -->
    <div v-else-if="state === 'loading'" class="captcha-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载验证...</span>
    </div>

    <!-- Initial / unknown state -->
    <div v-else class="captcha-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>准备验证...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Check, ArrowRight, Loading } from '@element-plus/icons-vue'
import { getCaptchaChallenge, verifyCaptcha } from '@/api/captcha'

const props = defineProps<{
  /** Scene: 'REGISTER' | 'POST' | 'COMMENT' */
  scene: string
}>()

const emit = defineEmits<{
  /** Emitted when verification succeeds; payload is the captchaToken */
  (e: 'verified', token: string): void
}>()

// ---- State ----
type State = 'loading' | 'invisible-ok' | 'slider-idle' | 'slider-ok' | 'error'
const state = ref<State>('loading')
const captchaType = ref<'INVISIBLE' | 'SLIDER'>('INVISIBLE')
const captchaId = ref('')
const sliderGapPercent = ref<number>(50)
const fillPercent = ref(0)
const trackRef = ref<HTMLDivElement | null>(null)

// ---- Drag state ----
let isDragging = false
let dragStartX = 0
let trackWidth = 0

// ---- Lifecycle ----
onMounted(() => {
  loadChallenge()
})

onBeforeUnmount(() => {
  removeDragListeners()
})

// ---- Challenge loading ----
async function loadChallenge() {
  state.value = 'loading'
  fillPercent.value = 0
  try {
    const { data } = await getCaptchaChallenge(props.scene)
    captchaId.value = data.data.captchaId
    captchaType.value = data.data.type
    sliderGapPercent.value = data.data.sliderGapPercent ?? 50

    if (captchaType.value === 'INVISIBLE') {
      // Auto-verify immediately
      await autoVerify()
    } else {
      state.value = 'slider-idle'
    }
  } catch {
    state.value = 'error'
  }
}

async function autoVerify() {
  try {
    const { data } = await verifyCaptcha(captchaId.value, null)
    state.value = 'invisible-ok'
    emit('verified', data.data.captchaToken)
  } catch {
    state.value = 'error'
  }
}

// ---- Mouse drag handlers ----
function onDragStart(event: MouseEvent) {
  if (state.value === 'slider-ok') return
  startDrag(event.clientX)
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(event: MouseEvent) {
  if (!isDragging) return
  moveDrag(event.clientX)
}

function onMouseUp(event: MouseEvent) {
  if (!isDragging) return
  endDrag(event.clientX)
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}

// ---- Touch handlers (mobile) ----
function onTouchStart(event: TouchEvent) {
  if (state.value === 'slider-ok') return
  const touch = event.touches[0]
  startDrag(touch.clientX)
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
}

function onTouchMove(event: TouchEvent) {
  event.preventDefault()
  if (!isDragging) return
  moveDrag(event.touches[0].clientX)
}

function onTouchEnd(event: TouchEvent) {
  if (!isDragging) return
  const lastTouch = event.changedTouches[0]
  endDrag(lastTouch.clientX)
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
}

// ---- Drag logic ----
function startDrag(clientX: number) {
  isDragging = true
  dragStartX = clientX
  trackWidth = trackRef.value?.offsetWidth ?? 300
}

function moveDrag(clientX: number) {
  const delta = clientX - dragStartX
  const pct = Math.min(100, Math.max(0, (delta / trackWidth) * 100))
  fillPercent.value = pct
}

async function endDrag(clientX: number) {
  isDragging = false
  const delta = clientX - dragStartX
  const pct = Math.round(Math.min(100, Math.max(0, (delta / trackWidth) * 100)))
  fillPercent.value = pct

  // Submit to backend
  try {
    const { data } = await verifyCaptcha(captchaId.value, pct)
    state.value = 'slider-ok'
    emit('verified', data.data.captchaToken)
  } catch {
    state.value = 'error'
    fillPercent.value = 0
  }
}

function removeDragListeners() {
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
}

// Expose reload for parent if needed
defineExpose({ reload: loadChallenge })
</script>

<style scoped>
.captcha-widget {
  margin: 8px 0;
  user-select: none;
}

/* ---- Invisible success ---- */
.captcha-invisible-ok {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #67c23a;
  font-size: 13px;
  padding: 6px 0;
}

/* ---- Loading ---- */
.captcha-loading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  padding: 6px 0;
}

/* ---- Slider ---- */
.captcha-slider-container {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.captcha-slider-container.success {
  border-color: #67c23a;
}
.captcha-slider-container.error {
  border-color: #f56c6c;
}

.captcha-track {
  position: relative;
  height: 44px;
  background: #f4f4f5;
  cursor: default;
}

.captcha-gap {
  position: absolute;
  top: 4px;
  bottom: 4px;
  width: 36px;
  margin-left: -18px;
  background: rgba(64, 158, 255, 0.25);
  border: 2px dashed #409eff;
  border-radius: 3px;
  pointer-events: none;
}

.captcha-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, #ecf5ff, #d9ecff);
  transition: width 0.05s linear;
  pointer-events: none;
}

.captcha-hint {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  pointer-events: none;
  z-index: 1;
}
.captcha-hint.success-text {
  color: #67c23a;
  font-weight: 600;
}

.captcha-handle {
  position: absolute;
  top: 0;
  width: 44px;
  height: 44px;
  margin-left: -22px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12);
  z-index: 2;
  transition: background 0.2s, border-color 0.2s;
  touch-action: none;
}
.captcha-handle:active {
  cursor: grabbing;
}
.captcha-slider-container.success .captcha-handle {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
  cursor: default;
}

.captcha-check-icon {
  color: #67c23a;
}
.captcha-slider-container.success .captcha-check-icon {
  color: #fff;
}

.captcha-error-msg {
  font-size: 12px;
  color: #f56c6c;
  padding: 4px 12px;
  background: #fef0f0;
}

.captcha-refresh {
  margin-top: 4px;
  font-size: 12px;
  color: #409eff;
  padding: 0;
}
</style>
