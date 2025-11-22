package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.item.ItemRequestDto;
import com.innowise.orderservice.dto.item.ItemResponseDto;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ItemMapper {

    Item toEntity(ItemRequestDto dto);

    ItemResponseDto toDto(Item entity);

    List<ItemResponseDto> toDtoList(List<Item> entities);

    void updateEntityFromDto(ItemRequestDto dto, @MappingTarget Item entity);
}
