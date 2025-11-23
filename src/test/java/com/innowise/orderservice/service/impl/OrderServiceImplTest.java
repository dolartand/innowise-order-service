package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.client.dto.UserInfoDto;
import com.innowise.orderservice.dto.order.OrderItemRequestDto;
import com.innowise.orderservice.dto.order.OrderRequestDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.enums.OrderStatus;
import com.innowise.orderservice.exception.InvalidOrderStateException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService unit tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("createOrder tests")
    class CreateOrderTests {

        @Test
        @DisplayName("should successfully create order")
        void shouldCreateOrder_Success() {
            OrderRequestDto requestDto = createOrderRequestDto();
            UserInfoDto userInfo = createUserInfoDto(true);
            Item item = createItem(1L);
            Order savedOrder = createOrder(1L, 1L);
            OrderResponseDto expected = createOrderResponseDto(1L);

            when(userServiceClient.getUserById(requestDto.userId())).thenReturn(userInfo);
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(orderMapper.orderToDto(savedOrder, userInfo)).thenReturn(expected);

            OrderResponseDto result = orderService.createOrder(requestDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);

            verify(userServiceClient, times(1)).getUserById(requestDto.userId());
            verify(itemRepository, times(1)).findById(1L);
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("should throw InvalidOrderStateException when user is inactive")
        void shouldThrowInvalidOrderStateException_WhenUserInactive() {
            OrderRequestDto requestDto = createOrderRequestDto();
            UserInfoDto userInfo = createUserInfoDto(false);

            when(userServiceClient.getUserById(requestDto.userId())).thenReturn(userInfo);

            assertThatThrownBy(() -> orderService.createOrder(requestDto))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("inactive user");

            verify(userServiceClient, times(1)).getUserById(requestDto.userId());
            verify(itemRepository, never()).findById(any());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when item not found")
        void shouldThrowResourceNotFoundException_WhenItemNotFound() {
            OrderRequestDto requestDto = createOrderRequestDto();
            UserInfoDto userInfo = createUserInfoDto(true);

            when(userServiceClient.getUserById(requestDto.userId())).thenReturn(userInfo);
            when(itemRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Item not found");

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getOrderById tests")
    class GetOrderByIdTests {

        @Test
        @DisplayName("should successfully find order by id")
        void shouldFindOrderById_Success() {
            Long orderId = 1L;
            Order order = createOrder(orderId, 1L);
            UserInfoDto userInfo = createUserInfoDto(true);
            OrderResponseDto expected = createOrderResponseDto(orderId);

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
            when(userServiceClient.getUserById(order.getUserId())).thenReturn(userInfo);
            when(orderMapper.orderToDto(order, userInfo)).thenReturn(expected);

            OrderResponseDto result = orderService.getOrderById(orderId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(orderId);

            verify(orderRepository, times(1)).findByIdAndDeletedFalse(orderId);
            verify(userServiceClient, times(1)).getUserById(order.getUserId());
            verify(orderMapper, times(1)).orderToDto(order, userInfo);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when order not found")
        void shouldThrowResourceNotFoundException_WhenOrderNotFound() {
            Long orderId = 999L;

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(orderId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Order not found");

            verify(orderRepository, times(1)).findByIdAndDeletedFalse(orderId);
            verify(userServiceClient, never()).getUserById(any());
        }
    }

    @Nested
    @DisplayName("updateOrderStatus tests")
    class UpdateOrderStatusTests {

        @Test
        @DisplayName("should successfully update order status from PENDING to PROCESSING")
        void shouldUpdateStatus_FromPendingToProcessing() {
            Long orderId = 1L;
            Order order = createOrder(orderId, 1L);
            order.setOrderStatus(OrderStatus.PENDING);
            UserInfoDto userInfo = createUserInfoDto(true);
            OrderResponseDto expected = createOrderResponseDto(orderId);

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(userServiceClient.getUserById(order.getUserId())).thenReturn(userInfo);
            when(orderMapper.orderToDto(order, userInfo)).thenReturn(expected);

            OrderResponseDto result = orderService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

            assertThat(result).isNotNull();
            verify(orderRepository, times(1)).save(order);
        }

        @Test
        @DisplayName("should throw InvalidOrderStateException for invalid status transition")
        void shouldThrowInvalidOrderStateException_ForInvalidTransition() {
            Long orderId = 1L;
            Order order = createOrder(orderId, 1L);
            order.setOrderStatus(OrderStatus.DELIVERED);

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, OrderStatus.PENDING))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Invalid status transition");

            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteOrder tests")
    class DeleteOrderTests {

        @Test
        @DisplayName("should successfully soft delete order with PENDING status")
        void shouldSoftDeleteOrder_WithPendingStatus() {
            Long orderId = 1L;
            Order order = createOrder(orderId, 1L);
            order.setOrderStatus(OrderStatus.PENDING);

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.softDeleteById(orderId)).thenReturn(1);

            orderService.deleteOrder(orderId);

            verify(orderRepository, times(1)).softDeleteById(orderId);
        }

        @Test
        @DisplayName("should throw InvalidOrderStateException when deleting order with PROCESSING status")
        void shouldThrowInvalidOrderStateException_WhenDeletingProcessingOrder() {
            Long orderId = 1L;
            Order order = createOrder(orderId, 1L);
            order.setOrderStatus(OrderStatus.PROCESSING);

            when(orderRepository.findByIdAndDeletedFalse(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.deleteOrder(orderId))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Cannot delete order");

            verify(orderRepository, never()).softDeleteById(any());
        }
    }

    // Helper methods
    private OrderRequestDto createOrderRequestDto() {
        OrderItemRequestDto itemDto = new OrderItemRequestDto(1L, 2);
        return OrderRequestDto.builder()
                .userId(1L)
                .items(List.of(itemDto))
                .build();
    }

    private UserInfoDto createUserInfoDto(boolean active) {
        return UserInfoDto.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .active(active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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

    private Order createOrder(Long id, Long userId) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("3000.00"))
                .deleted(false)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private OrderResponseDto createOrderResponseDto(Long id) {
        return OrderResponseDto.builder()
                .id(id)
                .userId(1L)
                .user(createUserInfoDto(true))
                .status(OrderStatus.PENDING)
                .totalPrice(new BigDecimal("3000.00"))
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}