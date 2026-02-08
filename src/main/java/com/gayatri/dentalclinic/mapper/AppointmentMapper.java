package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;

public class AppointmentMapper {

    public static Appointment toEntity(AppointmentRequestDto dto, Patient patient, Dentist dentist) {
        return Appointment.builder()
                .patient(patient)
                .dentist(dentist)
                .appointmentDate(dto.getAppointmentDate())
                .appointmentTime(dto.getAppointmentTime())
                .status(dto.getStatus())
                .remarks(dto.getRemarks())
                .build();
    }

    public static void updateEntity(AppointmentRequestDto dto, Appointment appointment, Patient patient, Dentist dentist) {
        appointment.setPatient(patient);
        appointment.setDentist(dentist);
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setStatus(dto.getStatus());
        appointment.setRemarks(dto.getRemarks());
    }

    public static AppointmentResponseDto toDto(Appointment appointment) {
        return AppointmentResponseDto.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .dentistId(appointment.getDentist().getId())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .remarks(appointment.getRemarks())
                .build();
    }
}
