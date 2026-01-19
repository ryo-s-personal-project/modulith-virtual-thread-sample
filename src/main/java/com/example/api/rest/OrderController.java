package com.example.api.rest;

import com.example.inventory.application.CreateInventoryItemCommand;
import com.example.inventory.application.InventoryDto;
import com.example.inventory.application.InventoryService;
import com.example.order.application.CreateOrderCommand;
import com.example.order.application.OrderDto;
import com.example.order.application.OrderService;
import com.example.shipping.application.ShipmentDto;
import com.example.shipping.application.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("注文作成リクエストを受信しました (スレッド: {})", Thread.currentThread());
        
        CreateOrderCommand command = new CreateOrderCommand(
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.unitPrice()
        );
        
        OrderDto order = orderService.createOrder(command);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable UUID orderId) {
        OrderDto order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/{orderId}/shipment")
    public ResponseEntity<ShipmentDto> getShipment(@PathVariable UUID orderId) {
        ShipmentDto shipment = shippingService.getShipment(orderId);
        return ResponseEntity.ok(shipment);
    }
    
    @PostMapping("/inventory")
    public ResponseEntity<InventoryDto> createInventoryItem(@RequestBody CreateInventoryRequest request) {
        CreateInventoryItemCommand command = new CreateInventoryItemCommand(
                request.productId(),
                request.quantity()
        );
        
        InventoryDto item = inventoryService.createInventoryItem(command);
        return ResponseEntity.ok(item);
    }
    
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<InventoryDto> getInventory(@PathVariable String productId) {
        InventoryDto item = inventoryService.getInventory(productId);
        return ResponseEntity.ok(item);
    }
    
    public record CreateOrderRequest(
            String customerId,
            String productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
    
    public record CreateInventoryRequest(
            String productId,
            Integer quantity
    ) {}
}
