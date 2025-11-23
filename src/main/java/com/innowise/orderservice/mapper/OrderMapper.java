package com.innowise.orderservice.mapper;

import com.innowise.orderservice.client.dto.UserInfoDto;
import com.innowise.orderservice.dto.order.OrderItemResponseDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {ItemMapper.class}
)
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "createdAt", source = "order.createdAt")
    @Mapping(target = "updatedAt", source = "order.updatedAt")
    @Mapping(target = "status", source = "order.orderStatus")
    @Mapping(target = "user", source = "userInfoDto")
    @Mapping(target = "items", source = "order.items")
    OrderResponseDto orderToDto(Order order, UserInfoDto userInfoDto);

    @Mapping(target = "total", expression = "java(calculateSubtotal(orderItem))")
    OrderItemResponseDto toOrderItemDto(OrderItem orderItem);

    List<OrderItemResponseDto> toOrderItemDtos(List<OrderItem> orderItems);

    default BigDecimal calculateSubtotal(OrderItem orderItem) {
        if (orderItem == null || orderItem.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }
}
