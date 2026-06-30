package com.example.mallhome.repository;

import com.example.mallhome.entity.ProductOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOrderItemRepository extends JpaRepository<ProductOrderItem, Long> {

    List<ProductOrderItem> findByOrderId(Long orderId);
}
