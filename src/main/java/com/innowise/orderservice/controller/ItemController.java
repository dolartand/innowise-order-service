package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * Create new item (only ADMIN)
     * @param itemRequestDto item data
     * @return created item
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponseDto> createItem(@Valid @RequestBody ItemRequestDto itemRequestDto) {
        ItemResponseDto createdItem = itemService.createItem(itemRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdItem);
    }

    /**
     * Get item by id (anyone)
     * @param id item id
     * @return item data
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItemById(@PathVariable Long id) {
        ItemResponseDto item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * Get all items with pagination (anyone)
     * @param pageable pagination parameters
     * @return page of items
     */
    @GetMapping
    public ResponseEntity<Page<ItemResponseDto>> getAllItems(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ItemResponseDto> items = itemService.getAllItems(pageable);
        return ResponseEntity.ok(items);
    }

    /**
     * Search items by name (anyone)
     * @param name item name
     * @return list of matching items
     */
    @GetMapping("/search")
    public ResponseEntity<List<ItemResponseDto>> searchItems(
            @RequestParam String name
    ) {
        List<ItemResponseDto> items = itemService.searchItemsByName(name);
        return ResponseEntity.ok(items);
    }

    /**
     * Update item (only ADMIN)
     * @param id item id
     * @param itemRequestDto new item data
     * @return updated item data
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ItemResponseDto> updateItem(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody ItemRequestDto itemRequestDto
    ) {
        ItemResponseDto updatedItem = itemService.updateItem(id, itemRequestDto);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * Delete item (only ADMIN)
     * @param id item id
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable(name = "id") Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
