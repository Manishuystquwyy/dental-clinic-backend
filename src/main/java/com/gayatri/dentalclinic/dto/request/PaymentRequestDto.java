package com.gayatri.dentalclinic.dto.request;

import com.gayatri.dentalclinic.enums.PaymentMode;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @Schema(description = "Bill id", example = "1")
    @NotNull(message = "Bill id is required")
    private Long billId;

    @Schema(description = "Payment mode", example = "UPI")
    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @Schema(description = "Payment amount", example = "2500.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must be 0 or greater")
    private BigDecimal amount;

    @Schema(description = "Payment date", example = "2026-02-10")
    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @Schema(description = "Payment status", example = "SUCCESS")
    private PaymentStatus status;
}
