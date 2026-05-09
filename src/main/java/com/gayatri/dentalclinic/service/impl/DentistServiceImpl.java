package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.config.CacheNames;
import com.gayatri.dentalclinic.dto.request.DentistRequestDto;
import com.gayatri.dentalclinic.dto.response.DentistResponseDto;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.DentistMapper;
import com.gayatri.dentalclinic.repository.DentistRepository;
import com.gayatri.dentalclinic.service.DentistService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DentistServiceImpl implements DentistService {

    private final DentistRepository dentistRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DENTISTS, allEntries = true)
    })
    public DentistResponseDto createDentist(DentistRequestDto requestDto) {
        if (dentistRepository.existsByPhone(requestDto.getPhone())) {
            throw new BadRequestException("Phone number already exists");
        }
        if (requestDto.getEmail() != null &&
                dentistRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Dentist dentist = DentistMapper.toEntity(requestDto);
        Dentist savedDentist = dentistRepository.save(dentist);
        return DentistMapper.toDto(savedDentist);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.DENTISTS, sync = true)
    public List<DentistResponseDto> getAllDentists() {
        return dentistRepository.findAll()
                .stream()
                .map(DentistMapper::toDto)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.DENTIST_BY_ID, key = "#id", sync = true)
    public DentistResponseDto getDentistById(Long id) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + id));
        return DentistMapper.toDto(dentist);
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = CacheNames.DENTIST_BY_ID, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheNames.DENTISTS, allEntries = true)
    )
    public DentistResponseDto updateDentist(Long id, DentistRequestDto requestDto) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + id));

        if (dentistRepository.existsByPhoneAndIdNot(requestDto.getPhone(), id)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (requestDto.getEmail() != null &&
                dentistRepository.existsByEmailAndIdNot(requestDto.getEmail(), id)) {
            throw new BadRequestException("Email already exists");
        }

        DentistMapper.updateEntity(requestDto, dentist);
        Dentist savedDentist = dentistRepository.save(dentist);
        return DentistMapper.toDto(savedDentist);
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = CacheNames.DENTIST_BY_ID, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheNames.DENTISTS, allEntries = true)
    )
    public DentistResponseDto updateDentistPicture(Long id, String pictureUrl) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + id));
        dentist.setPictureUrl(pictureUrl);
        Dentist savedDentist = dentistRepository.save(dentist);
        return DentistMapper.toDto(savedDentist);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.DENTIST_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.DENTISTS, allEntries = true)
    })
    public void deleteDentist(Long id) {
        Dentist dentist = dentistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + id));
        dentistRepository.delete(dentist);
    }
}
