package com.example.mallhome.repository;

import com.example.mallhome.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long>, JpaSpecificationExecutor<ProductOrder> {

    Optional<ProductOrder> findByOrderNo(String orderNo);

    List<ProductOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
