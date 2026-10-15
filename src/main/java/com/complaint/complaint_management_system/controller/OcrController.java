package com.complaint.complaint_management_system.controller;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import com.complaint.complaint_management_system.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.DELETE, RequestMethod.PUT})
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file was uploaded or the file is empty.");
        }

        try {
            System.out.println("Processing upload for file: " + file.getOriginalFilename() + " (size: " + file.getSize() + " bytes)");
            ExtractedComplaintData data =
                    ocrService.extractTextFromPdf(file);

            return ResponseEntity.ok(data);

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isBlank()) {
                errorMsg = e.getClass().getSimpleName();
            }
            return ResponseEntity
                    .badRequest()
                    .body("OCR Extraction Error: " + errorMsg);
        }
    }
}