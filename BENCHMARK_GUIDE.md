# Virtual Thread ベンチマークガイド

このガイドでは、Virtual Threadの優位性を確認するための実践的な手順を説明します。

## 前提条件

- アプリケーションが起動していること（`http://localhost:8080`）
- `curl`コマンドまたはHTTPクライアント（Postman、Thunder Clientなど）が利用可能
- Java 21以上が使用されていること

## PowerShellユーザー向け

PowerShellでは`curl`は`Invoke-WebRequest`のエイリアスで、`-X`オプションが使用できません。
以下のいずれかの方法を使用してください：

### 方法1: スクリプトを使用（推奨）

```powershell
.\benchmark.ps1
```

このスクリプトは、すべてのベンチマークを自動実行し、結果を比較表示します。

### 方法2: Invoke-RestMethodを使用

```powershell
# スレッド情報の確認
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/thread-info" -Method Get

# Virtual Threadでのベンチマーク
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=100" -Method Post

# 注文処理のテスト
Invoke-RestMethod -Uri "http://localhost:8080/api/load-test/orders?requestCount=50&productId=product-001" -Method Post
```

## 1. スレッド情報の確認

現在のスレッド状態を確認します。

### コマンド

**Linux/Mac/Git Bash:**
```bash
curl http://localhost:8080/api/benchmark/thread-info
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/thread-info" -Method Get
```

### 期待される結果

```json
{
  "totalThreads": 25,
  "virtualThreads": 0,
  "platformThreads": 25,
  "namedVirtualThreads": 0,
  "availableProcessors": 16,
  "totalMemoryMB": 68,
  "freeMemoryMB": 15,
  "maxMemoryMB": 3862
}
```

**重要な注意点：**
- `virtualThreads`が0でも問題ありません。Virtual Threadは実行が完了するとすぐに終了するため、このエンドポイントでは**実行中のVirtual Threadのみ**が表示されます
- ベンチマーク実行中のVirtual Thread数を確認するには、**ベンチマーク結果の`virtualThreadCount`フィールド**を参照してください
- ベンチマーク実行中にこのエンドポイントを呼び出すと、実行中のVirtual Threadが表示される可能性があります

**ポイント：**
- `virtualThreads`: 現在実行中のVirtual Threadの数
- `platformThreads`: プラットフォームスレッド（OSスレッド）の数
- Virtual Threadは軽量なため、大量に作成可能

## 2. I/Oシミュレーションベンチマーク（Virtual Thread vs プラットフォームスレッド）

このベンチマークは、**I/O待機処理のスループット**を計測しています。

### 計測している処理

- **I/O待機のシミュレート**: `Thread.sleep(ioDelayMs)`を使用して、データベースクエリや外部API呼び出しなどのI/O待機をシミュレート
- **並行処理**: 指定された数のリクエストを同時に実行
- **スループット**: すべてのリクエストが完了するまでの時間から、1秒あたりの処理数を計算

**具体的な処理内容：**
```java
// 各リクエストで実行される処理
Thread.sleep(ioDelayMs);  // I/O待機をシミュレート（デフォルト100ms）
```

これは、以下のような実際のI/O処理をシミュレートしています：
- データベースクエリの実行
- HTTP API呼び出し
- ファイルI/O
- ネットワーク通信

### 2-1. Virtual Threadでのベンチマーク

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=100"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=100" -Method Post
```

**パラメータ：**
- `concurrentRequests`: 並行リクエスト数（デフォルト: 100）
- `ioDelayMs`: I/O待機時間（ミリ秒、デフォルト: 100）

**レスポンス例：**
```json
{
  "concurrentRequests": 1000,
  "totalTimeMs": 102,
  "virtualThreadCount": 1000,
  "platformThreadCount": 0,
  "throughput": 9803.92
}
```

### 2-2. プラットフォームスレッドでのベンチマーク（比較用）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation-platform-threads?concurrentRequests=1000&ioDelayMs=100&threadPoolSize=10"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation-platform-threads?concurrentRequests=1000&ioDelayMs=100&threadPoolSize=10" -Method Post
```

**パラメータ：**
- `concurrentRequests`: 並行リクエスト数
- `ioDelayMs`: I/O待機時間（ミリ秒）
- `threadPoolSize`: スレッドプールサイズ（デフォルト: 10）

