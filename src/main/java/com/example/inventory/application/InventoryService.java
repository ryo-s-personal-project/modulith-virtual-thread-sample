package com.example.inventory.application;

import com.example.inventory.domain.InventoryItem;
import com.example.inventory.infrastructure.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    
    private final InventoryRepository inventoryRepository;
    
    @Transactional
    public InventoryDto createInventoryItem(CreateInventoryItemCommand command) {
        InventoryItem item = InventoryItem.builder()
                .productId(command.productId())
                .availableQuantity(command.initialQuantity())
                .reservedQuantity(0)
                .build();
        
        item = inventoryRepository.save(item);
        log.info("在庫アイテムを作成しました: アイテムID={}", item.getId());
        return InventoryDto.from(item);
    }
    
    public InventoryDto getInventory(String productId) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        return InventoryDto.from(item);
    }
}
