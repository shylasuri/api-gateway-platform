package com.gateway.platform.repository;

import com.gateway.platform.entity.Quota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, String> {
    Optional<Quota> findByUserIdAndPeriodStart(String userId, LocalDate periodStart);
}
