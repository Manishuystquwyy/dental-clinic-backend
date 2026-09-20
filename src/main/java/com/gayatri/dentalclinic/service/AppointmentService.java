package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.dto.response.AppointmentAvailabilityResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    AppointmentResponseDto createAppointment(AppointmentRequestDto requestDto);
    List<AppointmentResponseDto> getAllAppointments();
    AppointmentResponseDto getAppointmentById(Long id);
    AppointmentResponseDto updateAppointment(Long id, AppointmentRequestDto requestDto);
    void deleteAppointment(Long id);
    AppointmentAvailabilityResponseDto getAvailability(Long dentistId, LocalDate appointmentDate);
}
