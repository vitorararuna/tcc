package tcc.order_service.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ElementCollection
  @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
  private List<OrderProduct> products = new ArrayList<>();

  private Integer tableNumber;

  private String status;

  @Column(updatable = false)
  private LocalDateTime createdAt;

  private LocalDateTime lastUpdated;

  @Embeddable
  public static class OrderProduct {
    @Column(name = "product_code")
    private Long productCode;
    @Column(name = "quantity")
    private Integer quantity;

    public OrderProduct() {
    }

    public OrderProduct(Long productCode, Integer quantity) {
      this.productCode = productCode;
      this.quantity = quantity;
    }

    public Long getProductCode() {
      return productCode;
    }

    public void setProductCode(Long productCode) {
      this.productCode = productCode;
    }

    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    lastUpdated = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    lastUpdated = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public List<OrderProduct> getProducts() {
    return products;
  }

  public void setProducts(List<OrderProduct> products) {
    this.products = products;
  }

  public Integer getTableNumber() {
    return tableNumber;
  }

  public void setTableNumber(Integer tableNumber) {
    this.tableNumber = tableNumber;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}