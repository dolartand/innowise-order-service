package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemService {

    ItemResponseDto createItem(ItemRequestDto dto);

    ItemResponseDto getItemById(Long id);

    Page<ItemResponseDto> getAllItems(Pageable pageable);

    List<ItemResponseDto> searchItemsByName(String name);

    ItemResponseDto updateItem(Long id, ItemRequestDto requestDto);

    void deleteItem(Long id);
}
