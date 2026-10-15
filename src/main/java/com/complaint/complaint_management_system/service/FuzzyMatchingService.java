package com.complaint.complaint_management_system.service;

public interface FuzzyMatchingService {

    boolean matches(
            String actual,
            String expected,
            double threshold
    );

    boolean containsKeyword(
            String text,
            String expected,
            double threshold
    );
}