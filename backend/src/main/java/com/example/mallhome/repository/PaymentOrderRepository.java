package com.example.mallhome.repository;

import com.example.mallhome.entity.PaymentOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long>, JpaSpecificationExecutor<PaymentOrder> {

    Optional<PaymentOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentOrder> findWithLockByOrderNo(String orderNo);
}
