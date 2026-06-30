package com.example.mallhome.repository;

import com.example.mallhome.entity.PaymentNotifyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentNotifyRecordRepository extends JpaRepository<PaymentNotifyRecord, Long>, JpaSpecificationExecutor<PaymentNotifyRecord> {

    boolean existsByNotifyKey(String notifyKey);

    Optional<PaymentNotifyRecord> findByNotifyKey(String notifyKey);
}
