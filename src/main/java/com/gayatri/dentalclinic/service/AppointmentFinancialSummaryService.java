package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.config.AppointmentPolicy;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.dto.response.RefundResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Payment;
import com.gayatri.dentalclinic.entity.Refund;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import com.gayatri.dentalclinic.mapper.AppointmentMapper;
import com.gayatri.dentalclinic.repository.PaymentRepository;
import com.gayatri.dentalclinic.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Fetch financial details in two queries, independent of appointment list size. */
@Service
@RequiredArgsConstructor
public class AppointmentFinancialSummaryService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final BookingTime bookingTime;
    private final AppointmentPolicy appointmentPolicy;

    public AppointmentResponseDto toDto(Appointment appointment) {
        return toDtos(List.of(appointment)).getFirst();
    }

    public List<AppointmentResponseDto> toDtos(List<Appointment> appointments) {
        if (appointments.isEmpty()) return List.of();
        List<Long> ids = appointments.stream().map(Appointment::getId).filter(java.util.Objects::nonNull).toList();
        List<Payment> payments = ids.isEmpty() ? List.of() : paymentRepository.findByBillAppointmentIdIn(ids);
        Map<Long, List<Payment>> byAppointment = payments.stream().collect(Collectors.groupingBy(
                payment -> payment.getBill().getAppointment().getId()));
        List<Long> paymentIds = payments.stream().map(Payment::getId).toList();
        List<Refund> refunds = paymentIds.isEmpty() ? List.of() : refundRepository.findByPaymentIdIn(paymentIds);
        Map<Long, List<Refund>> refundsByAppointment = refunds.stream().collect(Collectors.groupingBy(
                refund -> refund.getPayment().getBill().getAppointment().getId()));
        Set<Long> queuedPaymentIds = refunds.stream().map(refund -> refund.getPayment().getId()).collect(Collectors.toSet());

        return appointments.stream().map(appointment -> {
            AppointmentResponseDto dto = AppointmentMapper.toDto(appointment);
            List<Payment> linked = byAppointment.getOrDefault(appointment.getId(), List.of());
            dto.setPaidAmount(linked.stream().filter(this::wasPaid).map(Payment::getAmount)
                    .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
            dto.setPaymentStatus(paymentStatus(linked));
            boolean cancellationOpen = appointment.getStatus() == AppointmentStatus.BOOKED
                    && appointment.getAppointmentDate() != null && appointment.getAppointmentTime() != null
                    && !bookingTime.now().isAfter(appointment.getAppointmentDate().atTime(appointment.getAppointmentTime())
                    .minusHours(appointmentPolicy.cancellationCutoffHours()));
            List<Payment> eligible = linked.stream().filter(payment ->
                    payment.getStatus() == PaymentStatus.SUCCESS && payment.getGatewayPaymentId() != null
                            && !payment.getGatewayPaymentId().isBlank() && payment.getAmount() != null
                            && payment.getAmount().signum() > 0 && !queuedPaymentIds.contains(payment.getId())).toList();
            dto.setRefundEligible(cancellationOpen && !eligible.isEmpty());
            dto.setRefundableAmount(cancellationOpen ? eligible.stream().map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO);
            dto.setRefunds(refundsByAppointment.getOrDefault(appointment.getId(), List.of()).stream()
                    .sorted(java.util.Comparator.comparing(refund -> refund.getPayment().getId()))
                    .map(refund -> RefundResponseDto.builder().paymentId(refund.getPayment().getId())
                            .status(refund.getStatus()).amount(refund.getAmount()).refundId(refund.getGatewayRefundId())
                            .requestedAt(refund.getRequestedAt()).completedAt(refund.getCompletedAt()).build()).toList());
            return dto;
        }).toList();
    }

    private boolean wasPaid(Payment payment) {
        return payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED;
    }

    private PaymentStatus paymentStatus(List<Payment> payments) {
        if (payments.isEmpty()) return null;
        if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.SUCCESS)) return PaymentStatus.SUCCESS;
        if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.REFUNDED)) return PaymentStatus.REFUNDED;
        if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.PENDING)) return PaymentStatus.PENDING;
        return PaymentStatus.FAILED;
    }
}
