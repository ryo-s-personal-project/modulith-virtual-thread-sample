# パッケージ構成図（DDD/クリーンアーキテクチャ版）

## 1. パッケージ構造（ツリー形式）

```mermaid
graph TD
    A[com.example] --> B[api.rest]
    A --> C[config]
    A --> D[order]
    A --> E[inventory]
    A --> F[shipping]
    A --> G[notification]
    A --> H[ModulithVirtualThreadSampleApplication]
    
    B --> B1[OrderController]
    B --> B2[NotificationController]
    
    C --> C1[VirtualThreadConfig]
    
    D --> D1[domain]
    D --> D2[application]
    D --> D3[infrastructure]
    D --> D4[adapter]
    
    D1 --> D1A[Order]
    D2 --> D2A[OrderService]
    D2 --> D2B[OrderDto]
    D2 --> D2C[CreateOrderCommand]
    D3 --> D3A[OrderRepository]
    D4 --> D4A[OrderCreatedEvent]
    D4 --> D4B[OrderConfirmedEvent]
    D4 --> D4C[OrderCancelledEvent]
    D4 --> D4D[OrderEventListener]
    
    E --> E1[domain]
    E --> E2[application]
    E --> E3[infrastructure]
    E --> E4[adapter]
    
    E1 --> E1A[InventoryItem]
    E2 --> E2A[InventoryService]
    E2 --> E2B[InventoryDto]
    E2 --> E2C[CreateInventoryItemCommand]
    E3 --> E3A[InventoryRepository]
    E4 --> E4A[InventoryReservedEvent]
    E4 --> E4B[InventoryReservationFailedEvent]
    E4 --> E4C[InventoryEventListener]
    
    F --> F1[domain]
    F --> F2[application]
    F --> F3[infrastructure]
    F --> F4[adapter]
    
    F1 --> F1A[Shipment]
    F2 --> F2A[ShippingService]
    F2 --> F2B[ShipmentDto]
    F3 --> F3A[ShipmentRepository]
    F4 --> F4A[ShipmentCreatedEvent]
    F4 --> F4B[ShippingEventListener]
    
    G --> G1[domain]
    G --> G2[application]
    G --> G3[infrastructure]
    G --> G4[adapter]
    
    G1 --> G1A[Notification]
    G2 --> G2A[NotificationService]
    G2 --> G2B[NotificationDto]
    G3 --> G3A[NotificationRepository]
    G4 --> G4A[NotificationEventListener]
    
    style A fill:#e1f5ff
    style D fill:#fff4e1
    style E fill:#e8f5e9
    style F fill:#f3e5f5
    style G fill:#fce4ec
    style B fill:#fff9c4
    style C fill:#e0f2f1
    style D1 fill:#ffe0b2
    style D2 fill:#fff9c4
    style D3 fill:#c8e6c9
    style D4 fill:#f8bbd0
```

## 2. クリーンアーキテクチャの層構造

