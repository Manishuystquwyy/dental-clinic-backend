package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.RazorpayOrderRequestDto;
import com.gayatri.dentalclinic.dto.request.RazorpayVerificationRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.dto.response.RazorpayOrderResponseDto;

public interface RazorpayPaymentService {

    RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto requestDto);
    AppointmentResponseDto verifyPaymentAndConfirmAppointment(RazorpayVerificationRequestDto requestDto);
    void handleWebhook(String rawPayload, String signature);
}
