package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayVerificationRequestDto {

    @Schema(description = "Dentist id", example = "1")
    @NotNull(message = "Dentist id is required")
    private Long dentistId;

    @Schema(description = "Appointment date", example = "2026-02-10")
    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    @Schema(description = "Appointment time", example = "10:30")
    @NotNull(message = "Appointment time is required")
    private LocalTime appointmentTime;

    @Schema(description = "Notes or remarks", example = "Initial consultation")
    private String remarks;

    @Schema(description = "Razorpay order id", example = "order_P1abcdEfgh2345")
    @NotBlank(message = "Razorpay order id is required")
    private String razorpayOrderId;

    @Schema(description = "Razorpay payment id", example = "pay_P1abcdEfgh2345")
    @NotBlank(message = "Razorpay payment id is required")
    private String razorpayPaymentId;

    @Schema(description = "Razorpay payment signature")
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