**レスポンス例：**
```json
{
  "concurrentRequests": 1000,
  "totalTimeMs": 10000,
  "virtualThreadCount": 0,
  "platformThreadCount": 1000,
  "throughput": 100.0
}
```

### 期待される結果の比較

| 項目 | Virtual Thread | プラットフォームスレッド（10スレッド） |
|------|----------------|--------------------------------------|
| 総処理時間 | 約100-120ms | 約10秒（1000リクエスト ÷ 10スレッド × 100ms） |
| スループット | 約8,000-10,000 req/sec | 約100 req/sec |
| Virtual Thread数 | 1000個（すべてVirtual Thread） | 0個（すべてプラットフォームスレッド） |
| スレッドプール制約 | なし | 10スレッドの制約あり |

**なぜこの差が生まれるのか：**

**Virtual Thread:**
- I/O待機中（`Thread.sleep()`実行中）にスレッドがブロックされない
- 1000個のVirtual Threadを同時に作成して並行処理できる
- すべてのリクエストがほぼ同時にI/O待機を開始し、ほぼ同時に完了する
- **結果**: 1000リクエスト × 100ms待機 = 約100msで完了（スループット約10,000 req/sec）

**プラットフォームスレッド:**
- スレッドプールのサイズ（10スレッド）に制約がある
- I/O待機中もスレッドが占有されるため、他の処理をブロック
- 1000リクエストを10スレッドで順次処理する必要がある
- **結果**: 1000リクエスト ÷ 10スレッド × 100ms待機 = 約10秒で完了（スループット約100 req/sec）

**スループットの計算式：**
```
スループット = リクエスト数 / 総処理時間(秒) = リクエスト数 / (総処理時間(ms) / 1000) * 1000
```

**例（1000リクエスト、100ms待機）：**
- Virtual Thread: 1000 / 0.1秒 = 10,000 req/sec
- プラットフォームスレッド（10スレッド）: 1000 / 10秒 = 100 req/sec

## 3. 実際の注文処理での負荷テスト

実際のビジネスロジックを使用した負荷テストです。

**⚠️ 重要な注意点：**

このテストでは、I/O待機（デフォルト500ms）をシミュレートして追加していますが、**実際の注文処理ではデータベース操作やトランザクション処理が主なボトルネックになるため、Virtual Threadの優位性が明確に現れない場合があります。**

**なぜ差が出にくいのか：**
1. **データベース操作が主なボトルネック**: H2インメモリデータベースでも、トランザクション処理、データベースロック、イベント発行などが含まれるため、I/O待機以外の処理時間が大きい
2. **CPU処理が含まれる**: ビジネスロジックの実行、データ変換、バリデーションなど、CPU処理が含まれる
3. **スレッドプールの制約が小さい**: データベース操作が主なボトルネックの場合、スレッドプールの制約による影響が相対的に小さくなる
4. **トランザクション時間が長い**: データベース操作の時間がI/O待機時間よりも長い場合、I/O待機の影響が相対的に小さくなる

**推奨：**
- **⚠️ Virtual Threadの優位性を明確に確認したい場合は、必ずセクション2の「I/Oシミュレーションベンチマーク」を使用してください**
- このテストは、実際のビジネスロジックでの使用例を示すためのものであり、Virtual Threadの優位性を確認するためのものではありません
- 実際の本番環境では、外部API呼び出しやネットワーク通信など、より多くのI/O待機が発生するため、Virtual Threadの優位性がより明確に現れる可能性があります

**I/O待機時間の調整：**
- `ioDelayMs`パラメータでI/O待機時間を調整できます（デフォルト: 500ms）
- より大きな値（例：1000ms、2000ms）に設定すると、差が明確になる可能性がありますが、それでもデータベース操作が主なボトルネックの場合は差が出にくい場合があります

### 3-1. 在庫の準備

**Linux/Mac/Git Bash:**
```bash
curl -X POST http://localhost:8080/api/orders/inventory \
  -H "Content-Type: application/json" \
  -d "{\"productId\": \"product-001\", \"quantity\": 1000}"
```

