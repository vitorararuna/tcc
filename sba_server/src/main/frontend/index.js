// /sba_server/src/main/frontend/index.js

console.log('--- Custom UI script loaded! ---');

import delayedOrdersView from './delayed-orders-view.vue';
import handleWithLabel from './handle-with-label.vue';

SBA.use({
  install({ viewRegistry }) {
    console.log('--- Registering custom view: delayed-orders ---');

    viewRegistry.addView({
      name: 'delayed-orders',
      path: '/pedidos-atrasados',
      component: delayedOrdersView,
      label: 'Pedidos Atrasados',
      order: 500,
      handle: handleWithLabel,
    });
  },
});