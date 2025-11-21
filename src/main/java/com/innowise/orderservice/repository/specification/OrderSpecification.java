package com.innowise.orderservice.repository.specification;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.enums.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public class OrderSpecification {

    private OrderSpecification() {
        throw new UnsupportedOperationException("Cannot create instance of utility class");
    }

    public static Specification<Order> isNotDeleted() {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("deleted"), false));
    }

    public static Specification<Order> hasUserId(Long userId) {
        return ((root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("userId"), userId);
        });
    }

    public static Specification<Order> hasStatus(OrderStatus orderStatus) {
        return ((root, query, criteriaBuilder) -> {
            if (orderStatus == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), orderStatus);
        });
    }

    public static Specification<Order> hasStatusIn(List<OrderStatus> orderStatuses) {
        return ((root, query, criteriaBuilder) -> {
            if (orderStatuses == null || orderStatuses.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("status").in(orderStatuses);
        });
    }

    public static Specification<Order> createdAfter(LocalDateTime from) {
        return ((root, query, criteriaBuilder) -> {
            if (from == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("created"), from);
        });
    }

    public static Specification<Order> createdBefore(LocalDateTime to) {
        return ((root, query, criteriaBuilder) -> {
            if (to == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("created"), to);
        });
    }

    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return ((root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return criteriaBuilder.conjunction();
            }

            if (from == null) {
                return criteriaBuilder.lessThanOrEqualTo(root.get("created"), to);
            }

            if (to == null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("created"), from);
            }
            return  criteriaBuilder.between(root.get("created"), from, to);
        });
    }
}
