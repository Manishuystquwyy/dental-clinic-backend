package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.dto.response.PaymentResponseDto;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.entity.Payment;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.PaymentMapper;
import com.gayatri.dentalclinic.repository.BillRepository;
import com.gayatri.dentalclinic.repository.PaymentRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        denyIfPatient();
        Bill bill = billRepository.findById(requestDto.getBillId())
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + requestDto.getBillId()));

        validateAmount(requestDto, bill);

        if (requestDto.getStatus() == null) {
            requestDto.setStatus(PaymentStatus.PENDING);
        }

        Payment payment = PaymentMapper.toEntity(requestDto, bill);
        Payment savedPayment = paymentRepository.save(payment);
        return PaymentMapper.toDto(savedPayment);
    }

    @Override
    public List<PaymentResponseDto> getAllPayments() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long patientId = SecurityUtils.getCurrentPatientId();
            return paymentRepository.findByBillAppointmentPatientId(patientId)
                    .stream()
                    .map(PaymentMapper::toDto)
                    .toList();
        }
        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDto)
                .toList();
    }

    @Override
    public PaymentResponseDto getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
        enforcePatientAccess(payment.getBill().getAppointment().getPatient().getId());
        return PaymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto updatePayment(Long id, PaymentRequestDto requestDto) {
        denyIfPatient();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));

        Bill bill = billRepository.findById(requestDto.getBillId())
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + requestDto.getBillId()));

        validateAmount(requestDto, bill);
        validateStatusTransition(payment.getStatus(), requestDto.getStatus());

        PaymentMapper.updateEntity(requestDto, payment, bill);
        Payment savedPayment = paymentRepository.save(payment);
        return PaymentMapper.toDto(savedPayment);
    }

    @Override
    public void deletePayment(Long id) {
        denyIfPatient();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
        paymentRepository.delete(payment);
    }

    private void validateAmount(PaymentRequestDto requestDto, Bill bill) {
        if (bill.getFinalAmount() != null && requestDto.getAmount() != null &&
                requestDto.getAmount().compareTo(bill.getFinalAmount()) > 0) {
            throw new BadRequestException("Payment amount cannot exceed bill final amount");
        }
    }

    private void validateStatusTransition(PaymentStatus currentStatus, PaymentStatus newStatus) {
        if (newStatus == null || currentStatus == null || newStatus == currentStatus) {
            return;
        }

        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != PaymentStatus.SUCCESS && newStatus != PaymentStatus.FAILED) {
                    throw new BadRequestException("Invalid status transition from PENDING to " + newStatus);
                }
            }
            case SUCCESS -> {
                if (newStatus != PaymentStatus.REFUNDED) {
                    throw new BadRequestException("Invalid status transition from SUCCESS to " + newStatus);
                }
            }
            case FAILED, REFUNDED -> {
                throw new BadRequestException("Cannot transition status from " + currentStatus);
            }
        }
    }

    private void enforcePatientAccess(Long patientId) {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long currentPatientId = SecurityUtils.getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                throw new AccessDeniedException("You can only access your own payments.");
            }
        }
    }

    private void denyIfPatient() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            throw new AccessDeniedException("Patients are not allowed to modify payments.");
        }
    }
}
