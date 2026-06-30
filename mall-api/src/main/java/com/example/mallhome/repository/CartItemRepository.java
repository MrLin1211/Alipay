package com.example.mallhome.repository;

import com.example.mallhome.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductIdAndSkuId(Long userId, Long productId, Long skuId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.userId = ?1")
    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByUserIdAndProductIdAndSkuId(Long userId, Long productId, Long skuId);
}