**PowerShell:**
```powershell
$body = @{
    productId = "product-001"
    quantity = 1000
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory" -Method Post -Body $body -ContentType "application/json"
```

**注意：** フィールド名は`quantity`です（`initialQuantity`ではありません）。

### 3-2. 並行注文処理のテスト（Virtual Thread）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/load-test/orders?requestCount=200&productId=product-001"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/load-test/orders?requestCount=200&productId=product-001&ioDelayMs=500" -Method Post
```

**パラメータ：**
- `requestCount`: 並行で作成する注文数（デフォルト: 200）
- `productId`: 商品ID（デフォルト: product-001）
- `ioDelayMs`: I/O待機時間（ミリ秒、デフォルト: 500）。より大きな値（1000ms、2000ms）に設定すると、差が明確になる可能性があります

**⚠️ 重要な注意：** 
- 実際の注文処理では、データベース操作が主なボトルネックになるため、**差が明確に現れない場合があります**
- **Virtual Threadの優位性を確認するには、必ずセクション2の「I/Oシミュレーションベンチマーク」を使用してください**
- このテストは、実際のビジネスロジックでの使用例を示すためのものであり、Virtual Threadの優位性を確認するためのものではありません

### 3-3. 並行注文処理のテスト（プラットフォームスレッド - 比較用）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/load-test/orders-platform-threads?requestCount=200&productId=product-001&threadPoolSize=10"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/load-test/orders-platform-threads?requestCount=200&productId=product-001&threadPoolSize=10&ioDelayMs=500" -Method Post
```

**パラメータ：**
- `requestCount`: 並行で作成する注文数（デフォルト: 200）
- `productId`: 商品ID（デフォルト: product-001）
- `threadPoolSize`: スレッドプールサイズ（デフォルト: 10）
- `ioDelayMs`: I/O待機時間（ミリ秒、デフォルト: 500）

**⚠️ 重要な注意：** 
- このテストでは、データベース操作が主なボトルネックになるため、Virtual Threadの優位性が明確に現れない場合があります
- **Virtual Threadの優位性を確認するには、必ずセクション2の「I/Oシミュレーションベンチマーク」を使用してください**

### 期待される結果

```json
{
  "totalRequests": 50,
  "successCount": 50,
  "failureCount": 0,
  "totalTimeMs": 250,
  "virtualThreadCount": 50,
  "throughput": 200.0
}
```

**ポイント：**
- `virtualThreadCount`: Virtual Threadで実行された処理数（50になれば正常）
- 各注文処理は非同期で実行され、Virtual Threadで処理される
- イベント駆動アーキテクチャにより、在庫確保、配送、通知が非同期で処理される

## 4. データ登録の確認

Virtual Threadで実行された処理によってデータが正しく登録されていることを確認する方法です。

### 4-1. REST APIで確認（推奨）

#### 注文データの確認

**Linux/Mac/Git Bash:**
```bash
# 注文を作成（注文IDを取得）
ORDER_ID=$(curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "customer-001", "productId": "product-001", "quantity": 2, "unitPrice": 1000.00}' \
  | jq -r '.id')

# 注文情報を取得
curl http://localhost:8080/api/orders/$ORDER_ID

# 配送情報を取得
curl http://localhost:8080/api/orders/$ORDER_ID/shipment
```

**PowerShell:**
```powershell
# 注文を作成
$order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body (@{
    customerId = "customer-001"
    productId = "product-001"
    quantity = 2
    unitPrice = 1000.00
} | ConvertTo-Json) -ContentType "application/json"

# 注文情報を取得
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)" -Method Get

# 配送情報を取得
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)/shipment" -Method Get
```

#### 在庫データの確認

**Linux/Mac/Git Bash:**
```bash
curl http://localhost:8080/api/orders/inventory/product-001
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory/product-001" -Method Get
```

#### 通知データの確認

**Linux/Mac/Git Bash:**
```bash
curl http://localhost:8080/api/notifications/customer-001
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/customer-001" -Method Get
```

### 4-2. H2コンソールで確認

データベースに直接アクセスしてデータを確認できます。

