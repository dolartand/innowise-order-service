package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);

    Optional<Item> findByIdAndDeletedFalse(Long id);

    Page<Item> findByDeletedFalse(Pageable pageable);

    Page<Item> findByNameContainingIgnoreCaseAndDeletedFalse(String name, Pageable pageable);

    @Modifying
    @Query("UPDATE Item i SET i.deleted = true WHERE i.id = :itemId")
    int softDeleteById(@Param("itemId") Long itemId);
}
