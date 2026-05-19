package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderResponseDto {

    @Schema(description = "Razorpay key id for frontend checkout")
    private String keyId;

    @Schema(description = "Razorpay order id")
    private String orderId;

    @Schema(description = "Amount in the smallest currency unit", example = "50000")
    private long amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Display name for checkout", example = "Gayatri Dental Clinic")
    private String name;

    @Schema(description = "Checkout description", example = "Consultation booking with Dr. Riya Kapoor")
    private String description;
}
