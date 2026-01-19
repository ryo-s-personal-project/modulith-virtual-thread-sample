package com.example.api.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
@Slf4j
public class BenchmarkController {
    
    /**
     * スレッド情報を取得
     */
    @GetMapping("/thread-info")
    public ResponseEntity<ThreadInfo> getThreadInfo() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        
        int activeThreads = rootGroup.activeCount();
        Thread[] threads = new Thread[activeThreads * 2];
        int count = rootGroup.enumerate(threads, true);
        
        List<String> threadNames = new ArrayList<>();
        int virtualThreadCount = 0;
        int platformThreadCount = 0;
        
        for (int i = 0; i < count; i++) {
            Thread thread = threads[i];
            if (thread != null) {
                String name = thread.getName();
                threadNames.add(name);
                if (thread.isVirtual()) {
                    virtualThreadCount++;
                } else {
                    platformThreadCount++;
                }
            }
        }
        
        return ResponseEntity.ok(new ThreadInfo(
                activeThreads,
                virtualThreadCount,
                platformThreadCount,
                threadNames.stream().filter(n -> n.startsWith("virtual-thread")).count(),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().totalMemory() / 1024 / 1024, // MB
                Runtime.getRuntime().freeMemory() / 1024 / 1024, // MB
                Runtime.getRuntime().maxMemory() / 1024 / 1024 // MB
        ));
    }
    
    /**
     * I/O待機をシミュレートする非同期処理のベンチマーク
     * Virtual Threadの優位性を確認するためのエンドポイント
     */
    @PostMapping("/io-simulation")
    public ResponseEntity<BenchmarkResult> benchmarkIoSimulation(
            @RequestParam(defaultValue = "100") int concurrentRequests,
            @RequestParam(defaultValue = "100") int ioDelayMs) {
        
        log.info("I/Oシミュレーションベンチマーク開始: 並行リクエスト数={}, I/O待機時間={}ms", 
                concurrentRequests, ioDelayMs);
        
        Instant start = Instant.now();
        
        // Virtual Threadを使用した非同期処理
        List<CompletableFuture<String>> futures = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    String threadName = Thread.currentThread().getName();
                    boolean isVirtual = Thread.currentThread().isVirtual();
                    
                    // I/O待機をシミュレート
                    try {
                        Thread.sleep(ioDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    return String.format("Request-%d completed on %s (Virtual: %s)", 
                            i, threadName, isVirtual);
                }))
                .toList();
        
        // すべての処理が完了するまで待機
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        long virtualThreadCount = futures.stream()
                .map(f -> f.join())
                .filter(s -> s.contains("Virtual: true"))
                .count();
        
        BenchmarkResult result = new BenchmarkResult(
                concurrentRequests,
                duration.toMillis(),
                virtualThreadCount,
                concurrentRequests - virtualThreadCount,
                (double) concurrentRequests / duration.toMillis() * 1000 // スループット (req/sec)
        );
        
        log.info("ベンチマーク完了: 総時間={}ms, スループット={:.2f} req/sec", 
                result.totalTimeMs(), result.throughput());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 従来のスレッドプールとの比較用（参考実装）
     * 注意: このエンドポイントは従来のスレッドプールを使用します
     */
    @PostMapping("/io-simulation-platform-threads")
    public ResponseEntity<BenchmarkResult> benchmarkWithPlatformThreads(
            @RequestParam(defaultValue = "100") int concurrentRequests,
            @RequestParam(defaultValue = "100") int ioDelayMs,
            @RequestParam(defaultValue = "10") int threadPoolSize) {
        
        log.info("プラットフォームスレッドでのベンチマーク開始: 並行リクエスト数={}, I/O待機時間={}ms, スレッドプールサイズ={}", 
                concurrentRequests, ioDelayMs, threadPoolSize);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        try {
            Instant start = Instant.now();
            
            List<CompletableFuture<String>> futures = IntStream.range(0, concurrentRequests)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        String threadName = Thread.currentThread().getName();
                        boolean isVirtual = Thread.currentThread().isVirtual();
                        
                        // I/O待機をシミュレート
                        try {
                            Thread.sleep(ioDelayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        return String.format("Request-%d completed on %s (Virtual: %s)", 
                                i, threadName, isVirtual);
                    }, executor))
                    .toList();
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            
            long virtualThreadCount = futures.stream()
                    .map(f -> f.join())
                    .filter(s -> s.contains("Virtual: true"))
                    .count();
            
            BenchmarkResult result = new BenchmarkResult(
                    concurrentRequests,
                    duration.toMillis(),
                    virtualThreadCount,
                    concurrentRequests - virtualThreadCount,
                    (double) concurrentRequests / duration.toMillis() * 1000
            );
            
            log.info("プラットフォームスレッドでのベンチマーク完了: 総時間={}ms, スループット={:.2f} req/sec", 
                    result.totalTimeMs(), result.throughput());
            
            return ResponseEntity.ok(result);
        } finally {
            executor.shutdown();
        }
    }
    
    public record ThreadInfo(
            int totalThreads,
            long virtualThreads,
            long platformThreads,
            long namedVirtualThreads,
            int availableProcessors,
            long totalMemoryMB,
            long freeMemoryMB,
            long maxMemoryMB
    ) {}
    
    public record BenchmarkResult(
            int concurrentRequests,
            long totalTimeMs,
            long virtualThreadCount,
            long platformThreadCount,
            double throughput
    ) {}
}
