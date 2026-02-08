package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.PaymentMode;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    @Schema(description = "Payment id", example = "1")
    private Long id;

    @Schema(description = "Bill id", example = "1")
    private Long billId;

    @Schema(description = "Payment mode", example = "UPI")
    private PaymentMode paymentMode;

    @Schema(description = "Payment amount", example = "2500.00")
    private BigDecimal amount;

    @Schema(description = "Payment date", example = "2026-02-10")
    private LocalDate paymentDate;

    @Schema(description = "Payment status", example = "SUCCESS")
    private PaymentStatus status;
}
