package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;

public interface FieldExtractionService {

    ExtractedComplaintData extractFields(
            String fileName,
            String text
    );
}