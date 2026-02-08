package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.TreatmentRequestDto;
import com.gayatri.dentalclinic.dto.response.TreatmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Treatment;

public class TreatmentMapper {

    public static Treatment toEntity(TreatmentRequestDto dto, Appointment appointment) {
        return Treatment.builder()
                .appointment(appointment)
                .treatmentName(dto.getTreatmentName())
                .description(dto.getDescription())
                .cost(dto.getCost())
                .build();
    }

    public static void updateEntity(TreatmentRequestDto dto, Treatment treatment, Appointment appointment) {
        treatment.setAppointment(appointment);
        treatment.setTreatmentName(dto.getTreatmentName());
        treatment.setDescription(dto.getDescription());
        treatment.setCost(dto.getCost());
    }

    public static TreatmentResponseDto toDto(Treatment treatment) {
        return TreatmentResponseDto.builder()
                .id(treatment.getId())
                .appointmentId(treatment.getAppointment().getId())
                .treatmentName(treatment.getTreatmentName())
                .description(treatment.getDescription())
                .cost(treatment.getCost())
                .build();
    }
}
