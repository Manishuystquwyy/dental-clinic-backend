package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.response.BillResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.BillMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.BillRepository;
import com.gayatri.dentalclinic.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final BillRepository billRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public BillResponseDto createBill(BillRequestDto requestDto) {
        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        Bill bill = BillMapper.toEntity(requestDto, appointment);
        Bill savedBill = billRepository.save(bill);
        return BillMapper.toDto(savedBill);
    }

    @Override
    public List<BillResponseDto> getAllBills() {
        return billRepository.findAll()
                .stream()
                .map(BillMapper::toDto)
                .toList();
    }

    @Override
    public BillResponseDto getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + id));
        return BillMapper.toDto(bill);
    }

    @Override
    public BillResponseDto updateBill(Long id, BillRequestDto requestDto) {
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
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bill not found with id: " + id));
        billRepository.delete(bill);
    }
}
