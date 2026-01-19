# Spring Modulith + Virtual Thread サンプルプロジェクト

このプロジェクトは、Spring ModulithとJava Virtual Threadを組み合わせたサンプルアプリケーションです。
モジュール化されたモノリスアーキテクチャで、非同期イベント処理にVirtual Threadを活用しています。

## プロジェクト概要

注文処理システムを例に、以下の4つのモジュールで構成されています：

1. **Order Module** (注文モジュール)
   - 注文の作成と管理
   - 注文ステータスの更新

2. **Inventory Module** (在庫モジュール)
   - 在庫の管理
   - 注文に対する在庫確保処理

3. **Shipping Module** (配送モジュール)
   - 配送情報の管理
   - 配送プロセスの処理

4. **Notification Module** (通知モジュール)
   - 顧客への通知送信
   - 通知履歴の管理

## アーキテクチャの特徴

### Spring Modulith
- モジュール間の境界を明確に定義
- イベント駆動アーキテクチャによる疎結合な連携
- モジュールの独立性を保ちながら、モノリスとして動作

### Virtual Thread
- Java 21のVirtual Threadを活用
- I/O待機が多い非同期処理に最適
- 従来のスレッドプールよりも効率的なリソース利用

## 技術スタック

- Java 21
- Spring Boot 3.2.0
- Spring Modulith 1.1.0
- Spring Data JPA
- H2 Database (インメモリ)
- Maven

## セットアップと実行

### 前提条件

- Java 21以上
- Maven 3.6以上

### ビルドと実行

```bash
# プロジェクトのビルド
mvn clean install

# アプリケーションの起動
mvn spring-boot:run
```

アプリケーションは `http://localhost:8080` で起動します。

### H2コンソール

データベースの状態を確認するには、以下のURLにアクセスしてください：
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- ユーザー名: `sa`
- パスワード: (空欄)

## API エンドポイント

### 注文関連

#### 注文の作成
```bash
POST /api/orders
Content-Type: application/json

{
  "customerId": "customer-001",
  "productId": "product-001",
  "quantity": 2,
  "unitPrice": 1000.00
}
```

#### 注文の取得
```bash
GET /api/orders/{orderId}
```

#### 配送情報の取得
```bash
GET /api/orders/{orderId}/shipment
```

### 在庫関連

#### 在庫アイテムの作成
```bash
POST /api/orders/inventory
Content-Type: application/json

{
  "productId": "product-001",
  "quantity": 100
}
```

#### 在庫情報の取得
```bash
GET /api/orders/inventory/{productId}
```

### 通知関連

#### 通知履歴の取得
```bash
GET /api/notifications/{recipientId}
```

## 使用例

### 1. 在庫の作成

```bash
curl -X POST http://localhost:8080/api/orders/inventory \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "product-001",
    "quantity": 100
  }'
```

### 2. 注文の作成

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "productId": "product-001",
    "quantity": 2,
    "unitPrice": 1000.00
  }'
```

注文を作成すると、以下の非同期処理がVirtual Threadで実行されます：

1. **Order Module**: 注文を作成し、`OrderCreatedEvent`を発行
2. **Inventory Module**: 在庫を確保し、結果に応じてイベントを発行
3. **Order Module**: 在庫確保結果を受け取り、注文を確定またはキャンセル
4. **Shipping Module**: 注文確定後、配送情報を作成
5. **Notification Module**: 各ステップで顧客に通知を送信

### 3. 注文状態の確認

```bash
curl http://localhost:8080/api/orders/{orderId}
```

### 4. 通知履歴の確認

```bash
curl http://localhost:8080/api/notifications/customer-001
```

## イベントフロー

```
[Order Created]
    ↓
[OrderCreatedEvent] → [Inventory Module] → [InventoryReservedEvent] or [InventoryReservationFailedEvent]
    ↓                                              ↓
[Notification]                              [Order Module]
    ↓                                              ↓
                                         [OrderConfirmedEvent]
                                                  ↓
                                         [Shipping Module]
                                                  ↓
                                         [ShipmentCreatedEvent]
                                                  ↓
                                         [Notification]