```mermaid
graph TB
    subgraph "Interface Layer (API)"
        API[api.rest<br/>OrderController<br/>NotificationController]
    end
    
    subgraph "Application Layer"
        APP_ORDER[order.application<br/>OrderService<br/>OrderDto<br/>CreateOrderCommand]
        APP_INV[inventory.application<br/>InventoryService<br/>InventoryDto<br/>CreateInventoryItemCommand]
        APP_SHIP[shipping.application<br/>ShippingService<br/>ShipmentDto]
        APP_NOTIF[notification.application<br/>NotificationService<br/>NotificationDto]
    end
    
    subgraph "Domain Layer"
        DOM_ORDER[order.domain<br/>Order<br/>- confirm<br/>- cancel<br/>- isPending]
        DOM_INV[inventory.domain<br/>InventoryItem<br/>- reserve<br/>- release<br/>- hasEnoughStock]
        DOM_SHIP[shipping.domain<br/>Shipment<br/>- ship<br/>- markAsDelivered]
        DOM_NOTIF[notification.domain<br/>Notification<br/>- markAsSent<br/>- markAsFailed]
    end
    
    subgraph "Infrastructure Layer"
        INF_ORDER[order.infrastructure<br/>OrderRepository]
        INF_INV[inventory.infrastructure<br/>InventoryRepository]
        INF_SHIP[shipping.infrastructure<br/>ShipmentRepository]
        INF_NOTIF[notification.infrastructure<br/>NotificationRepository]
    end
    
    subgraph "Adapter Layer (Events)"
        ADAPTER_ORDER[order.adapter<br/>Events<br/>OrderEventListener]
        ADAPTER_INV[inventory.adapter<br/>Events<br/>InventoryEventListener]
        ADAPTER_SHIP[shipping.adapter<br/>Events<br/>ShippingEventListener]
        ADAPTER_NOTIF[notification.adapter<br/>NotificationEventListener]
    end
    
    API --> APP_ORDER
    API --> APP_INV
    API --> APP_SHIP
    API --> APP_NOTIF
    
    APP_ORDER --> DOM_ORDER
    APP_ORDER --> INF_ORDER
    APP_INV --> DOM_INV
    APP_INV --> INF_INV
    APP_SHIP --> DOM_SHIP
    APP_SHIP --> INF_SHIP
    APP_NOTIF --> DOM_NOTIF
    APP_NOTIF --> INF_NOTIF
    
    ADAPTER_ORDER --> APP_ORDER
    ADAPTER_INV --> APP_INV
    ADAPTER_SHIP --> APP_SHIP
    ADAPTER_NOTIF --> APP_NOTIF
    
    style API fill:#fff9c4
    style APP_ORDER fill:#fff9c4
    style APP_INV fill:#fff9c4
    style APP_SHIP fill:#fff9c4
    style APP_NOTIF fill:#fff9c4
    style DOM_ORDER fill:#ffe0b2
    style DOM_INV fill:#ffe0b2
    style DOM_SHIP fill:#ffe0b2
    style DOM_NOTIF fill:#ffe0b2
    style INF_ORDER fill:#c8e6c9
    style INF_INV fill:#c8e6c9
    style INF_SHIP fill:#c8e6c9
    style INF_NOTIF fill:#c8e6c9
    style ADAPTER_ORDER fill:#f8bbd0
    style ADAPTER_INV fill:#f8bbd0
    style ADAPTER_SHIP fill:#f8bbd0
    style ADAPTER_NOTIF fill:#f8bbd0
```

## 3. モジュール境界と依存関係（DDD Bounded Context）

```mermaid
graph LR
    subgraph "Order Bounded Context"
        direction TB
        O_API[api.rest.OrderController]
        O_APP[application.OrderService]
        O_DOM[domain.Order]
        O_INF[infrastructure.OrderRepository]
        O_ADAPTER[adapter.OrderEventListener]
    end
    
    subgraph "Inventory Bounded Context"
        direction TB
        I_API[api.rest.OrderController]
        I_APP[application.InventoryService]
        I_DOM[domain.InventoryItem]
        I_INF[infrastructure.InventoryRepository]
        I_ADAPTER[adapter.InventoryEventListener]
    end
    
    subgraph "Shipping Bounded Context"
        direction TB
        S_API[api.rest.OrderController]
        S_APP[application.ShippingService]
        S_DOM[domain.Shipment]
        S_INF[infrastructure.ShipmentRepository]
        S_ADAPTER[adapter.ShippingEventListener]
    end
    
    subgraph "Notification Bounded Context"
        direction TB
        N_API[api.rest.NotificationController]
        N_APP[application.NotificationService]
        N_DOM[domain.Notification]
        N_INF[infrastructure.NotificationRepository]
        N_ADAPTER[adapter.NotificationEventListener]
    end
    
    O_API --> O_APP
    O_APP --> O_DOM
    O_APP --> O_INF
    O_ADAPTER --> O_APP
    
    I_API --> I_APP
    I_APP --> I_DOM
    I_APP --> I_INF
    I_ADAPTER --> I_APP
    
    S_API --> S_APP
    S_APP --> S_DOM
    S_APP --> S_INF
    S_ADAPTER --> S_APP
    
    N_API --> N_APP
    N_APP --> N_DOM
    N_APP --> N_INF
    N_ADAPTER --> N_APP
    
    O_ADAPTER -.->|Event| I_ADAPTER
    I_ADAPTER -.->|Event| O_ADAPTER
    O_ADAPTER -.->|Event| S_ADAPTER
    S_ADAPTER -.->|Event| N_ADAPTER
    O_ADAPTER -.->|Event| N_ADAPTER
    
    style O_DOM fill:#fff4e1,stroke:#ff9800,stroke-width:3px
    style I_DOM fill:#e8f5e9,stroke:#4caf50,stroke-width:3px
    style S_DOM fill:#f3e5f5,stroke:#9c27b0,stroke-width:3px
    style N_DOM fill:#fce4ec,stroke:#e91e63,stroke-width:3px
```

