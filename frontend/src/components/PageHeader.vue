<script setup lang="ts">
import { ChevronLeft } from '@lucide/vue'
import { RouterLink } from 'vue-router'

defineProps<{ title: string }>()
</script>

<template>
  <header class="grid grid-cols-[2.5rem_1fr_2.5rem] items-center">
    <RouterLink
      :to="{ name: 'today' }"
      aria-label="Back"
      class="-ml-2 flex size-10 items-center justify-center rounded-full"
    >
      <ChevronLeft class="size-7" />
    </RouterLink>
    <h1 class="caps justify-self-center text-base">{{ title }}</h1>
    <span />
  </header>
</template>
