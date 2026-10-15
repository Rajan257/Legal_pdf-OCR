package com.complaint.complaint_management_system.service;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

@Service
public class FuzzyMatchingServiceImpl
        implements FuzzyMatchingService {

    private final LevenshteinDistance
            levenshteinDistance =
            new LevenshteinDistance();

    @Override
    public boolean matches(
            String actual,
            String expected,
            double threshold) {

        if (actual == null ||
                expected == null ||
                actual.isBlank() ||
                expected.isBlank()) {

            return false;
        }

        String normalizedActual =
                normalize(actual);

        String normalizedExpected =
                normalize(expected);

        int distance =
                levenshteinDistance.apply(
                        normalizedActual,
                        normalizedExpected
                );

        int maxLength =
                Math.max(
                        normalizedActual.length(),
                        normalizedExpected.length()
                );

        if (maxLength == 0) {
            return false;
        }

        double similarity =
                1.0 -
                        ((double) distance
                                / maxLength);

        return similarity >= threshold;
    }

    @Override
    public boolean containsKeyword(
            String text,
            String expected,
            double threshold) {

        if (text == null ||
                expected == null ||
                text.isBlank() ||
                expected.isBlank()) {

            return false;
        }

        String normalizedText =
                normalize(text);

        String normalizedExpected =
                normalize(expected);

        // First check exact occurrence
        if (normalizedText.contains(
                normalizedExpected)) {

            return true;
        }

        // Then check individual words
        String[] words =
                normalizedText.split("\\s+");

        for (String word : words) {

            if (matches(
                    word,
                    normalizedExpected,
                    threshold)) {

                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9\\s]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }
}
