package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.response.BillResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Bill;

public class BillMapper {

    public static Bill toEntity(BillRequestDto dto, Appointment appointment) {
        return Bill.builder()
                .appointment(appointment)
                .totalAmount(dto.getTotalAmount())
                .discount(dto.getDiscount())
                .finalAmount(dto.getFinalAmount())
                .billDate(dto.getBillDate())
                .build();
    }

    public static void updateEntity(BillRequestDto dto, Bill bill, Appointment appointment) {
        bill.setAppointment(appointment);
        bill.setTotalAmount(dto.getTotalAmount());
        bill.setDiscount(dto.getDiscount());
        bill.setFinalAmount(dto.getFinalAmount());
        bill.setBillDate(dto.getBillDate());
    }

    public static BillResponseDto toDto(Bill bill) {
        return BillResponseDto.builder()
                .id(bill.getId())
                .appointmentId(bill.getAppointment().getId())
                .totalAmount(bill.getTotalAmount())
                .discount(bill.getDiscount())
                .finalAmount(bill.getFinalAmount())
                .billDate(bill.getBillDate())
                .build();
    }
}
