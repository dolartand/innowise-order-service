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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
    @DisplayName("getAllOrders tests")
    class GetAllOrdersTests {

        @Test
        @DisplayName("should filter orders by multiple statuses")
        void shouldFilterOrdersByMultipleStatuses() {
            List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.PROCESSING);
            Pageable pageable = PageRequest.of(0, 10);

            Order pendingOrder = createOrder(1L, 1L);
            pendingOrder.setOrderStatus(OrderStatus.PENDING);
            Order processingOrder = createOrder(2L, 1L);
            processingOrder.setOrderStatus(OrderStatus.PROCESSING);

            List<Order> orders = List.of(pendingOrder, processingOrder);
            Page<Order> orderPage = new PageImpl<>(orders, pageable, 2);

            when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(orderPage);
            when(userServiceClient.getUserById(anyLong()))
                    .thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        return createOrderResponseDto(order.getId());
                    });

            Page<OrderResponseDto> result = orderService.getAllOrders(
                    null, null, statuses, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty page when no orders match filters")
        void shouldReturnEmptyPageWhenNoOrdersMatchFilters() {
            LocalDateTime dateFrom = LocalDateTime.now().plusYears(1);
            LocalDateTime dateTo = LocalDateTime.now().plusYears(2);
            Pageable pageable = PageRequest.of(0, 10);

            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(emptyPage);

            Page<OrderResponseDto> result = orderService.getAllOrders(
                    dateFrom, dateTo, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should handle pagination correctly")
        void shouldHandlePaginationCorrectly() {
            Pageable pageable = PageRequest.of(1, 2);

            Order order3 = createOrder(3L, 1L);
            Order order4 = createOrder(4L, 1L);
            List<Order> orders = List.of(order3, order4);
            Page<Order> orderPage = new PageImpl<>(orders, pageable, 10);

            when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(orderPage);
            when(userServiceClient.getUserById(anyLong()))
                    .thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        return createOrderResponseDto(order.getId());
                    });

            Page<OrderResponseDto> result = orderService.getAllOrders(
                    null, null, null, pageable
            );

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(5);
            assertThat(result.getNumber()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getOrdersByUserId tests")
    class GetOrdersByUserIdTests {
        @Test
        @DisplayName("should return all orders for user with multiple orders")
        void shouldReturnAllOrdersForUserWithMultipleOrders() {
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Order order1 = createOrder(1L, userId);
            Order order2 = createOrder(2L, userId);
            Order order3 = createOrder(3L, userId);
            List<Order> orders = List.of(order1, order2, order3);
            Page<Order> orderPage = new PageImpl<>(orders, pageable, 3);

            when(orderRepository.findByUserIdAndDeletedFalse(userId, pageable))
                    .thenReturn(orderPage);
            when(userServiceClient.getUserById(userId))
                    .thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenAnswer(invocation -> {
                        Order order = invocation.getArgument(0);
                        return createOrderResponseDto(order.getId());
                    });

            Page<OrderResponseDto> result = orderService.getOrdersByUserId(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            verify(orderRepository).findByUserIdAndDeletedFalse(userId, pageable);
            verify(userServiceClient, times(1)).getUserById(userId);
        }

        @Test
        @DisplayName("should return empty page when user has no orders")
        void shouldReturnEmptyPageWhenUserHasNoOrders() {
            Long userId = 999L;
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(orderRepository.findByUserIdAndDeletedFalse(userId, pageable))
                    .thenReturn(emptyPage);
            when(userServiceClient.getUserById(userId))
                    .thenReturn(createUserInfoDto(true));

            Page<OrderResponseDto> result = orderService.getOrdersByUserId(userId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateOrder tests")
    class UpdateOrderTests {

        @Test
        @DisplayName("should recalculate totalPrice when items are updated")
        void shouldRecalculateTotalPriceWhenItemsUpdated() {
            Long orderId = 1L;
            Order existingOrder = createOrder(orderId, 1L);
            existingOrder.setTotalPrice(new BigDecimal("1500.00"));

            Item newItem = createItem(2L);
            OrderItemRequestDto newItemDto = new OrderItemRequestDto(2L, 3);
            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .items(List.of(newItemDto))
                    .build();

            when(orderRepository.findByIdAndDeletedFalse(orderId))
                    .thenReturn(Optional.of(existingOrder));
            when(itemRepository.findById(2L))
                    .thenReturn(Optional.of(newItem));
            when(orderRepository.save(existingOrder))
                    .thenReturn(existingOrder);
            when(userServiceClient.getUserById(1L))
                    .thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenReturn(createOrderResponseDto(orderId));

            orderService.updateOrder(orderId, updateDto);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new BigDecimal("4500.00"));
        }

        @Test
        @DisplayName("should handle multiple items with correct total calculation")
        void shouldHandleMultipleItemsWithCorrectTotal() {
            Long orderId = 1L;
            Order existingOrder = createOrder(orderId, 1L);

            Item item1 = createItem(1L, "Laptop", new BigDecimal("1500.00"));
            Item item2 = createItem(2L, "Mouse", new BigDecimal("25.00"));
            Item item3 = createItem(3L, "Keyboard", new BigDecimal("75.00"));

            OrderItemRequestDto itemDto1 = new OrderItemRequestDto(1L, 2);
            OrderItemRequestDto itemDto2 = new OrderItemRequestDto(2L, 3);
            OrderItemRequestDto itemDto3 = new OrderItemRequestDto(3L, 1);

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .items(List.of(itemDto1, itemDto2, itemDto3))
                    .build();

            when(orderRepository.findByIdAndDeletedFalse(orderId))
                    .thenReturn(Optional.of(existingOrder));
            when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
            when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
            when(itemRepository.findById(3L)).thenReturn(Optional.of(item3));
            when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
            when(userServiceClient.getUserById(1L)).thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenReturn(createOrderResponseDto(orderId));

            orderService.updateOrder(orderId, updateDto);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new BigDecimal("3150.00"));
        }

        @Test
        @DisplayName("should set totalPrice to zero when all items removed")
        void shouldSetTotalPriceToZeroWhenAllItemsRemoved() {
            Long orderId = 1L;
            Order existingOrder = createOrder(orderId, 1L);
            existingOrder.setTotalPrice(new BigDecimal("1500.00"));

            OrderItem existingItem = OrderItem.builder()
                    .id(1L)
                    .order(existingOrder)
                    .item(createItem(1L, "Laptop", new BigDecimal("1500.00")))
                    .quantity(1)
                    .price(new BigDecimal("1500.00"))
                    .build();
            existingOrder.getItems().add(existingItem);

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .items(List.of())
                    .build();

            when(orderRepository.findByIdAndDeletedFalse(orderId))
                    .thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
            when(userServiceClient.getUserById(1L)).thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenReturn(createOrderResponseDto(orderId));

            orderService.updateOrder(orderId, updateDto);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(savedOrder.getItems()).isEmpty();
        }

        @Test
        @DisplayName("should update both status and items successfully")
        void shouldUpdateBothStatusAndItemsSuccessfully() {
            Long orderId = 1L;
            Order existingOrder = createOrder(orderId, 1L);
            existingOrder.setOrderStatus(OrderStatus.PENDING);

            Item newItem = createItem(1L, "Laptop", new BigDecimal("1500.00"));
            OrderItemRequestDto itemDto = new OrderItemRequestDto(1L, 1);

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(List.of(itemDto))
                    .build();

            when(orderRepository.findByIdAndDeletedFalse(orderId))
                    .thenReturn(Optional.of(existingOrder));
            when(itemRepository.findById(1L)).thenReturn(Optional.of(newItem));
            when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
            when(userServiceClient.getUserById(1L)).thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenReturn(createOrderResponseDto(orderId));

            orderService.updateOrder(orderId, updateDto);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(savedOrder.getItems()).hasSize(1);
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new BigDecimal("1500.00"));
        }

        @Test
        @DisplayName("should update only status when items not provided")
        void shouldUpdateOnlyStatusWhenItemsNotProvided() {
            Long orderId = 1L;
            Order existingOrder = createOrder(orderId, 1L);
            existingOrder.setOrderStatus(OrderStatus.PENDING);

            OrderItem existingItem = OrderItem.builder()
                    .id(1L)
                    .order(existingOrder)
                    .item(createItem(1L, "Laptop", new BigDecimal("1500.00")))
                    .quantity(1)
                    .price(new BigDecimal("1500.00"))
                    .build();
            existingOrder.getItems().add(existingItem);
            existingOrder.setTotalPrice(new BigDecimal("1500.00"));

            OrderUpdateDto updateDto = OrderUpdateDto.builder()
                    .status(OrderStatus.PROCESSING)
                    .items(null)
                    .build();

            when(orderRepository.findByIdAndDeletedFalse(orderId))
                    .thenReturn(Optional.of(existingOrder));
            when(orderRepository.save(existingOrder)).thenReturn(existingOrder);
            when(userServiceClient.getUserById(1L)).thenReturn(createUserInfoDto(true));
            when(orderMapper.orderToDto(any(Order.class), any(UserInfoDto.class)))
                    .thenReturn(createOrderResponseDto(orderId));

            orderService.updateOrder(orderId, updateDto);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());

            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(savedOrder.getItems()).hasSize(1);
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new BigDecimal("1500.00"));
        }
    }

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

    private Item createItem(Long id, String name, BigDecimal price) {
        return Item.builder()
                .id(id)
                .name(name)
                .price(price)
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