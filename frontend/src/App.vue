<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <main
    class="mx-auto min-h-dvh max-w-md px-4 pt-[max(1rem,env(safe-area-inset-top))] pb-[max(1rem,env(safe-area-inset-bottom))]"
  >
    <RouterView />
  </main>
</template>
