package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.exception.BusinessException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.ItemMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemResponseDto createItem(ItemRequestDto requestDto) {
        log.info("Creating item {}", requestDto);

        if (itemRepository.existsByName(requestDto.name())) {
            throw new BusinessException("Item with name: " + requestDto.name() + " already exists");
        }

        Item item = itemMapper.toEntity(requestDto);
        Item savedItem = itemRepository.save(item);

        log.info("Item created successfully, id: {}", savedItem.getId());
        return itemMapper.toDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getItemById(Long id) {
        log.debug("Fetching item by id: {}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Item with id: %d", id)));

        return itemMapper.toDto(item);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemResponseDto> getAllItems(Pageable pageable) {
        log.debug("Fetching all items");

        return itemRepository.findAll(pageable)
                .map(itemMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDto> searchItemsByName(String name) {
        log.debug("Searching items by name: {}", name);

        List<Item> items = itemRepository.findByNameContainingIgnoreCase(name);
        return itemMapper.toDtoList(items);
    }

    @Override
    @Transactional
    public ItemResponseDto updateItem(Long id, ItemRequestDto requestDto) {
        log.info("Updating item with id: {}", id);

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Item with id: %d", id)));

        if (!item.getName().equals(requestDto.name()) && itemRepository.existsByName(requestDto.name())) {
            throw new BusinessException("Item with name: " + requestDto.name() + " already exists");
        }

        itemMapper.updateEntityFromDto(requestDto, item);
        Item updatedItem = itemRepository.save(item);

        log.info("Item updated successfully, id: {}", updatedItem.getId());
        return itemMapper.toDto(updatedItem);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        log.info("Deleting item with id: {}", id);

        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException(String.format("Item with id: %d", id));
        }

        itemRepository.deleteById(id);
        log.info("Item deleted successfully, id: {}", id);
    }
}
