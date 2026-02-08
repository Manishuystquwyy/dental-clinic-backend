package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.response.BillResponseDto;

import java.util.List;

public interface BillService {

    BillResponseDto createBill(BillRequestDto requestDto);
    List<BillResponseDto> getAllBills();
    BillResponseDto getBillById(Long id);
    BillResponseDto updateBill(Long id, BillRequestDto requestDto);
    void deleteBill(Long id);
}