1. **H2コンソールにアクセス**
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:testdb`
   - ユーザー名: `sa`
   - パスワード: (空欄)

2. **テーブルを確認**
   ```sql
   -- 注文テーブル
   SELECT * FROM orders;
   
   -- 在庫テーブル
   SELECT * FROM inventory_item;
   
   -- 配送テーブル
   SELECT * FROM shipment;
   
   -- 通知テーブル
   SELECT * FROM notification;
   
   -- Spring Modulithイベントテーブル（イベントが永続化されている場合）
   SELECT * FROM event_publication;
   ```

3. **データの確認ポイント**
   - `orders`テーブル: 注文が作成され、ステータスが更新されているか
   - `inventory_item`テーブル: 在庫の確保・解放が正しく反映されているか
   - `shipment`テーブル: 配送情報が作成されているか
   - `notification`テーブル: 通知が送信されているか

### 4-3. ログで確認

アプリケーションのログで、各処理がVirtual Threadで実行され、データが保存されていることを確認できます。

**ログの確認ポイント：**
- `注文を作成しました: 注文ID=xxx` - 注文が作成された
- `在庫を確保しました: 注文ID=xxx` - 在庫確保が完了した
- `注文を確定しました: 注文ID=xxx` - 注文が確定した
- `配送情報を作成しました: 注文ID=xxx` - 配送情報が作成された
- `通知を送信しました: 通知ID=xxx` - 通知が送信された

### 4-4. 完全なフロー確認の例

**PowerShellスクリプト例：**
```powershell
# 1. 在庫を準備
$inventory = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory" -Method Post -Body (@{
    productId = "product-001"
    quantity = 100
} | ConvertTo-Json) -ContentType "application/json"
Write-Host "在庫を作成しました: $($inventory | ConvertTo-Json)"

# 2. 注文を作成
$order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body (@{
    customerId = "customer-001"
    productId = "product-001"
    quantity = 2
    unitPrice = 1000.00
} | ConvertTo-Json) -ContentType "application/json"
Write-Host "注文を作成しました: $($order | ConvertTo-Json)"

# 3. 少し待機（非同期処理の完了を待つ）
# 注意: イベント処理（在庫確保、配送、通知）は非同期で実行されるため、
# データが完全に登録されるまで数秒かかることがあります
Start-Sleep -Seconds 3

# 4. 注文状態を確認
$orderStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)" -Method Get
Write-Host "注文状態: $($orderStatus.status)"

# 5. 配送情報を確認
try {
    $shipment = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)/shipment" -Method Get
    Write-Host "配送情報: $($shipment | ConvertTo-Json)"
} catch {
    Write-Host "配送情報はまだ作成されていません"
}

# 6. 通知を確認
$notifications = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/customer-001" -Method Get
Write-Host "通知数: $($notifications.Count)"
$notifications | ForEach-Object { Write-Host "  - $($_.type): $($_.message)" }