## 4. イベント駆動フロー（Virtual Thread）

```mermaid
sequenceDiagram
    participant API as OrderController
    participant OS as OrderService<br/>(Application)
    participant OE as OrderEventListener<br/>(Adapter)
    participant IS as InventoryService<br/>(Application)
    participant IE as InventoryEventListener<br/>(Adapter)
    participant SS as ShippingService<br/>(Application)
    participant SE as ShippingEventListener<br/>(Adapter)
    participant NS as NotificationService<br/>(Application)
    participant NE as NotificationEventListener<br/>(Adapter)
    
    Note over API,NE: Virtual Threadで非同期実行
    
    API->>OS: createOrder(CreateOrderCommand)
    OS->>OS: Order保存 (Domain)
    OS-->>IE: OrderCreatedEvent (async)
    OS-->>NE: OrderCreatedEvent (async)
    
    Note over IE: Virtual Thread-1
    IE->>IS: 在庫チェック
    IS->>IS: InventoryItem.reserve() (Domain)
    alt 在庫あり
        IE-->>OE: InventoryReservedEvent (async)
        OE->>OS: confirmOrder()
        OS->>OS: Order.confirm() (Domain)
        OS-->>SE: OrderConfirmedEvent (async)
        OS-->>NE: OrderConfirmedEvent (async)
        
        Note over SE: Virtual Thread-2
        SE->>SS: createShipment()
        SS->>SS: Shipment.ship() (Domain)
        SE-->>NE: ShipmentCreatedEvent (async)
        
        Note over NE: Virtual Thread-3
        NE->>NS: 通知送信
        NS->>NS: Notification.markAsSent() (Domain)
    else 在庫なし
        IE-->>OE: InventoryReservationFailedEvent (async)
        OE->>OS: cancelOrder()
        OS->>OS: Order.cancel() (Domain)
    end
```

## 5. テキスト形式のパッケージ構成（DDD/クリーンアーキテクチャ）

