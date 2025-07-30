package com.example.OrderService.controller;

import com.example.OrderService.dto.ProductDto;
import com.example.OrderService.model.Order;
import com.example.OrderService.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    public OrderController(OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder() {
        Order order = new Order("PENDING");
        Order savedOrder = orderRepository.save(order);
        return ResponseEntity.ok(savedOrder);
    }

    @PostMapping("/{productId}")
    @Retry(name = "inventoryRetry", fallbackMethod = "handleInventoryFailure")
    @CircuitBreaker(name = "inventoryCircuitBreaker", fallbackMethod = "handleInventoryFailure")
    public ResponseEntity<?> createOrder(@PathVariable Long productId) {
        String inventoryUrl = "http://inventory-service:8080/inventory/product/" + productId;

        ProductDto product = restTemplate.getForObject(inventoryUrl, ProductDto.class);

        if (product == null || product.getQuantity() == null || product.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body("Товара нет в наличии.");
        }

        Order order = new Order("PENDING");
        return ResponseEntity.ok(orderRepository.save(order));
    }

    // fallback-метод
    public ResponseEntity<?> handleInventoryFailure(Long productId, Throwable ex) {
        return ResponseEntity.status(503).body("Inventory временно недоступен, попробуйте позже.");
    }
}
