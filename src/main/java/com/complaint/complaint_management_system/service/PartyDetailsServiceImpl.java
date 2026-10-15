package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PartyDetailsServiceImpl implements PartyDetailsService {

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    @Override
    public ExtractedComplaintData extractPartyDetails(String text) {
        ExtractedComplaintData data = new ExtractedComplaintData();

        if (text == null || text.isBlank()) {
            return data;
        }

        String district = extractDistrict(text);
        String crimeYear = extractCrimeYear(text);
        String crimeNo = extractCrimeNo(text);
        String policeStation = extractPoliceStation(text);
        String stNo = extractStNo(text);
        String stYear = extractStYear(text);

        data.setDistrict(district);
        data.setCrimeYear(crimeYear);
        data.setCrimeNo(crimeNo);
        data.setPoliceStation(policeStation);
        data.setStNo(stNo);
        data.setStYear(stYear);

        return data;
    }

    private String extractDistrict(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Primary: District associated with Police Station (e.g., "P.S.- Chanda, District- Sultanpur" or "Police Station Chanda, District Sultanpur")
        Pattern psDistrictPattern = Pattern.compile(
                "\\b(?:P\\.?\\s*S\\.?|Police\\s+Station)[^,\\n]+,\\s*Dist(?:rict|t\\.?)?\\s*[:\\-–—]?\\s*([A-Za-z]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher psMatcher = psDistrictPattern.matcher(text);
        if (psMatcher.find()) {
            String dist = psMatcher.group(1).trim();
            if (isValidDistrictName(dist)) {
                return normalizeDistrict(dist);
            }
        }

        // Secondary: Standalone District label in case context (e.g., "District- Sultanpur" or "District: Sultanpur" or "District Sultanpur")
        Pattern standalonePattern = Pattern.compile(
                "\\bDist(?:rict|t\\.?)?\\s*[:\\-–—]\\s*([A-Za-z]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher saMatcher = standalonePattern.matcher(text);
        while (saMatcher.find()) {
            String dist = saMatcher.group(1).trim();
            if (isValidDistrictName(dist)) {
                return normalizeDistrict(dist);
            }
        }

        // Fallback: check for "District [Name]" followed by comma or period or newline
        Pattern fallbackPattern = Pattern.compile(
                "\\bDistrict\\s+([A-Za-z]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher fbMatcher = fallbackPattern.matcher(text);
        while (fbMatcher.find()) {
            String dist = fbMatcher.group(1).trim();
            if (isValidDistrictName(dist)) {
                return normalizeDistrict(dist);
            }
        }

        return "";
    }

    private String normalizeDistrict(String dist) {
        if (dist == null || dist.isBlank()) return "";
        dist = dist.trim();
        if (fuzzyMatchingService != null) {
            String[] known = {"Sultanpur", "Lucknow", "Allahabad", "Prayagraj", "Varanasi", "Kanpur",
                              "Ayodhya", "Faizabad", "Barabanki", "Rae Bareli", "Pratapgarh", "Jaunpur", "Amethi", "Sitapur"};
            for (String k : known) {
                if (fuzzyMatchingService.matches(dist, k, 0.75)) {
                    return k;
                }
            }
        }
        return dist;
    }

    private boolean isValidDistrictName(String name) {
        if (name == null || name.isBlank() || name.length() < 3) {
            return false;
        }
        String lower = name.toLowerCase();
        return !lower.equals("court") &&
                !lower.equals("judge") &&
                !lower.equals("bench") &&
                !lower.equals("state") &&
                !lower.equals("police") &&
                !lower.equals("station") &&
                !lower.equals("high") &&
                !lower.equals("dated") &&
                !lower.equals("under");
    }

    private String extractCrimeYear(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "(?:Case\\s+)?Crime\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }

        Pattern firPattern = Pattern.compile(
                "FIR\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher firMatcher = firPattern.matcher(text);
        if (firMatcher.find()) {
            return firMatcher.group(2).trim();
        }

        return "";
    }

    private String extractCrimeNo(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "(?:Case\\s+)?Crime\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        Pattern firPattern = Pattern.compile(
                "FIR\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher firMatcher = firPattern.matcher(text);
        if (firMatcher.find()) {
            return firMatcher.group(1).trim();
        }

        return "";
    }

    private String extractPoliceStation(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "\\b(?:P\\.?\\s*S\\.?|Police\\s+Station)\\s*[:\\-–—]?\\s*([A-Za-z]+(?:\\s+[A-Za-z]+)?)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        while (matcher.find()) {
            String ps = matcher.group(1).trim();
            String lower = ps.toLowerCase();
            if (!lower.startsWith("district") && !lower.startsWith("distt") && !lower.startsWith("dated") && ps.length() >= 3) {
                counts.put(ps, counts.getOrDefault(ps, 0) + 1);
            }
        }

        if (counts.isEmpty()) {
            return "";
        }

        // Return the most frequent police station associated with the case
        String bestPs = "";
        int maxCount = -1;
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                bestPs = entry.getKey();
            }
        }

        return bestPs;
    }

    private String extractStNo(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "Sessions?\\s+Trials?\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        Pattern shortPattern = Pattern.compile(
                "\\bS\\.?\\s*T\\.?\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher shortMatcher = shortPattern.matcher(text);
        if (shortMatcher.find()) {
            return shortMatcher.group(1).trim();
        }

        return "";
    }

    private String extractStYear(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        Pattern pattern = Pattern.compile(
                "Sessions?\\s+Trials?\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }

        Pattern shortPattern = Pattern.compile(
                "\\bS\\.?\\s*T\\.?\\s*(?:No\\.?|Number)?\\s*[:\\-–—]?\\s*(\\d+)\\s*[/\\-]\\s*(\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher shortMatcher = shortPattern.matcher(text);
        if (shortMatcher.find()) {
            return shortMatcher.group(2).trim();
        }

        return "";
    }
}