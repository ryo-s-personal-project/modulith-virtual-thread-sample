package com.example.api.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/load-test")
public class LoadTestController {
    
    private static final Logger log = LoggerFactory.getLogger(LoadTestController.class);
    
    private final com.example.order.application.OrderService orderService;
    private final com.example.inventory.application.InventoryService inventoryService;
    
    public LoadTestController(
            com.example.order.application.OrderService orderService,
            com.example.inventory.application.InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }
    
    /**
     * 大量の注文リクエストを並行処理してVirtual Threadの効果を確認
     */
    @PostMapping("/orders")
    public ResponseEntity<LoadTestResult> loadTestOrders(
            @RequestParam(defaultValue = "200") int requestCount,
            @RequestParam(defaultValue = "product-001") String productId,
            @RequestParam(defaultValue = "500") int ioDelayMs) {
        
        log.info("負荷テスト開始: リクエスト数={}, 商品ID={}", requestCount, productId);
        
        // 在庫を準備
        try {
            inventoryService.createInventoryItem(
                    new com.example.inventory.application.CreateInventoryItemCommand(productId, requestCount * 10));
        } catch (Exception e) {
            // 既に存在する場合は無視
        }
        
        // Virtual Thread Executorを使用
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        try {
            Instant start = Instant.now();
            
            // 並行で注文を作成
            List<CompletableFuture<String>> futures = IntStream.range(0, requestCount)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        String threadName = Thread.currentThread().getName();
                        boolean isVirtual = Thread.currentThread().isVirtual();
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            // I/O待機をシミュレート（外部API呼び出しやデータベースクエリの待機時間を想定）
                            // データベース操作の前に配置することで、Virtual Threadの優位性が明確になります
                            try {
                                Thread.sleep(ioDelayMs); // I/O待機をシミュレート
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            long dbStartTime = System.currentTimeMillis();
                            var command = new com.example.order.application.CreateOrderCommand(
                                    "customer-" + i,
                                    productId,
                                    1,
                                    java.math.BigDecimal.valueOf(1000)
                            );
                            
                            var order = orderService.createOrder(command);
                            long dbTime = System.currentTimeMillis() - dbStartTime;
                            long totalTime = System.currentTimeMillis() - startTime;
                            
                            // デバッグ用: 最初の10件のみログ出力
                            if (i < 10) {
                                log.debug("注文処理時間(VT): リクエスト{}={}ms (I/O待機: {}ms, DB操作: {}ms)", 
                                        i, totalTime, ioDelayMs, dbTime);
                            }
                            
                            return String.format("Order-%s created on %s (Virtual: %s)", 
                                    order.id(), threadName, isVirtual);
                        } catch (Exception e) {
                            log.error("注文作成エラー: リクエスト{}={}", i, e.getMessage(), e);
                            return String.format("Order-%d failed: %s", i, e.getMessage());
                        }
                    }, virtualThreadExecutor))
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
        
            log.info("負荷テスト完了: 成功={}, 失敗={}, 総時間={}ms, スループット={:.2f} req/sec, Virtual Thread数={}", 
                    result.successCount(), result.failureCount(), result.totalTimeMs(), 
                    String.format("%.2f", result.throughput()), virtualThreadCount);
            
            return ResponseEntity.ok(result);
        } finally {
            virtualThreadExecutor.shutdown();
        }
    }
    
    /**
     * プラットフォームスレッドでの注文処理負荷テスト（比較用）
     */
    @PostMapping("/orders-platform-threads")
    public ResponseEntity<LoadTestResult> loadTestOrdersWithPlatformThreads(
            @RequestParam(defaultValue = "200") int requestCount,
            @RequestParam(defaultValue = "product-001") String productId,
            @RequestParam(defaultValue = "10") int threadPoolSize,
            @RequestParam(defaultValue = "500") int ioDelayMs) {
        
        log.info("プラットフォームスレッドでの負荷テスト開始: リクエスト数={}, 商品ID={}, スレッドプールサイズ={}", 
                requestCount, productId, threadPoolSize);
        
        // 在庫を準備
        try {
            inventoryService.createInventoryItem(
                    new com.example.inventory.application.CreateInventoryItemCommand(productId, requestCount * 10));
        } catch (Exception e) {
            // 既に存在する場合は無視
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            Instant start = Instant.now();
            
            // 並行で注文を作成
            List<CompletableFuture<String>> futures = IntStream.range(0, requestCount)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        String threadName = Thread.currentThread().getName();
                        boolean isVirtual = Thread.currentThread().isVirtual();
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            // I/O待機をシミュレート（外部API呼び出しやデータベースクエリの待機時間を想定）
                            // プラットフォームスレッドでは、この待機時間がボトルネックになります
                            try {
                                Thread.sleep(ioDelayMs); // I/O待機をシミュレート
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            
                            long dbStartTime = System.currentTimeMillis();
                            var command = new com.example.order.application.CreateOrderCommand(
                                    "customer-" + i,
                                    productId,
                                    1,
                                    java.math.BigDecimal.valueOf(1000)
                            );
                            
                            var order = orderService.createOrder(command);
                            long dbTime = System.currentTimeMillis() - dbStartTime;
                            long totalTime = System.currentTimeMillis() - startTime;
                            
                            // デバッグ用: 最初の10件のみログ出力
                            if (i < 10) {
                                log.debug("注文処理時間(PT): リクエスト{}={}ms (I/O待機: {}ms, DB操作: {}ms)", 
                                        i, totalTime, ioDelayMs, dbTime);
                            }
                            
                            return String.format("Order-%s created on %s (Virtual: %s)", 
                                    order.id(), threadName, isVirtual);
                        } catch (Exception e) {
                            log.error("注文作成エラー(Platform): リクエスト{}={}", i, e.getMessage(), e);
                            return String.format("Order-%d failed: %s", i, e.getMessage());
                        }
                    }, executor))
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
        
            log.info("プラットフォームスレッドでの負荷テスト完了: 成功={}, 失敗={}, 総時間={}ms, スループット={:.2f} req/sec", 
                    result.successCount(), result.failureCount(), result.totalTimeMs(), 
                    String.format("%.2f", result.throughput()));
            
            return ResponseEntity.ok(result);
        } finally {
            executor.shutdown();
        }
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
