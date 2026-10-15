package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PartyDetailsServiceImpl implements PartyDetailsService {

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    @Override
    public ExtractedComplaintData extractPartyDetails(String text) {

        ExtractedComplaintData data =
                new ExtractedComplaintData();

        String district = extractDistrict(text);
        String crimeYear = extractCrimeYear(text);
        String crimeNo = extractCrimeNo(text);
        String policeStation = extractPoliceStation(text);
        String stNo=extractStNo(text);
        String stYear=extractStYear(text);


        data.setDistrict(district);
        data.setCrimeYear(crimeYear);
        data.setCrimeNo(crimeNo);
        data.setPoliceStation(policeStation);
        data.setStNo(stNo);
        data.setStYear(stYear);


        return data;
    }

    private String extractDistrict(String text) {

        Pattern pattern = Pattern.compile(
                "Police\\s+Station\\s+[^,\\n]+,\\s*District\\s+(.+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return "Not found";
        }

        String afterDistrict = matcher.group(1).trim();

        String[] words = afterDistrict.split("\\s+");

        StringBuilder district = new StringBuilder();

        for (int i = 0; i < Math.min(words.length, 4); i++) {

            if (words[i].matches(".*[,\\.].*")) {
                district.append(words[i].replaceAll("[,\\.]+$", ""));
                return district.toString().trim();
            }

            if (district.length() > 0) {
                district.append(" ");
            }

            district.append(words[i]);
        }

        return words.length > 0
                ? words[0].replaceAll("[^A-Za-z]+$", "")
                : "Not found";
    }

    private String extractCrimeYear(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Crime\\s*No\\.?\\s*(\\d+)\\s*/\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            String crimeYear = matcher.group(2);

            return crimeYear;
        }

        return null;
    }

    private String extractCrimeNo(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Crime\\s*No\\.?\\s*(\\d+)\\s*/\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            String crimeNo = matcher.group(1);

            return crimeNo;
        }

        return null;
    }
    private String extractPoliceStation(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "(?:P\\.\\s*S\\.?|Police\\s+Station)\\s*[:\\-–—]?\\s*([^,.;:()\\-–—\\n]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            String policeStation = matcher.group(1).trim();

            String[] words = policeStation.split("\\s+");

            if (words.length > 4) {
                policeStation = String.join(
                        " ",
                        Arrays.copyOf(words, 4)
                );
            }

            return policeStation.trim();
        }

        return null;
    }
    private String extractStNo(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Sessions?\\s+Trial\\s+No\\.?\\s*(\\d+)\\s*/\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            String stNo = matcher.group(1);

            return stNo;
        }

        return null;
    }
    private String extractStYear(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "Sessions?\\s+Trial\\s+No\\.?\\s*(\\d+)\\s*/\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {

            String stYear = matcher.group(2);

            return stYear;
        }

        return null;
    }
}