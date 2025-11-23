package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.BusinessException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService unit tests")
public class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemServiceImpl  itemService;

    @Nested
    @DisplayName("createItem tests")
    class CreateItemTests {

        @Test
        @DisplayName("should successfully create item")
        void shouldCreateItem_Success() {
            ItemRequestDto requestDto = createItemRequestDto();
            Item item = createItem(null);
            Item savedItem = createItem(1L);
            ItemResponseDto expected = createItemResponseDto(1L);

            when(itemRepository.existsByName(requestDto.name())).thenReturn(false);
            when(itemMapper.toEntity(requestDto)).thenReturn(item);
            when(itemRepository.save(item)).thenReturn(savedItem);
            when(itemMapper.toDto(savedItem)).thenReturn(expected);

            ItemResponseDto result = itemService.createItem(requestDto);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(expected);

            verify(itemRepository, times(1)).existsByName(requestDto.name());
            verify(itemMapper, times(1)).toEntity(requestDto);
            verify(itemRepository, times(1)).save(item);
            verify(itemMapper, times(1)).toDto(savedItem);
        }

        @Test
        @DisplayName("should throw BusinessException when item name already exists")
        void shouldThrowBusinessException_WhenItemNameExists() {
            ItemRequestDto requestDto = createItemRequestDto();

            when(itemRepository.existsByName(requestDto.name())).thenReturn(true);

            assertThatThrownBy(() -> itemService.createItem(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");

            verify(itemRepository, times(1)).existsByName(requestDto.name());
            verify(itemMapper, never()).toEntity(any());
            verify(itemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getItemById tests")
    class GetItemByIdTests {

        @Test
        @DisplayName("should successfully find item by id")
        void shouldFindItemById_Success() {
            Long itemId = 1L;
            Item item = createItem(itemId);
            ItemResponseDto expected = createItemResponseDto(itemId);

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
            when(itemMapper.toDto(item)).thenReturn(expected);

            ItemResponseDto result = itemService.getItemById(itemId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(itemId);

            verify(itemRepository, times(1)).findById(itemId);
            verify(itemMapper, times(1)).toDto(item);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when item not found")
        void shouldThrowResourceNotFoundException_WhenItemNotFound() {
            Long itemId = 999L;

            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(itemId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item");
        }
    }

    @Nested
    @DisplayName("getAllItems tests")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void shouldFindAllItems_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Item item1 = createItem(1L);
            Item item2 = createItem(2L);
            List<Item> items = Arrays.asList(item1, item2);
            Page<Item> itemsPage = new PageImpl<>(items, pageable, items.size());

            ItemResponseDto dto1 = createItemResponseDto(1L);
            ItemResponseDto dto2 = createItemResponseDto(2L);

            when(itemRepository.findAll(pageable)).thenReturn(itemsPage);
            when(itemMapper.toDto(item1)).thenReturn(dto1);
            when(itemMapper.toDto(item2)).thenReturn(dto2);

            Page<ItemResponseDto> result = itemService.getAllItems(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            verify(itemRepository, times(1)).findAll(pageable);
            verify(itemMapper, times(2)).toDto(any(Item.class));
        }
    }

    @Nested
    @DisplayName("searchItemsByName tests")
    class  SearchItemsByNameTests {

        @Test
        @DisplayName("should return list of items matching name")
        void shouldReturnMatchingItems() {
            String searchName = "Laptop";
            Item item1 = createItem(1L);
            Item item2 = createItem(2L);
            List<Item> items = List.of(item1, item2);

            ItemResponseDto dto1 = createItemResponseDto(1L);
            ItemResponseDto dto2 = createItemResponseDto(2L);

            when(itemRepository.findByNameContainingIgnoreCase(searchName)).thenReturn(items);
            when(itemMapper.toDtoList(items)).thenReturn(List.of(dto1, dto2));

            List<ItemResponseDto> result = itemService.searchItemsByName(searchName);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            verify(itemRepository, times(1)).findByNameContainingIgnoreCase(searchName);
            verify(itemMapper, times(1)).toDtoList(items);
        }
    }

    @Nested
    @DisplayName("updateItem tests")
    class UpdateItemTests {

        @Test
        @DisplayName("should successfully update item")
        void shouldUpdateItem_Success() {
            Long itemId = 1L;
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Updated Laptop Name")
                    .price(new BigDecimal("1600.00"))
                    .build();

            Item existingItem = createItem(itemId);
            Item updatedItem = createItem(itemId);
            updatedItem.setName("Updated Laptop Name");
            ItemResponseDto expected = createItemResponseDto(itemId);

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
            when(itemRepository.existsByName(requestDto.name())).thenReturn(false);
            when(itemRepository.save(existingItem)).thenReturn(updatedItem);
            when(itemMapper.toDto(updatedItem)).thenReturn(expected);

            ItemResponseDto result = itemService.updateItem(itemId, requestDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(itemId);

            verify(itemRepository, times(1)).findById(itemId);
            verify(itemMapper, times(1)).updateEntityFromDto(requestDto, existingItem);
            verify(itemRepository, times(1)).save(existingItem);
            verify(itemMapper, times(1)).toDto(updatedItem);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when item not found")
        void shouldThrowResourceNotFoundException_WhenItemNotFound() {
            Long itemId = 999L;
            ItemRequestDto requestDto = createItemRequestDto();

            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.updateItem(itemId, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item");

            verify(itemRepository, times(1)).findById(itemId);
            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when new name already exists")
        void shouldThrowBusinessException_WhenNewNameExists() {
            Long itemId = 1L;
            ItemRequestDto requestDto = ItemRequestDto.builder()
                    .name("Different Name")
                    .price(new BigDecimal("999.99"))
                    .build();
            Item existingItem = createItem(itemId);

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
            when(itemRepository.existsByName(requestDto.name())).thenReturn(true);

            assertThatThrownBy(() -> itemService.updateItem(itemId, requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");

            verify(itemRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteItem tests")
    class DeleteItemTests {

        @Test
        @DisplayName("should successfully delete item")
        void shouldDeleteItem_Success() {
            Long itemId = 1L;

            when(itemRepository.existsById(itemId)).thenReturn(true);

            itemService.deleteItem(itemId);

            verify(itemRepository, times(1)).existsById(itemId);
            verify(itemRepository, times(1)).deleteById(itemId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when item doesn't exist")
        void shouldThrowResourceNotFoundException_WhenItemDoesntExist() {
            Long itemId = 999L;

            when(itemRepository.existsById(itemId)).thenReturn(false);

            assertThatThrownBy(() -> itemService.deleteItem(itemId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item");

            verify(itemRepository, times(1)).existsById(itemId);
            verify(itemRepository, never()).deleteById(any());
        }
    }

    private ItemRequestDto createItemRequestDto() {
        return ItemRequestDto.builder()
                .name("Laptop")
                .price(new BigDecimal("1500.00"))
                .build();
    }

    private Item createItem(Long id) {
        return Item.builder()
                .id(id)
                .name("Laptop")
                .price(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ItemResponseDto createItemResponseDto(Long id) {
        return ItemResponseDto.builder()
                .id(id)
                .name("Laptop")
                .price(new BigDecimal("1500.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
