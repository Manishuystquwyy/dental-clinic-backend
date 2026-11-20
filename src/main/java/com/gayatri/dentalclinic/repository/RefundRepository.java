package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Refund;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByPaymentId(Long paymentId);

    @EntityGraph(attributePaths = {"payment", "payment.bill", "payment.bill.appointment"})
    List<Refund> findByPaymentIdIn(Collection<Long> paymentIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.id = :id")
    Optional<Refund> findWithLockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Refund r where r.payment.gatewayPaymentId = :paymentId")
    Optional<Refund> findWithLockByGatewayPaymentId(@Param("paymentId") String paymentId);

    @Query("select r.id from Refund r where r.nextAttemptAt <= :now "
            + "and (r.leaseUntil is null or r.leaseUntil <= :now) order by r.nextAttemptAt, r.id")
    List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);
}
