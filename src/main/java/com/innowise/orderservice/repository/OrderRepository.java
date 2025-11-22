package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdAndDeletedFalse(Long id);

    List<Order> findByUserIdAndDeletedFalse(Long userId);

    Page<Order> findByUserIdAndDeletedFalse(Long userId, Pageable pageable);

    Page<Order> findByDeletedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = :status WHERE o.id = :orderId AND o.deleted = false")
    int updateStatus(@Param("orderId") Long orderId, @Param("status") OrderStatus status);

    @Modifying
    @Query("UPDATE Order o SET o.deleted = true WHERE o.id = :orderId")
    int softDeleteById(@Param("orderId") Long orderId);

    long countByUserIdAndDeletedFalse(Long userId);
}
