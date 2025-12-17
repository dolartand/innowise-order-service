package com.innowise.orderservice.util;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;

import java.math.BigDecimal;
import java.util.List;

public final class OrderCalculationHelper {

    private OrderCalculationHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static BigDecimal calculateItemSubtotal(OrderItem orderItem) {
        if (orderItem == null || orderItem.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        return orderItem.getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));
    }

    public static BigDecimal calculateOrderTotal(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return order.getItems().stream()
                .map(OrderCalculationHelper::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal calculateTotalFromItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .map(OrderCalculationHelper::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}