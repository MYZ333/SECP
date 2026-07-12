<template>
  <div class="brand-logo" :class="{ compact }">
    <svg class="brand-logo__icon" viewBox="0 0 52 34" aria-hidden="true">
      <path d="M15.5 1.5 C7 1.5 1.5 7 1.5 15.5 v3 C1.5 27 7 32.5 15.5 32.5 h6.2 l-4.6-5.6 h-1.6 c-4.6 0-7-2.4-7-7 v-5.8 c0-4.6 2.4-7 7-7 h1.6 L21.7 1.5 Z" />
      <path d="M36.5 1.5 C45 1.5 50.5 7 50.5 15.5 v3 c0 8.5-5.5 14-14 14 h-6.2 l4.6-5.6 h1.6 c4.6 0 7-2.4 7-7 v-5.8 c0-4.6-2.4-7-7-7 h-1.6 L30.3 1.5 Z" />
      <rect x="19" y="14.6" width="14" height="4.8" rx="1" />
    </svg>
    <span class="brand-logo__name">12组</span>
    <span v-if="subtitle" class="brand-logo__subtitle">{{ subtitle }}</span>
  </div>
</template>

<script setup>
defineProps({
  subtitle: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})
</script>

<style scoped>
.brand-logo { display: inline-flex; align-items: center; gap: 8px; white-space: nowrap; }
.brand-logo__icon { width: 40px; height: auto; fill: #FF6A00; flex: 0 0 auto; }
.brand-logo__name { color: #FF6A00; font-size: 28px; font-weight: 700; line-height: 1; }
.brand-logo__subtitle { margin-left: 4px; padding-top: 6px; color: #8A93A6; font-size: 14px; }
.compact .brand-logo__icon { width: 34px; }
.compact .brand-logo__name { font-size: 24px; }
@media (max-width: 680px) { .brand-logo__subtitle { display: none; } }
</style>
