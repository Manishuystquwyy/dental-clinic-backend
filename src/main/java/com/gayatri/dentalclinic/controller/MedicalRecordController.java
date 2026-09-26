package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.MedicalDocumentRequestDto;
import com.gayatri.dentalclinic.dto.request.PrescriptionRequestDto;
import com.gayatri.dentalclinic.dto.response.MedicalRecordResponseDto;
import com.gayatri.dentalclinic.service.MedicalRecordService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Medical Records", description = "Private prescriptions and patient documents")
public class MedicalRecordController {
    private final MedicalRecordService service;

    @GetMapping("/medical-records/mine")
    public ResponseEntity<List<MedicalRecordResponseDto>> mine() {
        return privateResponse(service.getMyRecords());
    }

    @GetMapping("/appointments/{appointmentId}/medical-records")
    public ResponseEntity<List<MedicalRecordResponseDto>> appointmentRecords(@PathVariable Long appointmentId) {
        return privateResponse(service.getAppointmentRecords(appointmentId));
    }

    @GetMapping("/medical-records/{id}")
    public ResponseEntity<MedicalRecordResponseDto> record(@PathVariable Long id) {
        return privateResponse(service.getRecord(id));
    }

    @PostMapping("/appointments/{appointmentId}/medical-records/prescription")
    public ResponseEntity<MedicalRecordResponseDto> prescribe(@PathVariable Long appointmentId,
                                                              @Valid @RequestBody PrescriptionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.prescribe(appointmentId, request));
    }

    @PostMapping(value = "/appointments/{appointmentId}/medical-records/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MedicalRecordResponseDto> upload(@PathVariable Long appointmentId,
            @Valid @ModelAttribute MedicalDocumentRequestDto request, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.upload(appointmentId, request, file));
    }

    @GetMapping("/medical-records/{id}/file")
    public ResponseEntity<byte[]> file(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean download) {
        var file = service.download(id);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("Content-Security-Policy", "sandbox")
                .contentType(MediaType.parseMediaType(file.contentType())).contentLength(file.content().length).body(file.content());
    }

    private <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
