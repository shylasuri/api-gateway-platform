package com.gateway.platform.repository;

import com.gateway.platform.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, String> {
    List<BillingRecord> findByUserIdOrderByPeriodStartDesc(String userId);
    Optional<BillingRecord> findByUserIdAndPeriodStart(String userId, LocalDate periodStart);
}
