package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.client.dto.UserInfoDto;
import com.innowise.orderservice.dto.order.OrderItemRequestDto;
import com.innowise.orderservice.dto.order.OrderRequestDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.dto.order.OrderUpdateDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.enums.OrderStatus;
import com.innowise.orderservice.exception.InvalidOrderStateException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.repository.specification.OrderSpecification;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        log.info("Creating order for user with id: {}", requestDto.userId());

        UserInfoDto userInfoDto = userServiceClient.getUserById(requestDto.userId());
        if (!userInfoDto.active()) {
            throw new InvalidOrderStateException("Cannot create order for inactive user");
        }

        Order order = Order.builder()
                .userId(requestDto.userId())
                .orderStatus(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        for (OrderItemRequestDto itemDto : requestDto.items()) {
            Item item = itemRepository.findById(itemDto.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("Item not found with id: %d", itemDto.itemId())));

            OrderItem orderItem = OrderItem.builder()
                    .item(item)
                    .quantity(itemDto.quantity())
                    .price(item.getPrice())
                    .build();

            addOrderItem(order, orderItem);
        }

        calculateTotalPrice(order);

        Order savedOrder = orderRepository.save(order);
        log.info("Created order for user with id: {}", requestDto.userId());

        return orderMapper.orderToDto(savedOrder, userInfoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Order not found with id: %d", id)));

        UserInfoDto userInfoDto = userServiceClient.getUserById(order.getUserId());

        return orderMapper.orderToDto(order, userInfoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getAllOrders(LocalDateTime dateFrom, LocalDateTime dateTo, List<OrderStatus> statuses, Pageable pageable) {
        log.debug("Fetching orders with filters - dateFrom: {}, dateTo: {}, statuses: {}",
                dateFrom, dateTo, statuses);

        Specification<Order> spec = OrderSpecification.isNotDeleted()
                .and(OrderSpecification.createdBetween(dateFrom, dateTo))
                .and(OrderSpecification.hasStatusIn(statuses));

        return orderRepository.findAll(spec, pageable)
                .map(order -> {
                    UserInfoDto userInfoDto = userServiceClient.getUserById(order.getUserId());
                    return orderMapper.orderToDto(order, userInfoDto);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> getOrdersByUserId(Long userId, Pageable pageable) {
        log.debug("Fetching orders for user: {}", userId);

        UserInfoDto userInfo = userServiceClient.getUserById(userId);

        return orderRepository.findByUserIdAndDeletedFalse(userId, pageable)
                .map(order -> orderMapper.orderToDto(order, userInfo));
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrder(Long id, OrderUpdateDto updateDto) {
        log.debug("Updating order with id: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Order not found with id: %d", id)));

        if (order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot update order in status: " + order.getOrderStatus()
            );
        }

        if (updateDto.status() != null) {
            validateStatusTransition(order.getOrderStatus(), updateDto.status());
            order.setOrderStatus(updateDto.status());
        }

        if (updateDto.items() != null && !updateDto.items().isEmpty()) {
            clearOrderItems(order);

            for (OrderItemRequestDto itemDto : updateDto.items()) {
                Item item = itemRepository.findById(itemDto.itemId())
                        .orElseThrow(() -> new ResourceNotFoundException(String.format(
                                "Item not found with id: %d", itemDto.itemId()
                        )));

                OrderItem orderItem = OrderItem.builder()
                        .item(item)
                        .quantity(itemDto.quantity())
                        .price(item.getPrice())
                        .build();

                addOrderItem(order, orderItem);
            }

            calculateTotalPrice(order);
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Updated order with id: {}", updatedOrder.getId());

        UserInfoDto userInfoDto = userServiceClient.getUserById(updatedOrder.getUserId());
        return orderMapper.orderToDto(updatedOrder, userInfoDto);
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long id, OrderStatus orderStatus) {
        log.info("Updating order status with id: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Order not found with id: %d", id)));

        validateStatusTransition(order.getOrderStatus(),orderStatus);
        order.setOrderStatus(orderStatus);

        Order updatedOrder = orderRepository.save(order);
        log.info("Updated order status with id: {}", updatedOrder.getId());

        UserInfoDto userInfoDto = userServiceClient.getUserById(updatedOrder.getUserId());
        return orderMapper.orderToDto(updatedOrder, userInfoDto);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Soft deleting order with id: {}", id);

        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Order not found with id: %d", id)));

        if (order.getOrderStatus() != OrderStatus.PENDING &&
                order.getOrderStatus() != OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(
                    "Cannot delete order in status: " + order.getOrderStatus()
            );
        }

        orderRepository.softDeleteById(id);
        log.info("Order soft deleted with id: {}", id);
    }

    private void addOrderItem(Order order, OrderItem orderItem) {
        order.getItems().add(orderItem);
        orderItem.setOrder(order);
    }

    private void removeOrderItem(Order order, OrderItem orderItem) {
        order.getItems().remove(orderItem);
        orderItem.setOrder(null);
    }

    private void clearOrderItems(Order order) {
        order.getItems().clear();
    }

    private void calculateTotalPrice(Order order) {
        BigDecimal totalPrice = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalPrice(totalPrice);
    }

    private void validateStatusTransition(OrderStatus orderStatus, OrderStatus newStatus) {
        boolean isValid = switch (orderStatus) {
            case PENDING -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new InvalidOrderStateException(
                    String.format("Invalid status transition from %s to %s", orderStatus, newStatus)
            );
        }
    }
}
