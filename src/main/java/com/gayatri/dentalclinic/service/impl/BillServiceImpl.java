package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.response.BillResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.BillMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.BillRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public BillResponseDto createBill(BillRequestDto requestDto) {
        denyIfPatient();
        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        Bill bill = BillMapper.toEntity(requestDto, appointment);
        Bill savedBill = billRepository.save(bill);
        return BillMapper.toDto(savedBill);
    }

    @Override
    public List<BillResponseDto> getAllBills() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long patientId = SecurityUtils.getCurrentPatientId();
            return billRepository.findByAppointmentPatientId(patientId)
                    .stream()
                    .map(BillMapper::toDto)
                    .toList();
        }
        return billRepository.findAll()
                .stream()
                .map(BillMapper::toDto)
                .toList();
    }

    @Override
    public BillResponseDto getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + id));
        enforcePatientAccess(bill.getAppointment().getPatient().getId());
        return BillMapper.toDto(bill);
    }

    @Override
    public BillResponseDto updateBill(Long id, BillRequestDto requestDto) {
        denyIfPatient();
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + id));

        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        BillMapper.updateEntity(requestDto, bill, appointment);
        Bill savedBill = billRepository.save(bill);
        return BillMapper.toDto(savedBill);
    }

    @Override
    public void deleteBill(Long id) {
        denyIfPatient();
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + id));
        billRepository.delete(bill);
    }

    private void enforcePatientAccess(Long patientId) {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long currentPatientId = SecurityUtils.getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                throw new AccessDeniedException("You can only access your own bills.");
            }
        }
    }

    private void denyIfPatient() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            throw new AccessDeniedException("Patients are not allowed to modify bills.");
        }
    }
}
