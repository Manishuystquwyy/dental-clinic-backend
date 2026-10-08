package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.RefundStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class RefundResponseDto {
    private Long paymentId;
    private RefundStatus status;
    private BigDecimal amount;
    private String refundId;
    private Instant requestedAt;
    private Instant completedAt;
}
