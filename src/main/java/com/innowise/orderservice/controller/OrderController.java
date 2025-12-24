package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.order.OrderRequestDto;
import com.innowise.orderservice.dto.order.OrderResponseDto;
import com.innowise.orderservice.dto.order.OrderUpdateDto;
import com.innowise.orderservice.enums.OrderStatus;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create new order (only authenticated)
     * @param orderRequestDto order data with items
     * @return created order with user info
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto orderRequestDto,
            @AuthenticationPrincipal Long userId) {
        OrderResponseDto createdOrder = orderService.createOrder(orderRequestDto, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdOrder);
    }

    /**
     * Get order by id (only ADMIN or owner of order)
     * @param id order id
     * @return order with user info
     */
    @GetMapping("/{id}")
    @PostAuthorize("hasRole('ADMIN') or returnObject.body.userId == authentication.principal")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable(name = "id") Long id){
        OrderResponseDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * Get all orders with optional filters and pagination
     * @param dateFrom filter by creation date from
     * @param dateTo filter by creation date to
     * @param statuses filter by order statuses
     * @param pageable pagination parameters
     * @return page of orders with user info
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime dateTo,

            @RequestParam(required = false)
            List<OrderStatus> statuses,

            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<OrderResponseDto> orders = orderService.getAllOrders(
                dateFrom, dateTo, statuses, pageable
        );
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by user id (only ADMIN or owner of orders)
     * @param userId user id
     * @param pageable pagination parameters
     * @return page of user`s orders with user info
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<Page<OrderResponseDto>> getOrdersByUserId(
            @PathVariable(name = "userId") Long userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<OrderResponseDto> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Update order
     * @param id order id
     * @param order update data
     * @return update order with user info
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody OrderUpdateDto order
    ) {
        OrderResponseDto updatedOrder = orderService.updateOrder(id, order);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Update only order status
     * @param id order id
     * @param status new status
     * @return updated order with user info
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable(name = "id") Long id,
            @RequestParam OrderStatus status
    ) {
        OrderResponseDto updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * Soft delete order by id
     * @param id order id
     * @return 204 NO CONTENT
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable(name = "id") Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
