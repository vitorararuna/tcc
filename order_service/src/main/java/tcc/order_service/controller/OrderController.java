package tcc.order_service.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tcc.order_service.model.Order;
import tcc.order_service.service.OrderService;
import tcc.order_service.scheduler.OrderProcessingTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/orders")
public class OrderController {

  @Autowired
  private OrderService orderService;

  private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

  private final MeterRegistry meterRegistry;
  private final OrderProcessingTask orderProcessingTask;

  private final Counter getAllOrdersCounter;
  private final Timer getAllOrdersTimer;
  private final Counter getOrderByIdCounter;
  private final Timer getOrderByIdTimer;
  private final Counter createOrderCounter;
  private final Timer createOrderTimer;
  private final Counter updateOrderCounter;
  private final Timer updateOrderTimer;
  private final Counter deleteOrderCounter;
  private final Timer deleteOrderTimer;

  @Autowired
  public OrderController(MeterRegistry meterRegistry, OrderProcessingTask orderProcessingTask) {
    this.meterRegistry = meterRegistry;
    this.orderProcessingTask = orderProcessingTask;

    this.getAllOrdersCounter = meterRegistry.counter("endpoint.getAllOrders.count");
    this.getAllOrdersTimer = meterRegistry.timer("endpoint.getAllOrders.time");

    this.getOrderByIdCounter = meterRegistry.counter("endpoint.getOrderById.count");
    this.getOrderByIdTimer = meterRegistry.timer("endpoint.getOrderById.time");

    this.createOrderCounter = meterRegistry.counter("endpoint.createOrder.count");
    this.createOrderTimer = meterRegistry.timer("endpoint.createOrder.time");

    this.updateOrderCounter = meterRegistry.counter("endpoint.updateOrder.count");
    this.updateOrderTimer = meterRegistry.timer("endpoint.updateOrder.time");

    this.deleteOrderCounter = meterRegistry.counter("endpoint.deleteOrder.count");
    this.deleteOrderTimer = meterRegistry.timer("endpoint.deleteOrder.time");
  }

  @GetMapping
  public List<Order> getAllOrders() {
    getAllOrdersCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to get all orders.");
    List<Order> orders = orderService.findAll();

    logger.info("Returning {} orders.", orders.size());

    getAllOrdersTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return orders;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
    getOrderByIdCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to get order with id: {}", id);

    Optional<Order> order = orderService.findById(id);
    ResponseEntity<Order> response = order.map(o -> {
      logger.info("Order found: {}", o);
      return ResponseEntity.ok(o);
    }).orElseGet(() -> {
      logger.warn("Order with id {} not found.", id);
      return ResponseEntity.notFound().build();
    });

    getOrderByIdTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return response;
  }

  @PostMapping
  public ResponseEntity<?> createOrder(@RequestBody Order order) {
    createOrderCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to create order: {}", order);

    try {
      Order savedOrder = orderService.save(order);
      logger.info("Order created successfully with id: {}", savedOrder.getId());
      createOrderTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return ResponseEntity.ok(savedOrder);
    } catch (RuntimeException e) {
      logger.error("Failed to create order: {}", e.getMessage());
      createOrderTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<?> updateOrderStatus(
      @PathVariable Long id,
      @RequestBody Map<String, String> statusUpdate) {

    updateOrderCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received status update request for order ID {}: {}", id, statusUpdate);

    if (!statusUpdate.containsKey("status")) {
      logger.warn("Invalid status update request - missing 'status' field");
      return ResponseEntity.badRequest().body("Campo 'status' obrigatório");
    }

    String newStatus = statusUpdate.get("status").toUpperCase();
    if (!isValidStatusTransition(newStatus)) {
      logger.warn("Invalid status transition attempted: {}", newStatus);
      return ResponseEntity.badRequest().body("Status inválido ou transição não permitida");
    }

    ResponseEntity<?> response = orderService.findById(id)
        .map(existingOrder -> {
          String previousStatus = existingOrder.getStatus();
          existingOrder.setStatus(newStatus);

          try {
            Order updatedOrder = orderService.save(existingOrder);
            logger.info("Order {} status updated from {} to {}",
                id, previousStatus, newStatus);

            updateOrderTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
            return ResponseEntity.ok(updatedOrder);
          } catch (RuntimeException e) {
            logger.error("Failed to update status: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(e.getMessage());
          }
        })
        .orElseGet(() -> {
          logger.warn("Order ID {} not found for status update", id);
          return ResponseEntity.notFound().build();
        });

    updateOrderTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return response;
  }


  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
    deleteOrderCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to delete order with id: {}", id);

    ResponseEntity<Void> response = orderService.findById(id)
        .map(order -> {
          orderService.deleteById(id);
          logger.info("Successfully deleted order with id: {}", id);
          return ResponseEntity.ok().<Void>build();
        })
        .orElseGet(() -> {
          logger.warn("Order with id {} not found for deletion.", id);
          return ResponseEntity.notFound().build();
        });

    deleteOrderTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return response;
  }

  @GetMapping("/delayed-view")
  public List<tcc.order_service.dto.DelayedOrderDTO> getDelayedOrdersForView() {
    logger.info("Received request for delayed orders view from Spring Boot Admin.");

    LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(3);
    List<Order> delayedOrders = orderService.findPendingOrdersOlderThan(cutoffTime);

    return delayedOrders.stream()
        .map(this::convertToDto)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Endpoint para acionar manualmente a tarefa agendada de verificação de pedidos atrasados.
   * Permite executar a tarefa sob demanda, além da execução automática a cada 30 segundos.
   *
   * @return Mensagem de confirmação da execução
   */
  @PostMapping("/trigger-scheduled-task")
  public ResponseEntity<Map<String, String>> triggerScheduledTask() {
    logger.info("Received request to manually trigger scheduled task: processPendingOrders");

    try {
      // Aciona manualmente a tarefa agendada
      orderProcessingTask.processPendingOrders();

      Map<String, String> response = Map.of(
          "status", "success",
          "message", "Tarefa agendada 'processPendingOrders' acionada com sucesso"
      );

      logger.info("Scheduled task triggered successfully");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      logger.error("Error triggering scheduled task: {}", e.getMessage(), e);

      Map<String, String> response = Map.of(
          "status", "error",
          "message", "Erro ao acionar tarefa: " + e.getMessage()
      );

      return ResponseEntity.internalServerError().body(response);
    }
  }

  private tcc.order_service.dto.DelayedOrderDTO convertToDto(Order order) {
      java.time.Duration delay = java.time.Duration.between(order.getCreatedAt(), java.time.LocalDateTime.now());
      String formattedDelay = String.format("%dm %ds", delay.toMinutes(), delay.toSecondsPart());

      List<String> productDescriptions = order.getProducts().stream()
          .map(p -> String.format("Cód: %d (Qtd: %d)", p.getProductCode(), p.getQuantity()))
          .collect(java.util.stream.Collectors.toList());

      return new tcc.order_service.dto.DelayedOrderDTO(
          order.getId(),
          order.getTableNumber(),
          formattedDelay,
          productDescriptions
      );
  }

  private boolean isValidStatusTransition(String newStatus) {
    return switch (newStatus) {
      case "PENDING", "CANCELED", "FINISHED" -> true;
      default -> false;
    };
  }
}
