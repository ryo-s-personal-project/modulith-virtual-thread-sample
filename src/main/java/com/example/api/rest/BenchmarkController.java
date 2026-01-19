package com.example.api.rest;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {
    
    private static final Logger log = LoggerFactory.getLogger(BenchmarkController.class);
    
    /**
     * スレッド情報を取得
     * 注意: Virtual Threadは実行が完了するとすぐに終了するため、
     * このエンドポイントでは実行中のVirtual Threadのみが表示されます。
     * ベンチマーク実行中のVirtual Thread数を確認するには、ベンチマーク結果を参照してください。
     */
    @GetMapping("/thread-info")
    public ResponseEntity<ThreadInfo> getThreadInfo() {
        // Thread.getAllStackTraces().keySet()を使用してすべてのスレッドを取得
        Set<Thread> allThreads = Thread.getAllStackTraces().keySet();
        
        List<String> threadNames = new ArrayList<>();
        long virtualThreadCount = 0;
        long platformThreadCount = 0;
        long namedVirtualThreadCount = 0;
        
        // デバッグ用: 最初の10個のスレッド名をログに出力
        List<String> sampleThreadNames = new ArrayList<>();
        
        for (Thread thread : allThreads) {
            String name = thread.getName();
            threadNames.add(name);
            
            if (thread.isVirtual()) {
                virtualThreadCount++;
                if (name.startsWith("virtual-thread") || name.startsWith("ForkJoinPool") || 
                    name.contains("virtual") || name.contains("Virtual")) {
                    namedVirtualThreadCount++;
                }
                if (sampleThreadNames.size() < 10) {
                    sampleThreadNames.add(name + " (Virtual)");
                }
            } else {
                platformThreadCount++;
                if (sampleThreadNames.size() < 10) {
                    sampleThreadNames.add(name + " (Platform)");
                }
            }
        }
        
        log.debug("Sample thread names: {}", sampleThreadNames);
        log.debug("Total threads: {}, Virtual: {}, Platform: {}", 
                allThreads.size(), virtualThreadCount, platformThreadCount);
        
        return ResponseEntity.ok(new ThreadInfo(
                allThreads.size(),
                virtualThreadCount,
                platformThreadCount,
                namedVirtualThreadCount,
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
        
        // Virtual Thread Executorを使用
        ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        try {
            Instant start = Instant.now();
            
            // Virtual Threadを使用した非同期処理
            List<CompletableFuture<ThreadExecutionInfo>> futures = IntStream.range(0, concurrentRequests)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                        Thread currentThread = Thread.currentThread();
                        String threadName = currentThread.getName();
                        boolean isVirtual = currentThread.isVirtual();
                        
                        // デバッグ用ログ（最初の数件のみ）
                        if (i < 5) {
                            log.debug("Request-{} executing on thread: '{}' (Virtual: {})", 
                                    i, threadName != null ? threadName : "unnamed", isVirtual);
                        }
                        
                        // I/O待機をシミュレート
                        try {
                            Thread.sleep(ioDelayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        return new ThreadExecutionInfo(i, threadName, isVirtual);
                    }, virtualThreadExecutor))
                    .toList();
            
            // すべての処理が完了するまで待機
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            
            // スレッド情報を集計
            List<ThreadExecutionInfo> executionInfos = futures.stream()
                    .map(f -> f.join())
                    .toList();
            
            long virtualThreadCount = executionInfos.stream()
                    .filter(info -> info.isVirtual())
                    .count();
            
            // デバッグ用：最初の10件のスレッド情報をログ出力
            executionInfos.stream()
                    .limit(10)
                    .forEach(info -> {
                        String threadName = info.threadName() != null && !info.threadName().isEmpty() 
                                ? info.threadName() 
                                : "unnamed";
                        log.info("Request-{} executed on thread: '{}' (Virtual: {})", 
                                info.requestId(), threadName, info.isVirtual());
                    });
            
            // 統計情報をログ出力
            log.info("Thread execution summary: Total={}, Virtual={}, Platform={}", 
                    executionInfos.size(), virtualThreadCount, executionInfos.size() - virtualThreadCount);
            
            double throughput = duration.toMillis() > 0 
                    ? (double) concurrentRequests / duration.toMillis() * 1000 
                    : 0.0;
            
            BenchmarkResult result = new BenchmarkResult(
                    concurrentRequests,
                    duration.toMillis(),
                    virtualThreadCount,
                    concurrentRequests - virtualThreadCount,
                    throughput
            );
            
            log.info("ベンチマーク完了: 総時間={}ms, スループット={} req/sec, Virtual Thread数={}", 
                    result.totalTimeMs(), String.format("%.2f", result.throughput()), virtualThreadCount);
            
            return ResponseEntity.ok(result);
        } finally {
            virtualThreadExecutor.shutdown();
        }
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
    
    // スレッド実行情報を保持する内部クラス
    private record ThreadExecutionInfo(int requestId, String threadName, boolean isVirtual) {}
}
