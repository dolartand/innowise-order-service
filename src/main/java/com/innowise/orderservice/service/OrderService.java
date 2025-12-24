package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.order.OrderRequestDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.dto.order.OrderUpdateDto;
import com.innowise.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto requestDto, Long userId);

    OrderResponseDto getOrderById(Long id);

    Page<OrderResponseDto> getAllOrders(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            List<OrderStatus> statuses,
            Pageable pageable
    );

    Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable);

    OrderResponseDto updateOrder(Long id, OrderUpdateDto updateDto);

    OrderResponseDto updateOrderStatus(Long id, OrderStatus orderStatus);

    void deleteOrder(Long id);
}
