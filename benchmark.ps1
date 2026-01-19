# Virtual Thread ベンチマーク用 PowerShell スクリプト
# 文字エンコーディング: UTF-8 with BOM

# 文字エンコーディングをUTF-8に設定（複数の方法で確実に）
$PSDefaultParameterValues['*:Encoding'] = 'utf8'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$env:PYTHONIOENCODING = 'utf-8'
chcp 65001 | Out-Null

# フォント設定（可能な場合）
try {
    $host.UI.RawUI.OutputEncoding = [System.Text.Encoding]::UTF8
} catch {}

Write-Host "=== Virtual Thread Benchmark ===" -ForegroundColor Cyan

# 1. スレッド情報の確認
Write-Host "`n1. Thread Information..." -ForegroundColor Yellow
$threadInfo = Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/thread-info" -Method Get
Write-Host "Total Threads: $($threadInfo.totalThreads)" -ForegroundColor Green
Write-Host "Virtual Threads: $($threadInfo.virtualThreads)" -ForegroundColor Green
Write-Host "Platform Threads: $($threadInfo.platformThreads)" -ForegroundColor Green
Write-Host "Memory Usage: $($threadInfo.totalMemoryMB) MB" -ForegroundColor Green

# 2. Virtual Threadでのベンチマーク
Write-Host "`n2. Virtual Thread I/O Simulation Benchmark..." -ForegroundColor Yellow
$virtualThreadResult = Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=100" -Method Post
Write-Host "Total Time: $($virtualThreadResult.totalTimeMs) ms" -ForegroundColor Green
Write-Host "Throughput: $([math]::Round($virtualThreadResult.throughput, 2)) req/sec" -ForegroundColor Green
Write-Host "Virtual Thread Count: $($virtualThreadResult.virtualThreadCount)" -ForegroundColor Green

# 3. プラットフォームスレッドでのベンチマーク（比較用）
Write-Host "`n3. Platform Thread Benchmark (for comparison)..." -ForegroundColor Yellow
$platformThreadResult = Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation-platform-threads?concurrentRequests=1000&ioDelayMs=100&threadPoolSize=10" -Method Post
Write-Host "Total Time: $($platformThreadResult.totalTimeMs) ms" -ForegroundColor Green
Write-Host "Throughput: $([math]::Round($platformThreadResult.throughput, 2)) req/sec" -ForegroundColor Green
Write-Host "Virtual Thread Count: $($platformThreadResult.virtualThreadCount)" -ForegroundColor Green

# 4. 比較結果
Write-Host "`n=== Comparison Results ===" -ForegroundColor Cyan
$speedup = [math]::Round($platformThreadResult.totalTimeMs / $virtualThreadResult.totalTimeMs, 2)
Write-Host "Speed Improvement: ${speedup}x" -ForegroundColor Magenta
Write-Host "Throughput Improvement: $([math]::Round($virtualThreadResult.throughput / $platformThreadResult.throughput, 2))x" -ForegroundColor Magenta

# 5. 実際の注文処理での負荷テスト
Write-Host "`n4. Preparing Inventory..." -ForegroundColor Yellow
$inventoryBody = @{
    productId = "product-001"
    quantity = 1000
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory" -Method Post -Body $inventoryBody -ContentType "application/json" | Out-Null
    Write-Host "Inventory prepared" -ForegroundColor Green
} catch {
    Write-Host "Inventory already exists or error occurred: $_" -ForegroundColor Yellow
}

Write-Host "`n5. Concurrent Order Processing Test (Virtual Thread)..." -ForegroundColor Yellow
Write-Host "WARNING: This test may not show clear Virtual Thread advantages due to database operations being the main bottleneck" -ForegroundColor Yellow
Write-Host "For clearer comparison, use the I/O simulation benchmark (section 2) instead" -ForegroundColor Cyan
$loadTestResult = Invoke-RestMethod -Uri "http://localhost:8080/api/load-test/orders?requestCount=200&productId=product-001&ioDelayMs=500" -Method Post
Write-Host "Total Requests: $($loadTestResult.totalRequests)" -ForegroundColor Green
Write-Host "Success Count: $($loadTestResult.successCount)" -ForegroundColor Green
Write-Host "Failure Count: $($loadTestResult.failureCount)" -ForegroundColor Green
Write-Host "Total Time: $($loadTestResult.totalTimeMs) ms" -ForegroundColor Green
Write-Host "Throughput: $([math]::Round($loadTestResult.throughput, 2)) req/sec" -ForegroundColor Green
Write-Host "Virtual Thread Count: $($loadTestResult.virtualThreadCount)" -ForegroundColor Green

Write-Host "`n5-2. Concurrent Order Processing Test (Platform Threads - for comparison)..." -ForegroundColor Yellow
$loadTestResultPlatform = Invoke-RestMethod -Uri "http://localhost:8080/api/load-test/orders-platform-threads?requestCount=200&productId=product-001&threadPoolSize=10&ioDelayMs=500" -Method Post
Write-Host "Total Requests: $($loadTestResultPlatform.totalRequests)" -ForegroundColor Green
Write-Host "Success Count: $($loadTestResultPlatform.successCount)" -ForegroundColor Green
Write-Host "Failure Count: $($loadTestResultPlatform.failureCount)" -ForegroundColor Green
Write-Host "Total Time: $($loadTestResultPlatform.totalTimeMs) ms" -ForegroundColor Green
Write-Host "Throughput: $([math]::Round($loadTestResultPlatform.throughput, 2)) req/sec" -ForegroundColor Green
Write-Host "Virtual Thread Count: $($loadTestResultPlatform.virtualThreadCount)" -ForegroundColor Green

Write-Host "`n=== Order Processing Comparison ===" -ForegroundColor Cyan
$orderSpeedup = [math]::Round($loadTestResultPlatform.totalTimeMs / $loadTestResult.totalTimeMs, 2)
Write-Host "Speed Improvement: ${orderSpeedup}x" -ForegroundColor Magenta
Write-Host "Throughput Improvement: $([math]::Round($loadTestResult.throughput / $loadTestResultPlatform.throughput, 2))x" -ForegroundColor Magenta

Write-Host "`n6. Waiting for async event processing to complete..." -ForegroundColor Yellow
Write-Host "Note: Event processing (inventory, shipping, notifications) runs asynchronously on Virtual Threads" -ForegroundColor Gray
Start-Sleep -Seconds 3

Write-Host "`n7. Verifying Data Registration..." -ForegroundColor Yellow
try {
    # Check inventory status
    $inventoryStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory/product-001" -Method Get
    Write-Host "Inventory Status:" -ForegroundColor Cyan
    Write-Host "  Available: $($inventoryStatus.availableQuantity)" -ForegroundColor Green
    Write-Host "  Reserved: $($inventoryStatus.reservedQuantity)" -ForegroundColor Green
    
    # Check notifications (sample customer)
    $notifications = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/customer-0" -Method Get
    Write-Host "`nNotifications for customer-0: $($notifications.Count) items" -ForegroundColor Cyan
    if ($notifications.Count -gt 0) {
        $notifications | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - $($_.type): $($_.message)" -ForegroundColor Green
        }
    }
    
    Write-Host "`nData verification complete!" -ForegroundColor Green
    Write-Host "Note: Orders, inventory, shipments, and notifications were created asynchronously using Virtual Threads" -ForegroundColor Gray
} catch {
    Write-Host "Data verification failed: $_" -ForegroundColor Yellow
    Write-Host "This may be normal if async processing is still in progress" -ForegroundColor Gray
}

Write-Host "`n=== Benchmark Complete ===" -ForegroundColor Cyan
