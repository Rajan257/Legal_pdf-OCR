package com.complaint.complaint_management_system.service;

import java.util.List;

public interface NlpKeywordExtractionService {
    List<String> extractKeywords(String subject, String description);
}
