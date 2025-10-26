<!-- /sba_server/src/main/frontend/handle-with-label.vue -->
<template>
  <div class="is-flex is-align-items-center">
    <icon-clock />
    <span class="ml-2">Pedidos Atrasados</span>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import iconClock from './icon-clock.vue';

export default defineComponent({
  components: {
    iconClock,
  },
});
</script>