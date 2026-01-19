# ベンチマーク条件の比較

このドキュメントでは、Virtual Threadとプラットフォームスレッドのベンチマーク条件が平等かどうかを確認します。

## LoadTestController（注文処理ベンチマーク）

### 1. I/O待機時間（スリープ）

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| パラメータ名 | `ioDelayMs` | `ioDelayMs` | ✅ 同じ |
| デフォルト値 | 500ms | 500ms | ✅ 同じ |
| 実装 | `Thread.sleep(ioDelayMs)` | `Thread.sleep(ioDelayMs)` | ✅ 同じ |
| タイミング | データベース操作の前 | データベース操作の前 | ✅ 同じ |

**結論**: ✅ **平等**

### 2. リクエスト数

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| パラメータ名 | `requestCount` | `requestCount` | ✅ 同じ |
| デフォルト値 | 200 | 200 | ✅ 同じ |

**結論**: ✅ **平等**

### 3. 商品ID

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| パラメータ名 | `productId` | `productId` | ✅ 同じ |
| デフォルト値 | "product-001" | "product-001" | ✅ 同じ |

**結論**: ✅ **平等**

### 4. 在庫準備

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 実装 | `inventoryService.createInventoryItem(...)` | `inventoryService.createInventoryItem(...)` | ✅ 同じ |
| 在庫数量 | `requestCount * 10` | `requestCount * 10` | ✅ 同じ |

**結論**: ✅ **平等**

### 5. 注文作成処理

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| サービス呼び出し | `orderService.createOrder(command)` | `orderService.createOrder(command)` | ✅ 同じ |
| コマンド作成 | 同じパラメータ | 同じパラメータ | ✅ 同じ |

**結論**: ✅ **平等**

### 6. タイミング計測

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 開始時刻 | `Instant.now()` | `Instant.now()` | ✅ 同じ |
| 終了時刻 | `Instant.now()` | `Instant.now()` | ✅ 同じ |
| 計測方法 | `Duration.between(start, end)` | `Duration.between(start, end)` | ✅ 同じ |

**結論**: ✅ **平等**

### 7. 完了待機

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 実装 | `CompletableFuture.allOf(...).join()` | `CompletableFuture.allOf(...).join()` | ✅ 同じ |

**結論**: ✅ **平等**

### 8. スレッドプール（意図的な違い）

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 実装 | `Executors.newVirtualThreadPerTaskExecutor()` | `Executors.newFixedThreadPool(threadPoolSize)` | ⚠️ **意図的な違い** |
| スレッド数 | 無制限（必要に応じて作成） | 制限あり（デフォルト: 10） | ⚠️ **意図的な違い** |
| パラメータ | なし | `threadPoolSize` (デフォルト: 10) | ⚠️ **意図的な違い** |

**結論**: ⚠️ **意図的な違い** - これがVirtual Threadの優位性を示すための重要な違いです。

### 9. 処理の順序

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 1. I/O待機 | `Thread.sleep(ioDelayMs)` | `Thread.sleep(ioDelayMs)` | ✅ 同じ |
| 2. コマンド作成 | `new CreateOrderCommand(...)` | `new CreateOrderCommand(...)` | ✅ 同じ |
| 3. 注文作成 | `orderService.createOrder(command)` | `orderService.createOrder(command)` | ✅ 同じ |

**結論**: ✅ **平等**

## BenchmarkController（I/Oシミュレーションベンチマーク）

### 1. I/O待機時間（スリープ）

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| パラメータ名 | `ioDelayMs` | `ioDelayMs` | ✅ 同じ |
| デフォルト値 | 100ms | 100ms | ✅ 同じ |
| 実装 | `Thread.sleep(ioDelayMs)` | `Thread.sleep(ioDelayMs)` | ✅ 同じ |

**結論**: ✅ **平等**

### 2. 並行リクエスト数

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| パラメータ名 | `concurrentRequests` | `concurrentRequests` | ✅ 同じ |
| デフォルト値 | 100 | 100 | ✅ 同じ |

**結論**: ✅ **平等**

### 3. スレッドプール（意図的な違い）

| 項目 | Virtual Thread | Platform Thread | 状態 |
|------|---------------|-----------------|------|
| 実装 | `Executors.newVirtualThreadPerTaskExecutor()` | `Executors.newFixedThreadPool(threadPoolSize)` | ⚠️ **意図的な違い** |
| スレッド数 | 無制限 | 制限あり（デフォルト: 10） | ⚠️ **意図的な違い** |
| パラメータ | なし | `threadPoolSize` (デフォルト: 10) | ⚠️ **意図的な違い** |

**結論**: ⚠️ **意図的な違い** - これがVirtual Threadの優位性を示すための重要な違いです。

## 総合評価

### ✅ 平等な条件

1. **I/O待機時間（スリープ）**: 両方とも同じ `ioDelayMs` パラメータを使用
2. **リクエスト数**: 両方とも同じ `requestCount` パラメータを使用
3. **商品ID**: 両方とも同じ `productId` パラメータを使用
4. **在庫準備**: 両方とも同じ処理を実行
5. **注文作成処理**: 両方とも同じサービスメソッドを呼び出し
6. **タイミング計測**: 両方とも同じ方法で計測
7. **完了待機**: 両方とも同じ方法で待機
8. **処理の順序**: 両方とも同じ順序で処理

### ⚠️ 意図的な違い（これが比較のポイント）

1. **スレッドプール**:
   - Virtual Thread: 無制限（必要に応じて作成）
   - Platform Thread: 制限あり（デフォルト: 10スレッド）

この違いは、Virtual Threadの優位性を実証するための**意図的な違い**です。

- Virtual Threadは、I/O待機中に他のタスクを実行できるため、大量の並行リクエストを効率的に処理できます
- Platform Threadは、スレッドプールのサイズに制限があるため、I/O待機中もスレッドがブロックされ、スループットが制限されます

## 結論

**条件は平等です**（スレッドプールの違いを除く）。

スレッドプールの違いは、Virtual Threadの優位性を実証するための**意図的な違い**であり、これがベンチマークの目的です。

他のすべての条件（I/O待機時間、リクエスト数、処理内容、計測方法など）は完全に平等です。
