package tcc.order_service.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tcc.order_service.model.Order;
import tcc.order_service.service.OrderService;

import java.util.List;
@Component
public class OrderProcessingTask {

  private static final Logger logger = LoggerFactory.getLogger(OrderProcessingTask.class);
  private final OrderService orderService;

  public OrderProcessingTask(OrderService orderService) {
    this.orderService = orderService;
  }

  @Scheduled(fixedRate = 30000)
  public void processPendingOrders() {
    logger.info("Verificando pedidos pendentes com mais de 3 minutos...");

    LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(3);
    List<Order> delayedOrders = orderService.findPendingOrdersOlderThan(cutoffTime);

    for (Order order : delayedOrders) {
      try {
        logger.warn(
            " ATENÇÃO - PEDIDO ATRASADO !!!  Pedido ID {} está atrasado (criado em: {}).",
            order.getId(),
            order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
      } catch (Exception e) {
        logger.error("Erro ao verificar pedido ID {}: {}", order.getId(), e.getMessage());
      }
    }

    if (delayedOrders.isEmpty()) {
      logger.info("Nenhum pedido atrasado encontrado.");
    } else {
      logger.info("Total de pedidos atrasados: {}", delayedOrders.size());
    }
  }
}