# 7. 在庫を確認
$inventoryStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/inventory/product-001" -Method Get
Write-Host "在庫状態: 利用可能=$($inventoryStatus.availableQuantity), 確保済み=$($inventoryStatus.reservedQuantity)"
```

**期待される結果：**
- 注文が`CONFIRMED`または`SHIPPED`ステータスになっている
- 配送情報が作成されている（追跡番号が発行されている）
- 通知が複数件登録されている（ORDER_CREATED, ORDER_CONFIRMED, SHIPMENT_CREATEDなど）
- 在庫の`reservedQuantity`が増加している

**重要な注意点：非同期処理のタイミング**

このアプリケーションでは、以下のように非同期処理が連鎖しています：

1. **注文作成**（同期）: `OrderService.createOrder()`で注文をデータベースに保存
2. **イベント発行**（非同期）: `@Async`で`OrderCreatedEvent`を発行
3. **在庫確保**（非同期）: `InventoryEventListener`がイベントを受信して在庫を確保
4. **注文確定**（非同期）: `OrderEventListener`が在庫確保イベントを受信して注文を確定
5. **配送作成**（非同期）: `ShippingEventListener`が注文確定イベントを受信して配送情報を作成
6. **通知送信**（非同期）: `NotificationEventListener`が各イベントを受信して通知を送信

**ベンチマーク完了のログが出た時点では：**
- ✅ 注文の作成は完了している（データベースに保存済み）
- ⏳ イベント処理（在庫確保、配送、通知）はまだ実行中またはこれから実行される

これは正常な動作です。非同期処理の特性により、注文作成は完了していますが、その後のイベント処理は別のVirtual Threadで実行されるため、完了までに時間がかかります。

**データ確認のタイミング：**
- 注文データ: ベンチマーク完了直後に確認可能
- 在庫データ: 数秒待ってから確認（イベント処理の完了を待つ）
- 配送データ: 数秒待ってから確認
- 通知データ: 数秒待ってから確認

## 5. ログでの確認

アプリケーションのログを確認すると、各非同期処理がVirtual Threadで実行されていることが分かります。

### ログ例（ベンチマーク実行時）

```
INFO  I/Oシミュレーションベンチマーク開始: 並行リクエスト数=10, I/O待機時間=100ms
DEBUG Request-0 executing on thread: 'unnamed' (Virtual: true)
DEBUG Request-1 executing on thread: 'unnamed' (Virtual: true)
...
INFO  Request-0 executed on thread: 'unnamed' (Virtual: true)
INFO  Request-1 executed on thread: 'unnamed' (Virtual: true)
...
INFO  Thread execution summary: Total=10, Virtual=10, Platform=0
INFO  ベンチマーク完了: 総時間=117ms, スループット=85.47 req/sec, Virtual Thread数=10
```

### ログ例（実際の注文処理時）

```
INFO  注文を作成中: 顧客ID=customer-1, 商品ID=product-001, 数量=1
INFO  OrderCreatedEventを発行中: 注文ID=xxx
INFO  OrderCreatedEventを受信しました: 商品ID=product-001, 数量=1 (スレッド: virtual-thread-2)
INFO  在庫を確保しました: 注文ID=xxx
INFO  通知を送信中: 受信者ID=customer-1, タイプ=ORDER_CREATED (スレッド: virtual-thread-4)
```

**ポイント：**
- ログに`(Virtual: true)`と表示されていれば、Virtual Threadで実行されている
- `Thread execution summary: Total=10, Virtual=10, Platform=0`のように、すべての処理がVirtual Threadで実行されていることを確認できる
- ベンチマーク結果の`virtualThreadCount`がリクエスト数と一致していれば正常

## 6. 段階的な負荷テスト

段階的に負荷を上げて、Virtual Threadのスケーラビリティを確認します。

### テスト1: 小規模（100リクエスト）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=100&ioDelayMs=50"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=100&ioDelayMs=50" -Method Post
```

### テスト2: 中規模（1,000リクエスト）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=50"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=50" -Method Post
```

### テスト3: 大規模（10,000リクエスト）

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=10000&ioDelayMs=50"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=10000&ioDelayMs=50" -Method Post
```

**期待される結果：**
- **Virtual Thread**: スケールに応じて処理時間がほぼ一定（I/O待機時間に近い）。100リクエストでも10,000リクエストでも、ほぼ同じ時間で処理できる
- **プラットフォームスレッド**: スレッドプールの制約により、リクエスト数が増えると処理時間が線形に増加

## 7. メモリ使用量の確認

大量のリクエストを処理する際のメモリ使用量を確認します。

### ベンチマーク前のスレッド情報

**Linux/Mac/Git Bash:**
```bash
curl http://localhost:8080/api/benchmark/thread-info
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/thread-info" -Method Get
```

### ベンチマーク実行

**Linux/Mac/Git Bash:**
```bash
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=10000&ioDelayMs=100"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=10000&ioDelayMs=100" -Method Post
```

### ベンチマーク後のスレッド情報

**Linux/Mac/Git Bash:**
```bash
curl http://localhost:8080/api/benchmark/thread-info
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/benchmark/thread-info" -Method Get
```

**ポイント：**
- Virtual Threadは軽量（数KB）のため、大量に作成してもメモリ使用量が少ない
- プラットフォームスレッドは1MB程度のスタックメモリが必要
- 10,000個のVirtual Threadを作成しても、メモリ使用量の増加はわずか

## 8. ベンチマーク結果の解釈

### レスポンスフィールドの説明

```json
{
  "concurrentRequests": 1000,      // 並行リクエスト数
  "totalTimeMs": 102,              // 総処理時間（ミリ秒）
  "virtualThreadCount": 1000,     // Virtual Threadで実行された処理数（重要！）
  "platformThreadCount": 0,        // プラットフォームスレッドで実行された処理数
  "throughput": 9803.92            // スループット（req/sec）
}
```

