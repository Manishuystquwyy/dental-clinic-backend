package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.dto.response.PaymentResponseDto;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.entity.Payment;

public class PaymentMapper {

    public static Payment toEntity(PaymentRequestDto dto, Bill bill) {
        return Payment.builder()
                .bill(bill)
                .paymentMode(dto.getPaymentMode())
                .amount(dto.getAmount())
                .paymentDate(dto.getPaymentDate())
                .status(dto.getStatus())
                .build();
    }

    public static void updateEntity(PaymentRequestDto dto, Payment payment, Bill bill) {
        payment.setBill(bill);
        payment.setPaymentMode(dto.getPaymentMode());
        payment.setAmount(dto.getAmount());
        payment.setPaymentDate(dto.getPaymentDate());
        payment.setStatus(dto.getStatus());
    }

    public static PaymentResponseDto toDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .paymentMode(payment.getPaymentMode())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .build();
    }
}
