package com.gateway.platform.repository;

import com.gateway.platform.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, String> {

    List<UsageRecord> findByUserIdOrderByRequestedAtDesc(String userId);

    @Query("select count(u) from UsageRecord u where u.user.id = :userId and u.requestedAt >= :from")
    long countByUserSince(@Param("userId") String userId, @Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.user.id = :userId and u.requestedAt between :from and :to")
    long countByUserBetween(@Param("userId") String userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(u) from UsageRecord u where u.requestedAt >= :from")
    long countAllSince(@Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.allowed = true and u.requestedAt >= :from")
    long countAllowedSince(@Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.allowed = false and u.requestedAt >= :from")
    long countRejectedSince(@Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.rejectionReason = :reason and u.requestedAt >= :from")
    long countByRejectionReasonSince(@Param("reason") UsageRecord.RejectionReason reason, @Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.user.id = :userId and u.allowed = true and u.requestedAt >= :from")
    long countAllowedByUserSince(@Param("userId") String userId, @Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.user.id = :userId and u.allowed = false and u.requestedAt >= :from")
    long countRejectedByUserSince(@Param("userId") String userId, @Param("from") Instant from);

    @Query("select count(u) from UsageRecord u where u.user.id = :userId and u.rejectionReason = :reason and u.requestedAt >= :from")
    long countByUserAndRejectionReasonSince(@Param("userId") String userId, @Param("reason") UsageRecord.RejectionReason reason, @Param("from") Instant from);

    @Query("select avg(u.responseTimeMs) from UsageRecord u where u.user.id = :userId and u.responseTimeMs is not null and u.requestedAt >= :from")
    Double averageLatencyByUserSince(@Param("userId") String userId, @Param("from") Instant from);

    @Query("select avg(u.responseTimeMs) from UsageRecord u where u.responseTimeMs is not null and u.requestedAt >= :from")
    Double averageLatencySince(@Param("from") Instant from);

    @Query("select u.api.id, u.api.name, count(u) from UsageRecord u where u.api is not null and u.requestedAt >= :from group by u.api.id, u.api.name order by count(u) desc")
    List<Object[]> topApisSince(@Param("from") Instant from);

    @Query("select u.user.id, u.user.email, count(u) from UsageRecord u where u.requestedAt >= :from group by u.user.id, u.user.email order by count(u) desc")
    List<Object[]> topConsumersSince(@Param("from") Instant from);

    @Query("select function('date', u.requestedAt), count(u) from UsageRecord u where u.requestedAt >= :from group by function('date', u.requestedAt) order by function('date', u.requestedAt)")
    List<Object[]> requestsPerDaySince(@Param("from") Instant from);

    List<UsageRecord> findTop50ByUserIdOrderByRequestedAtDesc(String userId);
}