**重要な確認ポイント：**
- **`virtualThreadCount`**: これが`concurrentRequests`と一致していれば、すべての処理がVirtual Threadで実行されている
- **`totalTimeMs`**: I/O待機時間（`ioDelayMs`）に近い値になれば、並行処理が効率的に実行されている
- **`throughput`**: 高いほど良い。Virtual Threadでは通常、プラットフォームスレッドの10-100倍のスループットを実現

### 正常な結果の例

**Virtual Thread（1000リクエスト、100ms待機）:**
- `virtualThreadCount`: 1000
- `totalTimeMs`: 100-120ms
- `throughput`: 8,000-10,000 req/sec

**プラットフォームスレッド（1000リクエスト、100ms待機、10スレッド）:**
- `virtualThreadCount`: 0
- `totalTimeMs`: 約10,000ms
- `throughput`: 約100 req/sec

## 9. まとめ：Virtual Threadの優位性

### 1. スループットの向上
- I/O待機が多い処理で、従来のスレッドプールよりも高いスループットを実現
- スレッド数の制約がないため、大量の並行処理が可能
- **実測値**: プラットフォームスレッドの10-100倍のスループット

### 2. リソース効率
- プラットフォームスレッド（OSスレッド）は1MB程度のスタックメモリが必要
- Virtual Threadは数KB程度で、大量のスレッドを作成可能
- **実測値**: 10,000個のVirtual Threadでも、メモリ使用量の増加はわずか

### 3. スケーラビリティ
- 100万以上のVirtual Threadを作成可能
- プラットフォームスレッドは通常数千が上限
- **実測値**: 10,000リクエストでも、処理時間はほぼ一定

### 4. シンプルなコード
- `Executors.newVirtualThreadPerTaskExecutor()`を使用するだけで、自動的にVirtual Threadで実行される
- `@Async`アノテーションと組み合わせることで、Springの非同期処理でもVirtual Threadを活用可能
- 明示的なスレッドプール管理が不要

## トラブルシューティング

### エラー: "Connection refused"
- アプリケーションが起動しているか確認
- ポート8080が使用可能か確認

### エラー: "Timeout"
- リクエスト数やI/O待機時間を減らして再試行
- システムリソース（メモリ、CPU）を確認

### `virtualThreadCount`が0になる

**原因1: アプリケーションが再起動されていない**
- コードを変更した場合は、アプリケーションを再起動してください

**原因2: Java 21が使用されていない**
- `java -version`でJava 21以上が使用されているか確認
- IDEの設定でJava 21が使用されているか確認

**原因3: Virtual Threadが無効になっている**
- `application.yml`で`spring.threads.virtual.enabled: true`が設定されているか確認
- ただし、`Executors.newVirtualThreadPerTaskExecutor()`を使用している場合は、この設定は不要です

**原因4: ログで確認**
- アプリケーションのログで`(Virtual: true)`が表示されているか確認
- `Thread execution summary: Total=X, Virtual=X, Platform=0`が表示されていれば正常

### `thread-info`で`virtualThreads`が0になる

これは正常な動作です。Virtual Threadは実行が完了するとすぐに終了するため、`thread-info`エンドポイントでは実行中のVirtual Threadのみが表示されます。

ベンチマーク実行中のVirtual Thread数を確認するには、**ベンチマーク結果の`virtualThreadCount`フィールド**を参照してください。

### 期待通りの結果が得られない場合

1. **ログを確認**: アプリケーションのログで`(Virtual: true)`が表示されているか確認
2. **ベンチマーク結果を確認**: `virtualThreadCount`がリクエスト数と一致しているか確認
3. **Java 21を確認**: `java -version`でJava 21以上が使用されているか確認
4. **アプリケーションを再起動**: コードを変更した場合は、アプリケーションを再起動してください

## 参考

- [Java Virtual Threads Documentation](https://openjdk.org/jeps/444)
- [Spring Boot Virtual Threads](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.spring-application.threads.virtual)
- [Project Loom: Virtual Threads](https://openjdk.org/projects/loom/)