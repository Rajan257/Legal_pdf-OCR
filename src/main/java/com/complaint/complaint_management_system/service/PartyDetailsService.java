package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import org.springframework.stereotype.Service;

@Service
public interface PartyDetailsService {
    ExtractedComplaintData extractPartyDetails(
            String text
    );
}
