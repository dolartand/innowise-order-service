package com.innowise.orderservice.repository;

import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);
}
