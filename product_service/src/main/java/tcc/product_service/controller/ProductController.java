package tcc.product_service.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tcc.product_service.model.Product;
import tcc.product_service.service.ProductService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/products")
public class ProductController {

  @Autowired
  private ProductService productService;

  private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

  private final MeterRegistry meterRegistry;

  private final Counter getAllProductsCounter;
  private final Timer getAllProductsTimer;
  private final Counter getProductByIdCounter;
  private final Timer getProductByIdTimer;
  private final Counter createProductCounter;
  private final Timer createProductTimer;
  private final Counter updateProductCounter;
  private final Timer updateProductTimer;
  private final Counter deleteProductCounter;
  private final Timer deleteProductTimer;

  @Autowired
  public ProductController(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;

    // Contadores e temporizadores para cada endpoint
    this.getAllProductsCounter = meterRegistry.counter("endpoint.getAllProducts.count");
    this.getAllProductsTimer = meterRegistry.timer("endpoint.getAllProducts.time");

    this.getProductByIdCounter = meterRegistry.counter("endpoint.getProductById.count");
    this.getProductByIdTimer = meterRegistry.timer("endpoint.getProductById.time");

    this.createProductCounter = meterRegistry.counter("endpoint.createProduct.count");
    this.createProductTimer = meterRegistry.timer("endpoint.createProduct.time");

    this.updateProductCounter = meterRegistry.counter("endpoint.updateProduct.count");
    this.updateProductTimer = meterRegistry.timer("endpoint.updateProduct.time");

    this.deleteProductCounter = meterRegistry.counter("endpoint.deleteProduct.count");
    this.deleteProductTimer = meterRegistry.timer("endpoint.deleteProduct.time");
  }

  @GetMapping
  public List<Product> getAllProducts() {
    getAllProductsCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to get all products.");

    List<Product> products = productService.findAll();

    logger.info("Returning {} products.", products.size());

    getAllProductsTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return products;
  }

  @GetMapping("/{id}")
  public ResponseEntity<Product> getProductById(@PathVariable Long id) {
    getProductByIdCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to get product with id: {}", id);

    Optional<Product> product = productService.findById(id);
    ResponseEntity<Product> response = product.map(p -> {
      logger.info("Product found: {}", p);
      return ResponseEntity.ok(p);
    }).orElseGet(() -> {
      logger.warn("Product with id {} not found.", id);
      return ResponseEntity.notFound().build();
    });

    getProductByIdTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return response;
  }

  @PostMapping
  public Product createProduct(@RequestBody Product product) {
    createProductCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to create product: {}", product);

    Product savedProduct = productService.save(product);

    logger.info("Product created successfully with id: {}", savedProduct.getId());
    createProductTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return savedProduct;
  }


  @PutMapping("/{id}")
  public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
    updateProductCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to update product with id: {}", id);

    Optional<Product> optionalProduct = productService.findById(id);

    if (optionalProduct.isPresent()) {
      Product existingProduct = optionalProduct.get();
      logger.debug("Existing product details: {}", existingProduct);

      existingProduct.setName(productDetails.getName());
      existingProduct.setDescription(productDetails.getDescription());
      existingProduct.setPrice(productDetails.getPrice());

      Product updatedProduct = productService.save(existingProduct);

      logger.info("Successfully updated product with id: {}", id);
      updateProductTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return ResponseEntity.ok(updatedProduct);
    } else {
      logger.warn("Product with id {} not found for update.", id);
      updateProductTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
    deleteProductCounter.increment();
    long start = System.currentTimeMillis();

    logger.info("Received request to delete product with id: {}", id);

    ResponseEntity<Void> response = productService.findById(id)
        .map(product -> {
          productService.deleteById(id);
          logger.info("Successfully deleted product with id: {}", id);
          return ResponseEntity.ok().<Void>build();
        })
        .orElseGet(() -> {
          logger.warn("Product with id {} not found for deletion.", id);
          return ResponseEntity.notFound().build();
        });

    deleteProductTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
    return response;
  }

  @GetMapping("/exists/{productCode}")
  public ResponseEntity<Boolean> checkIfProductExists(@PathVariable Long productCode) {
    logger.info("Checking existence of product with code: {}", productCode);

    Optional<Product> product = productService.findById(productCode);
    boolean exists = product.isPresent();

    logger.info("Product with code {} exists: {}", productCode, exists);
    return ResponseEntity.ok(exists);
  }

  @PostMapping("/batch-details")
  public Map<Long, String> getProductBatchDetails(@RequestBody List<Long> productIds) {
    return productService.getProductNamesBatch(productIds);
  }

  @GetMapping("/reload-test")
  public ResponseEntity<String> checkReloadTest() {

    return ResponseEntity.ok("Reload Ok");
  }
}
