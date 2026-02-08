package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.dto.response.PaymentResponseDto;

import java.util.List;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto requestDto);
    List<PaymentResponseDto> getAllPayments();
    PaymentResponseDto getPaymentById(Long id);
    PaymentResponseDto updatePayment(Long id, PaymentRequestDto requestDto);
    void deletePayment(Long id);
}