```

## Virtual Threadの確認

ログを確認すると、各非同期処理がVirtual Threadで実行されていることが分かります：

```
INFO ... (Thread: virtual-thread-0)
INFO ... (Thread: virtual-thread-1)
```

従来のスレッドプールと異なり、Virtual Threadは軽量で、大量のI/O待機処理を効率的に処理できます。

## Virtual Threadの優位性を確認する方法

### 1. スレッド情報の確認

現在のスレッド状態を確認できます：

```bash
curl http://localhost:8080/api/benchmark/thread-info
```

レスポンス例：
```json
{
  "totalThreads": 45,
  "virtualThreads": 12,
  "platformThreads": 33,
  "namedVirtualThreads": 12,
  "availableProcessors": 8,
  "totalMemoryMB": 512,
  "freeMemoryMB": 256,
  "maxMemoryMB": 1024
}
```

### 2. I/Oシミュレーションベンチマーク

大量のI/O待機処理を並行実行して、Virtual Threadの効果を確認：

```bash
# Virtual Threadを使用（デフォルト）
curl -X POST "http://localhost:8080/api/benchmark/io-simulation?concurrentRequests=1000&ioDelayMs=100"

# 従来のスレッドプールと比較（参考）
curl -X POST "http://localhost:8080/api/benchmark/io-simulation-platform-threads?concurrentRequests=1000&ioDelayMs=100&threadPoolSize=10"
```

**期待される結果：**
- **Virtual Thread**: 1000リクエストを約100msで処理（I/O待機時間に近い）
- **プラットフォームスレッド（10スレッド）**: 1000リクエストを約10秒で処理（スレッドプールの制約により遅延）

### 3. 実際の注文処理での負荷テスト

実際のビジネスロジックを使用した負荷テスト：

```bash
# 在庫を準備
curl -X POST http://localhost:8080/api/orders/inventory \
  -H "Content-Type: application/json" \
  -d '{"productId": "product-001", "quantity": 1000}'

# 50件の注文を並行処理
curl -X POST "http://localhost:8080/api/load-test/orders?requestCount=50&productId=product-001"
```

### 4. ログでの確認

アプリケーションのログを確認すると、各非同期処理がVirtual Threadで実行されていることが分かります：

```
INFO 注文を作成中: 顧客ID=customer-1, 商品ID=product-001, 数量=1 (スレッド: virtual-thread-0)
INFO OrderCreatedEventを発行中: 注文ID=xxx (スレッド: virtual-thread-1)
INFO OrderCreatedEventを受信しました: 商品ID=product-001, 数量=1 (スレッド: virtual-thread-2)
```

### Virtual Threadの優位性

1. **スループットの向上**
   - I/O待機が多い処理で、従来のスレッドプールよりも高いスループットを実現
   - スレッド数の制約がないため、大量の並行処理が可能

2. **リソース効率**
   - プラットフォームスレッド（OSスレッド）は1MB程度のスタックメモリが必要
   - Virtual Threadは数KB程度で、大量のスレッドを作成可能

3. **スケーラビリティ**
   - 100万以上のVirtual Threadを作成可能
   - プラットフォームスレッドは通常数千が上限

### ベンチマーク結果の解釈

- **スループット**: 1秒あたりの処理数（高いほど良い）
- **総処理時間**: すべてのリクエストが完了するまでの時間（短いほど良い）
- **Virtual Thread数**: Virtual Threadで実行された処理数（多いほど良い）

**比較例：**
- Virtual Thread: 1000リクエストを100msで処理 → スループット 10,000 req/sec
- プラットフォームスレッド（10スレッド）: 1000リクエストを10秒で処理 → スループット 100 req/sec

## プロジェクト構造

```
src/main/java/com/example/
├── api/                    # REST APIコントローラー
├── config/                 # 設定クラス（Virtual Thread設定など）
├── order/                  # Order Module
│   ├── Order.java
│   ├── OrderService.java
│   └── ...
├── inventory/              # Inventory Module
│   ├── InventoryItem.java
│   ├── InventoryService.java
│   └── ...
├── shipping/               # Shipping Module
│   ├── Shipment.java
│   ├── ShippingService.java
│   └── ...
└── notification/           # Notification Module
    ├── Notification.java
    ├── NotificationService.java
    └── ...
```

## 学習ポイント

1. **Spring Modulithのモジュール化**
   - パッケージベースのモジュール境界
   - `@ApplicationModuleListener`によるモジュール間イベント連携

2. **Virtual Threadの活用**
   - I/O待機が多い処理での効率化
   - `@Async`アノテーションとの組み合わせ

3. **イベント駆動アーキテクチャ**
   - モジュール間の疎結合な連携
   - 非同期処理による応答性の向上

## ライセンス

このプロジェクトはサンプルコードとして提供されています。
