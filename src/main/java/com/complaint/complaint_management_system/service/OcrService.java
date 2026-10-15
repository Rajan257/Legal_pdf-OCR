package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    ExtractedComplaintData extractTextFromPdf(MultipartFile file) throws Exception;
}
