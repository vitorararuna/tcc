<template>
  <section class="section">
    <div class="container">
      <h1 class="title">Visão Geral de Pedidos Atrasados</h1>
      <h2 class="subtitle">
        Buscando em <strong>order_service</strong> por pedidos com status 'PENDING' há mais de 3 minutos.
      </h2>

      <sba-alert v-if="error" :error="error" :title="`Falha ao carregar os pedidos`" />

      <div v-if="loading" class="is-loading" />

      <table class="table is-fullwidth is-hoverable" v-if="!loading && orders.length > 0">
        <thead>
        <tr>
          <th>ID do Pedido</th>
          <th>Mesa</th>
          <th>Tempo de Atraso</th>
          <th>Produtos</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="order in orders" :key="order.orderId">
          <td v-text="order.orderId" />
          <td v-text="order.tableNumber" />
          <td>
            <span class="tag is-danger is-light" v-text="order.timeDelayed" />
          </td>
          <td>
            <ul>
              <li v-for="(product, index) in order.products" :key="index" v-text="product" />
            </ul>
          </td>
        </tr>
        </tbody>
      </table>

      <div v-if="!loading && orders.length === 0 && !error" class="has-text-centered">
        <p class="is-size-5">
          🎉 Nenhum pedido atrasado no momento!
        </p>
      </div>
    </div>
  </section>
</template>

<script>
import { onMounted, ref } from 'vue';
import axios from 'axios';

export default {
  setup() {
    // LOG: Confirma que o componente está sendo inicializado.
    console.log('[DelayedOrdersView] Component is setting up.');

    const loading = ref(true);
    const error = ref(null);
    const orders = ref([]);

    const fetchDelayedOrders = async () => {
      // LOG: Confirma que a função de busca foi chamada.
      console.log('[DelayedOrdersView] Starting to fetch delayed orders...');
      loading.value = true;
      error.value = null;
      try {
        const response = await axios.get('http://localhost:4040/orders/delayed-view');
        // LOG: Mostra os dados que recebemos com sucesso. Essencial para ver se a API está retornando o que esperamos.
        console.log('[DelayedOrdersView] Data received successfully:', response.data);
        orders.value = response.data;
      } catch (e) {
        // LOG DE ERRO: Captura e exibe qualquer erro na chamada da API (404, 500, CORS, etc.).
        console.error('[DelayedOrdersView] API call failed!', e);
        error.value = e;
      } finally {
        // LOG: Confirma que o processo terminou, independentemente do resultado.
        console.log('[DelayedOrdersView] Fetch process finished.');
        loading.value = false;
      }
    };

    onMounted(() => {
      // LOG: Confirma que o hook 'onMounted' foi disparado, o que deve acionar a busca de dados.
      console.log('[DelayedOrdersView] Component mounted, calling fetchDelayedOrders.');
      fetchDelayedOrders();
    });

    return {
      loading,
      error,
      orders,
    };
  },
};
</script>

<style scoped>
.table ul {
  list-style-type: disc;
  padding-left: 20px;
}
</style>