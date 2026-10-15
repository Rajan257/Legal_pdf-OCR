package com.complaint.complaint_management_system.controller;

import com.complaint.complaint_management_system.service.NlpKeywordExtractionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nlp")
public class NlpTestController {

    private final NlpKeywordExtractionService nlpService;

    public NlpTestController(NlpKeywordExtractionService nlpService) {
        this.nlpService = nlpService;
    }

    @PostMapping("/test")
    public List<String> testNlp(
            @RequestParam String subject,
            @RequestParam String description) {
        List<String> keywords =
                nlpService.extractKeywords(subject, description);

        System.out.println("Extracted Keywords: " + keywords);

        return keywords;
    }
}