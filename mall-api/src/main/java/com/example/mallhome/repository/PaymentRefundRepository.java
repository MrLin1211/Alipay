package com.example.mallhome.repository;

import com.example.mallhome.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long>, JpaSpecificationExecutor<PaymentRefund> {

    List<PaymentRefund> findByOrderNoOrderByCreatedAtDesc(String orderNo);

    List<PaymentRefund> findByOrderNoAndStatusIn(String orderNo, Collection<String> statuses);
}