```
com.example
├── api                              # Interface Layer
│   └── rest                         # REST API層
│       ├── OrderController          # 注文関連API
│       └── NotificationController  # 通知関連API
│
├── config                           # 設定クラス
│   └── VirtualThreadConfig          # Virtual Thread設定
│
├── order                            # Order Bounded Context
│   ├── domain                       # Domain Layer
│   │   └── Order                    # エンティティ（ドメインロジック含む）
│   │       - confirm()
│   │       - cancel()
│   │       - isPending()
│   │
│   ├── application                  # Application Layer
│   │   ├── OrderService             # アプリケーションサービス
│   │   ├── OrderDto                 # データ転送オブジェクト
│   │   └── CreateOrderCommand       # コマンドオブジェクト
│   │
│   ├── infrastructure               # Infrastructure Layer
│   │   └── OrderRepository          # リポジトリ（JPA）
│   │
│   └── adapter                      # Adapter Layer
│       ├── OrderCreatedEvent        # イベント
│       ├── OrderConfirmedEvent      # イベント
│       ├── OrderCancelledEvent       # イベント
│       └── OrderEventListener       # イベントリスナー
│
├── inventory                        # Inventory Bounded Context
│   ├── domain
│   │   └── InventoryItem            # エンティティ（ドメインロジック含む）
│   │       - reserve()
│   │       - release()
│   │       - hasEnoughStock()
│   │
│   ├── application
│   │   ├── InventoryService
│   │   ├── InventoryDto
│   │   └── CreateInventoryItemCommand
│   │
│   ├── infrastructure
│   │   └── InventoryRepository
│   │
│   └── adapter
│       ├── InventoryReservedEvent
│       ├── InventoryReservationFailedEvent
│       └── InventoryEventListener
│
├── shipping                         # Shipping Bounded Context
│   ├── domain
│   │   └── Shipment                 # エンティティ（ドメインロジック含む）
│   │       - ship()
│   │       - markAsDelivered()
│   │
│   ├── application
│   │   ├── ShippingService
│   │   └── ShipmentDto
│   │
│   ├── infrastructure
│   │   └── ShipmentRepository
│   │
│   └── adapter
│       ├── ShipmentCreatedEvent
│       └── ShippingEventListener
│
├── notification                     # Notification Bounded Context
│   ├── domain
│   │   └── Notification              # エンティティ（ドメインロジック含む）
│   │       - markAsSent()
│   │       - markAsFailed()
│   │
│   ├── application
│   │   ├── NotificationService
│   │   └── NotificationDto
│   │
│   ├── infrastructure
│   │   └── NotificationRepository
│   │
│   └── adapter
│       └── NotificationEventListener
│
└── ModulithVirtualThreadSampleApplication # メインクラス
```

## 6. 依存関係の方向（クリーンアーキテクチャ）

```mermaid
graph TB
    subgraph "Outer Layers"
        API[Interface Layer<br/>REST Controllers]
        ADAPTER[Adapter Layer<br/>Event Listeners]
        INFRA[Infrastructure Layer<br/>Repositories]
    end
    
    subgraph "Inner Layers"
        APP[Application Layer<br/>Services, DTOs, Commands]
        DOMAIN[Domain Layer<br/>Entities with Business Logic]
    end
    
    API --> APP
    ADAPTER --> APP
    INFRA --> DOMAIN
    APP --> DOMAIN
    
    style DOMAIN fill:#ffe0b2,stroke:#ff9800,stroke-width:3px
    style APP fill:#fff9c4,stroke:#fbc02d,stroke-width:2px
    style API fill:#e3f2fd,stroke:#2196f3,stroke-width:1px
    style ADAPTER fill:#f3e5f5,stroke:#9c27b0,stroke-width:1px
    style INFRA fill:#e8f5e9,stroke:#4caf50,stroke-width:1px
```

## 7. アーキテクチャの特徴

### DDD（ドメイン駆動設計）の要素

1. **Bounded Context（境界付けられたコンテキスト）**
   - 各モジュール（order, inventory, shipping, notification）が独立したBounded Context
   - モジュール間はイベントで連携（Context Mapping）

2. **Domain Layer（ドメイン層）**
   - エンティティにビジネスロジックを集約
   - 例：`Order.confirm()`, `InventoryItem.reserve()`, `Shipment.ship()`

3. **Application Layer（アプリケーション層）**
   - ユースケースの実装
   - ドメインオブジェクトの協調
   - DTO/Commandによる入出力の抽象化

### クリーンアーキテクチャの原則

1. **依存関係の方向**
   - 外側の層は内側の層に依存
   - 内側の層は外側の層に依存しない
   - Domain Layerが最も内側（独立）

2. **層の分離**
   - **Domain**: ビジネスロジックの核心（フレームワーク非依存）
   - **Application**: ユースケースの実装
   - **Infrastructure**: 技術的な詳細（JPA、データベース）
   - **Adapter**: 外部システムとの接続（イベント、REST）

3. **関心の分離**
   - 各層が明確な責務を持つ
   - テスト容易性の向上

### Spring Modulithとの統合

- 各Bounded ContextがSpring Modulithのモジュールとして機能
- モジュール間のイベント連携は`@ApplicationModuleListener`で実現
- Virtual Threadによる非同期処理でスループット向上
