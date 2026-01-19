package com.example.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/load-test")
@RequiredArgsConstructor
@Slf4j
public class LoadTestController {
    
    private final com.example.order.application.OrderService orderService;
    private final com.example.inventory.application.InventoryService inventoryService;
    
    /**
     * 大量の注文リクエストを並行処理してVirtual Threadの効果を確認
     */
    @PostMapping("/orders")
    public ResponseEntity<LoadTestResult> loadTestOrders(
            @RequestParam(defaultValue = "50") int requestCount,
            @RequestParam(defaultValue = "product-001") String productId) {
        
        log.info("負荷テスト開始: リクエスト数={}, 商品ID={}", requestCount, productId);
        
        // 在庫を準備
        try {
            inventoryService.createInventoryItem(
                    new com.example.inventory.application.CreateInventoryItemCommand(productId, requestCount * 10));
        } catch (Exception e) {
            // 既に存在する場合は無視
        }
        
        Instant start = Instant.now();
        
        // 並行で注文を作成
        List<CompletableFuture<String>> futures = IntStream.range(0, requestCount)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String threadName = Thread.currentThread().getName();
                    boolean isVirtual = Thread.currentThread().isVirtual();
                    
                    try {
                        var command = new com.example.order.application.CreateOrderCommand(
                                "customer-" + i,
                                productId,
                                1,
                                java.math.BigDecimal.valueOf(1000)
                        );
                        
                        var order = orderService.createOrder(command);
                        return String.format("Order-%s created on %s (Virtual: %s)", 
                                order.id(), threadName, isVirtual);
                    } catch (Exception e) {
                        return String.format("Order-%d failed: %s", i, e.getMessage());
                    }
                }))
                .toList();
        
        // すべての処理が完了するまで待機
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        long successCount = futures.stream()
                .map(f -> f.join())
                .filter(s -> s.contains("created"))
                .count();
        
        long virtualThreadCount = futures.stream()
                .map(f -> f.join())
                .filter(s -> s.contains("Virtual: true"))
                .count();
        
        LoadTestResult result = new LoadTestResult(
                requestCount,
                successCount,
                requestCount - successCount,
                duration.toMillis(),
                virtualThreadCount,
                (double) requestCount / duration.toMillis() * 1000
        );
        
        log.info("負荷テスト完了: 成功={}, 失敗={}, 総時間={}ms, スループット={:.2f} req/sec", 
                result.successCount(), result.failureCount(), result.totalTimeMs(), result.throughput());
        
        return ResponseEntity.ok(result);
    }
    
    public record LoadTestResult(
            int totalRequests,
            long successCount,
            long failureCount,
            long totalTimeMs,
            long virtualThreadCount,
            double throughput
    ) {}
}
