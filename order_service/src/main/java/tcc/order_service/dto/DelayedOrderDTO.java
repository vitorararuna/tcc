package tcc.order_service.dto;

import java.util.List;

public class DelayedOrderDTO {
  private final Long orderId;
  private final Integer tableNumber;
  private final String timeDelayed;
  private final List<String> products;

  public DelayedOrderDTO(Long orderId, Integer tableNumber, String timeDelayed, List<String> products) {
    this.orderId = orderId;
    this.tableNumber = tableNumber;
    this.timeDelayed = timeDelayed;
    this.products = products;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Integer getTableNumber() {
    return tableNumber;
  }

  public String getTimeDelayed() {
    return timeDelayed;
  }

  public List<String> getProducts() {
    return products;
  }
}