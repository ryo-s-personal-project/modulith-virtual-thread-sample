package com.example.order.application;

import com.example.order.domain.Order;
import com.example.order.infrastructure.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public OrderDto createOrder(CreateOrderCommand command) {
        log.info("注文を作成中: 顧客ID={}, 商品ID={}, 数量={}", 
                command.customerId(), command.productId(), command.quantity());
        
        BigDecimal totalAmount = command.unitPrice().multiply(BigDecimal.valueOf(command.quantity()));
        
        Order order = Order.builder()
                .customerId(command.customerId())
                .productId(command.productId())
                .quantity(command.quantity())
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.PENDING)
                .build();
        
        order = orderRepository.save(order);
        
        // Publish event asynchronously using virtual thread
        publishOrderCreatedEvent(order);
        
        log.info("注文を作成しました: 注文ID={}", order.getId());
        return OrderDto.from(order);
    }
    
    @Async
    private void publishOrderCreatedEvent(Order order) {
        log.info("OrderCreatedEventを発行中: 注文ID={} (スレッド: {})", 
                order.getId(), Thread.currentThread());
        
        com.example.order.adapter.OrderCreatedEvent event = 
                new com.example.order.adapter.OrderCreatedEvent(
                        order.getId(),
                        order.getCustomerId(),
                        order.getProductId(),
                        order.getQuantity(),
                        order.getTotalAmount()
                );
        
        eventPublisher.publishEvent(event);
    }
    
    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.confirm();
        orderRepository.save(order);
        
        com.example.order.adapter.OrderConfirmedEvent confirmedEvent = 
                new com.example.order.adapter.OrderConfirmedEvent(
                        order.getId(),
                        order.getCustomerId(),
                        order.getProductId(),
                        order.getQuantity()
                );
        
        eventPublisher.publishEvent(confirmedEvent);
        log.info("注文を確定しました: 注文ID={}", order.getId());
    }
    
    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.cancel();
        orderRepository.save(order);
        
        com.example.order.adapter.OrderCancelledEvent cancelledEvent = 
                new com.example.order.adapter.OrderCancelledEvent(
                        order.getId(),
                        order.getProductId(),
                        order.getQuantity()
                );
        
        eventPublisher.publishEvent(cancelledEvent);
        log.info("注文をキャンセルしました: 注文ID={}", order.getId());
    }
    
    public OrderDto getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return OrderDto.from(order);
    }
}
