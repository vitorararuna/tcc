package tcc.product_service.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tcc.product_service.model.Product;
import tcc.product_service.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

  @Autowired
  private ProductRepository productRepository;

  private final Counter productCreateCounter;
  private final Counter productUpdateCounter;
  private final Counter productDeleteCounter;

  @Autowired
  public ProductService(MeterRegistry meterRegistry) {
    this.productCreateCounter = meterRegistry.counter("product.create.count");
    this.productUpdateCounter = meterRegistry.counter("product.update.count");
    this.productDeleteCounter = meterRegistry.counter("product.delete.count");
  }

  public List<Product> findAll() {
    return productRepository.findAll();
  }

  public Optional<Product> findById(Long id) {
    return productRepository.findById(id);
  }

  public Product save(Product product) {
    if (product.getId() == null) {
      productCreateCounter.increment();
    } else {
      productUpdateCounter.increment();
    }
    return productRepository.save(product);
  }

  public void deleteById(Long id) {
    productDeleteCounter.increment();
    productRepository.deleteById(id);
  }

  public Map<Long, String> getProductNamesBatch(List<Long> productIds) {
    List<Long> distinctIds = productIds.stream()
        .distinct()
        .collect(Collectors.toList());

    return productRepository.findAllById(distinctIds)
        .stream()
        .collect(Collectors.toMap(
            Product::getId,
            product -> String.format("%s (%d)", product.getName(), product.getId()),
            (existing, replacement) -> existing
        ));
  }
}