package tcc.order_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tcc.order_service.model.Order;
import tcc.order_service.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final WebClient.Builder webClientBuilder;
  private final MeterRegistry meterRegistry;
  private final Counter totalProductsCounter;
  private final String PRODUCT_SERVICE_URL = "http://172.19.0.3:2020/products/";

  @Autowired
  public OrderService(
      OrderRepository orderRepository,
      WebClient.Builder webClientBuilder,
      MeterRegistry meterRegistry) {
    this.orderRepository = orderRepository;
    this.webClientBuilder = webClientBuilder;
    this.meterRegistry = meterRegistry;
    this.totalProductsCounter = meterRegistry.counter("order.products.total"); // Métrica global
  }

  public List<Order> findAll() {
    return orderRepository.findAll();
  }

  public Optional<Order> findById(Long id) {
    return orderRepository.findById(id);
  }

  public Order save(Order order) {
    for(Order.OrderProduct product : order.getProducts()) {
      if(!checkProductExists(product.getProductCode())) {
        throw new RuntimeException("Produto não encontrado: " + product.getProductCode());
      }
    }

    Order savedOrder = orderRepository.save(order);

    List<Long> productIds = order.getProducts().stream()
        .map(Order.OrderProduct::getProductCode)
        .distinct()
        .collect(Collectors.toList());

    Map<Long, String> productNames = getProductNames(productIds);

    for (int i = 0; i < productIds.size(); i++) {
      for (int j = i + 1; j < productIds.size(); j++) {
        Long id1 = productIds.get(i);
        Long id2 = productIds.get(j);

        String name1 = productNames.getOrDefault(id1, "Produto Desconhecido");
        String name2 = productNames.getOrDefault(id2, "Produto Desconhecido");

        String pairLabel = String.format("%s - %s",
            name1, name2);

        // Métrica de combinações de produtos
        meterRegistry.counter("order.product.combinations",
            "pair", pairLabel,
            "product1_id", String.valueOf(id1),
            "product2_id", String.valueOf(id2)
        ).increment();
      }
    }

    // Métrica de produtos individuais
    for(Order.OrderProduct product : savedOrder.getProducts()) {
      totalProductsCounter.increment(product.getQuantity());

      String productName = productNames.getOrDefault(product.getProductCode(), "Desconhecido");

      Counter productCounter = meterRegistry.counter("order.products.details",
          "productId", String.valueOf(product.getProductCode()),
          "productName", productName,
          "quantity", String.valueOf(product.getQuantity())
      );
      productCounter.increment(product.getQuantity());
    }

    return savedOrder;
  }

  public void deleteById(Long id) {
    orderRepository.deleteById(id);
  }

  public boolean checkProductExists(Long productCode) {
    WebClient webClient = webClientBuilder.build();

    String url = PRODUCT_SERVICE_URL + "exists/" + productCode;

    try {
      Boolean exists = webClient.get()
          .uri(url)
          .retrieve()
          .onStatus(httpStatus -> httpStatus.is4xxClientError(),
              clientResponse -> Mono.error(new RuntimeException("Produto não encontrado")))
          .bodyToMono(Boolean.class)
          .block();

      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      new RuntimeException("Produto não encontrado", e);
      return false;
    }
  }

  public List<Order> findPendingOrdersOlderThan(LocalDateTime cutoffTime) {
    return orderRepository.findPendingOrdersOlderThan(cutoffTime);
  }

  private Map<Long, String> getProductNames(List<Long> productIds) {
    WebClient webClient = webClientBuilder.build();

    return webClient.post()
        .uri(PRODUCT_SERVICE_URL + "batch-details")
        .bodyValue(productIds)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<Long, String>>() {})
        .blockOptional()
        .orElse(Collections.emptyMap());
  }
}
