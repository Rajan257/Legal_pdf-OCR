package com.complaint.complaint_management_system.service;

import com.complaint.complaint_management_system.dto.ExtractedComplaintData;
import com.complaint.complaint_management_system.model.Draft_party;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FieldExtractionServiceImpl implements FieldExtractionService {

    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;

    @Autowired
    private PartyDetailsService partyDetailsService;

    @Override
    public ExtractedComplaintData extractFields(String fileName, String text) {

        ExtractedComplaintData data = new ExtractedComplaintData();

        data.setOcrText(text);
        data.setEst_code(extractEstCode(fileName));
        data.setNotice_no(extractNoticeNo(fileName));

        String prayer = extractPrayer(text);

        data.setPrayer(prayer);

        data.setParties(extractParties(text));

        ExtractedComplaintData partyDetails =
                partyDetailsService.extractPartyDetails(prayer);

        data.setDistrict(partyDetails.getDistrict());

        data.setCrimeYear(partyDetails.getCrimeYear());

        data.setCrimeNo(partyDetails.getCrimeNo());

        data.setPoliceStation(partyDetails.getPoliceStation());

        data.setStNo(partyDetails.getStNo());

        data.setStYear(partyDetails.getStYear());

        return data;
    }

    private String extractEstCode(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return "Not found";
        }

        char firstChar = Character.toUpperCase(fileName.charAt(0));

        if (firstChar == 'L') {
            return "UPHCO2";
        }

        if (firstChar == 'A') {
            return "UPHC01";
        }

        return "Not found";
    }

    private String extractNoticeNo(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return "Not found";
        }

        Matcher matcher = Pattern.compile("\\d{8,}")
                        .matcher(fileName);

        if (matcher.find()) {
            return matcher.group();
        }

        return "Not found";
    }

    private String extractPrayer(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        String[] pages =
                text.split("(?=--- PAGE \\d+ ---)");

        int prayerPage = -1;
        int prayerStart = -1;

        for (int pageIndex = 0;
             pageIndex < pages.length;
             pageIndex++) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            int start =
                    findPrayerStart(lines);

            if (start != -1) {
                prayerPage = pageIndex;
                prayerStart = start;
                break;
            }
        }

        if (prayerStart == -1) {
            return "";
        }

        StringBuilder prayer =
                new StringBuilder();

        for (int pageIndex = prayerPage;
             pageIndex < pages.length;
             pageIndex++) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            int start =
                    pageIndex == prayerPage
                            ? prayerStart
                            : 0;

            for (int i = start;
                 i < lines.length;
                 i++) {

                String line =
                        cleanLine(lines[i]);


                if (findPrayerFooter(lines, i)) {

                    return removeTrailingPlaceBeforeFooter(
                            normalizePrayerHeading(
                                    prayer.toString()
                            )
                    );
                }

                if (!line.isBlank()) {

                    prayer.append(line)
                            .append("\n");
                }
            }
        }

        return removeTrailingPlaceBeforeFooter(
                normalizePrayerHeading(
                        prayer.toString()
                )
        );
    }



    private int findPrayerStart(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = cleanLine(lines[i]);

            if (line.isBlank()) {
                continue;
            }

            String normalized = line.toLowerCase()
                    .replaceAll("[^a-z0-9 ]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            boolean hasPrayerHeading = normalized.equals("prayer")
                    || normalized.equals("prayers");

            boolean hasApplication = fuzzyMatchingService
                    .containsKeyword(normalized, "application", 0.80);

            boolean hasInterim = fuzzyMatchingService
                    .containsKeyword(normalized, "interim", 0.80);

            boolean hasRelief = fuzzyMatchingService
                    .containsKeyword(normalized, "relief", 0.80);

            System.out.println("Line index: " + i);
            System.out.println("Line: " + line);
            System.out.println("hasPrayerHeading: " + hasPrayerHeading);
            System.out.println("hasApplication: " + hasApplication);
            System.out.println("hasInterim: " + hasInterim);
            System.out.println("hasRelief: " + hasRelief);

            if (hasPrayerHeading || (hasApplication && hasInterim && hasRelief)) {
                System.out.println("PRAYER HEADING DETECTED AT INDEX: " + i);
                System.out.println("DETECTED LINE: " + line);
                System.out.println("EXTRACTION STARTS AT INDEX: " + (i + 1));

                return i + 1;
            }
        }

        System.out.println("NO PRAYER HEADING FOUND");
        return -1;
    }




    private boolean findPrayerFooter(
            String[] lines,
            int currentIndex) {

        String line = cleanLine(lines[currentIndex]);

        // Primary rule: existing DATE/ DATED -> ADVOCATE -> footer logic
        if (line.matches("(?i).*\\b(?:DATE|DATED)\\b.*")) {
            if (findAdvocateAfterDated(lines, currentIndex) != -1) {
                return true;
            }
        }

        // Fallback rule: stop immediately when a footer identifier is found
        return containsCounselFor(line)
                || containsAor(line)
                || containsMobile(line)
                || containsRegistration(line);
    }

    private int findAdvocateAfterDated(
            String[] lines,
            int datedIndex) {

        int end =
                Math.min(
                        lines.length - 1,
                        datedIndex + 6
                );

        for (int i = datedIndex;
             i <= end;
             i++) {

            String line =
                    cleanLine(lines[i]);

            if (!containsAdvocate(line)) {
                continue;
            }

            if (line.matches(
                    "(?i).*\\b(?:DATE|DATED)\\b.*\\bADVOCATE\\b.*"
            )) {

                return i;
            }

            if (findFooterIdentifierAfterAdvocate(
                    lines,
                    i
            ) != -1) {

                return i;
            }
        }

        return -1;
    }

    private int findFooterIdentifierAfterAdvocate(
            String[] lines,
            int advocateIndex) {

        int end =
                Math.min(
                        lines.length - 1,
                        advocateIndex + 10
                );

        for (int i = advocateIndex;
             i <= end;
             i++) {

            String line =
                    cleanLine(lines[i]);

            if (containsCounselFor(line)) {
                return i;
            }

            if (containsAor(line)
                    || containsMobile(line)
                    || containsRegistration(line)) {

                return i;
            }
        }

        return -1;
    }

    private boolean containsAdvocate(String line) {

        return fuzzyMatchingService.containsKeyword(
                line,
                "advocate",
                0.80
        );
    }

    private boolean containsCounselFor(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        return fuzzyMatchingService.containsKeyword(
                line,
                "counsel for",
                0.80
        );
    }

    private boolean containsAor(String line) {

        return fuzzyMatchingService.containsKeyword(
                line,
                "AOR",
                0.80
        );
    }

    private boolean containsMobile(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        return fuzzyMatchingService.containsKeyword(line, "mobile", 0.80)
                || fuzzyMatchingService.containsKeyword(line, "mob", 0.80)
                || fuzzyMatchingService.containsKeyword(line, "phone", 0.80);

    }

    private boolean containsRegistration(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        return fuzzyMatchingService.containsKeyword(line, "registration", 0.80)
                || fuzzyMatchingService.containsKeyword(line, "regn", 0.80)
                || fuzzyMatchingService.containsKeyword(line, "enrollment", 0.80)
                || fuzzyMatchingService.containsKeyword(line, "enrolment", 0.80);
    }

    private String removeTrailingPlaceBeforeFooter(
            String prayer) {

        if (prayer == null || prayer.isBlank()) {
            return prayer;
        }

        String[] lines =
                prayer.split("\\R");

        List<String> result =
                new ArrayList<>();

        for (String line : lines) {

            String cleaned =
                    cleanLine(line);

            if (cleaned.equalsIgnoreCase("Lucknow")
                    || cleaned.equalsIgnoreCase("Allahabad")) {

                continue;
            }

            result.add(cleaned);
        }

        return String.join(
                "\n",
                result
        ).trim();
    }

    private String normalizePrayerHeading(
            String prayer) {

        if (prayer == null) {
            return "";
        }

        return prayer
                .replaceAll(
                        "(?i)^\\s*prayer[s]?\\s*:?\\s*",
                        ""
                )
                .trim();
    }

    private List<Draft_party> extractParties(
            String text) {

        List<Draft_party> bestParties =
                new ArrayList<>();

        if (text == null || text.isBlank()) {
            return bestParties;
        }

        String[] pages =
                text.split("(?=--- PAGE \\d+ ---)");

        for (int petitionerPage = 0;
             petitionerPage < pages.length;
             petitionerPage++) {

            String[] lines =
                    pages[petitionerPage].split("\\R");

            for (int petitionerLine = 0;
                 petitionerLine < lines.length;
                 petitionerLine++) {

                String line =
                        cleanLine(lines[petitionerLine]);

                if (!isPetitionerLine(line)) {
                    continue;
                }

                int[] petitionerStart =
                        searchPetitionerStart(
                                pages,
                                petitionerPage,
                                petitionerLine
                        );

                if (petitionerStart == null) {
                    continue;
                }

                int[] versusPosition =
                        findVersusAcrossPages(
                                pages,
                                petitionerPage,
                                petitionerLine
                        );

                if (versusPosition == null) {
                    continue;
                }

                List<Draft_party> petitionerParties =
                        parsePetitionerBlock(
                                pages,
                                petitionerStart,
                                petitionerPage,
                                petitionerLine
                        );

                List<Draft_party> respondentParties =
                        parseRespondentBlock(
                                pages,
                                versusPosition
                        );

                List<Draft_party> candidate =
                        new ArrayList<>();

                candidate.addAll(petitionerParties);
                candidate.addAll(respondentParties);

                if (candidate.size() >= bestParties.size()) {
                    bestParties = candidate;
                }
            }
        }

        return bestParties;
    }

    private int[] findHighCourtHeading(
            String[] pages,
            int versusPageIndex,
            int versusLineIndex) {

        for (int pageIndex =
             versusPageIndex;
             pageIndex >= 0;
             pageIndex--) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            int start =
                    pageIndex == versusPageIndex
                            ? versusLineIndex - 1
                            : lines.length - 1;

            for (int i = start;
                 i >= 0;
                 i--) {

                String line =
                        cleanLine(lines[i])
                                .toLowerCase();

                if (line.contains("high court")
                        || line.contains("judicature")) {

                    return new int[]{
                            pageIndex,
                            i
                    };
                }
            }
        }

        return null;
    }


    private int[] searchPetitionerStart(
            String[] pages,
            int petitionerPageIndex,
            int petitionerLineIndex) {

        int[] headingPosition =
                findHighCourtHeading(
                        pages,
                        petitionerPageIndex,
                        petitionerLineIndex
                );

        if (headingPosition == null) {
            return null;
        }

        int pageIndex = headingPosition[0];
        int lineIndex = headingPosition[1] + 1;

        int[] firstYearPosition = null;
        int[] bracketPosition = null;

        while (pageIndex <= petitionerPageIndex) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            while (lineIndex < lines.length) {

                if (pageIndex == petitionerPageIndex
                        && lineIndex >= petitionerLineIndex) {
                    break;
                }

                String line =
                        cleanLine(lines[lineIndex]);

                if (line.isBlank()) {
                    lineIndex++;
                    continue;
                }

                if (firstYearPosition == null) {

                    if (containsCaseYear(line)) {
                        firstYearPosition =
                                new int[]{
                                        pageIndex,
                                        lineIndex
                                };
                    }

                    lineIndex++;
                    continue;
                }

                if (isBracketLine(line)) {
                    bracketPosition =
                            new int[]{
                                    pageIndex,
                                    lineIndex
                            };
                    break;
                }

                lineIndex++;
            }

            if (bracketPosition != null
                    || (pageIndex == petitionerPageIndex
                    && lineIndex >= petitionerLineIndex)) {
                break;
            }

            pageIndex++;
            lineIndex = 0;
        }

        int[] startPosition =
                bracketPosition != null
                        ? bracketPosition
                        : firstYearPosition;

        if (startPosition == null) {
            return null;
        }

        pageIndex = startPosition[0];
        lineIndex = startPosition[1] + 1;

        while (pageIndex <= petitionerPageIndex) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            while (lineIndex < lines.length) {

                if (pageIndex == petitionerPageIndex
                        && lineIndex >= petitionerLineIndex) {
                    return null;
                }

                String line =
                        cleanLine(lines[lineIndex]);

                if (!line.isBlank()) {
                    return new int[]{
                            pageIndex,
                            lineIndex
                    };
                }

                lineIndex++;
            }

            pageIndex++;
            lineIndex = 0;
        }

        return null;
    }


    private boolean isPetitionerLine(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String cleaned = line
                .replaceAll("[^A-Za-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        String[] roles = {
                "petitioner",
                "applicant",
                "revisionist",
                "appellant",
                "detenue"

        };

        for (String role : roles) {

            if (cleaned.endsWith(role)) {
                return true;
            }

            int start = Math.max(0, cleaned.length() - role.length() - 20);

            String ending = cleaned.substring(start);

            if (fuzzyMatchingService.containsKeyword(
                    ending,
                    role,
                    0.80
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean isVersusLine(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String cleaned = cleanLine(line);

        cleaned = cleaned
                .replaceAll("[^A-Za-z0-9/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return false;
        }

        return fuzzyMatchingService.matches(
                cleaned,
                "versus",
                0.80
        ) || fuzzyMatchingService.matches(
                cleaned,
                "vs",
                0.80
        ) || fuzzyMatchingService.matches(
                cleaned,
                "v/s",
                0.80
        );
    }

    private int[] findVersusAcrossPages(
            String[] pages,
            int petitionerPageIndex,
            int petitionerLineIndex) {

        int checkedLines = 0;

        int pageIndex =
                petitionerPageIndex;

        int lineIndex =
                petitionerLineIndex + 1;

        while (pageIndex < pages.length
                && checkedLines < 6) {

            String[] lines =
                    pages[pageIndex].split("\\R");

            while (lineIndex < lines.length
                    && checkedLines < 6) {

                String nextLine =
                        cleanLine(lines[lineIndex]);

                if (!nextLine.isBlank()) {

                    if (isVersusLine(nextLine)) {

                        return new int[]{
                                pageIndex,
                                lineIndex
                        };
                    }

                    checkedLines++;
                }

                lineIndex++;
            }

            pageIndex++;
            lineIndex = 0;
        }

        return null;
    }

    private List<Draft_party> parsePetitionerBlock(
            String[] pages,
            int[] startPosition,
            int petitionerPageIndex,
            int petitionerLineIndex) {

        List<Draft_party> parties =
                new ArrayList<>();

        List<String> petitionerLines =
                collectPetitionerLines(
                        pages,
                        startPosition,
                        petitionerPageIndex,
                        petitionerLineIndex
                );

        if (petitionerLines.isEmpty()) {
            return parties;
        }

        boolean numberedMode = false;

        for (String line : petitionerLines) {

            if (parseSerial(line) != null) {
                numberedMode = true;
                break;
            }
        }

        if (numberedMode) {

            StringBuilder currentName =
                    new StringBuilder();

            int currentSerial = 0;

            for (String line : petitionerLines) {

                Integer serial =
                        parseSerial(line);

                if (serial != null) {

                    if (currentSerial > 0
                            && !currentName.isEmpty()) {

                        addParty(
                                parties,
                                "P",
                                currentSerial,
                                currentName.toString()
                        );
                    }

                    currentSerial = serial;

                    currentName =
                            new StringBuilder(
                                    removeNumber(line)
                            );

                    continue;
                }

                if (currentSerial > 0) {

                    currentName
                            .append(" ")
                            .append(line);
                }
            }

            if (currentSerial > 0
                    && !currentName.isEmpty()) {

                addParty(
                        parties,
                        "P",
                        currentSerial,
                        currentName.toString()
                );
            }

            return parties;
        }

        StringBuilder unnumberedParty =
                new StringBuilder();

        for (String line : petitionerLines) {

            String cleanedLine =
                    removePartyRole(line);

            if (cleanedLine.isBlank()) {
                continue;
            }

            if (!unnumberedParty.isEmpty()) {
                unnumberedParty.append(" ");
            }

            unnumberedParty.append(cleanedLine);
        }

        if (!unnumberedParty.isEmpty()) {

            addParty(
                    parties,
                    "P",
                    1,
                    unnumberedParty.toString()
            );
        }

        return parties;
    }
    private List<String> collectPetitionerLines(
            String[] pages,
            int[] startPosition,
            int petitionerPageIndex,
            int petitionerLineIndex) {

        List<String> lines =
                new ArrayList<>();

        int pageIndex =
                startPosition[0];

        int lineIndex =
                startPosition[1];

        for (; pageIndex <= petitionerPageIndex; pageIndex++) {

            String[] pageLines =
                    pages[pageIndex].split("\\R");

            int start =
                    pageIndex == startPosition[0]
                            ? lineIndex
                            : 0;

            int end =
                    pageIndex == petitionerPageIndex
                            ? petitionerLineIndex + 1
                            : pageLines.length;

            for (int i = start;
                 i < end;
                 i++) {

                String line =
                        cleanLine(pageLines[i]);

                if (line.isBlank()) {
                    continue;
                }

                if (isVersusLine(line)) {
                    return lines;
                }

                if (isListOfDatesLine(line)) {
                    return lines;
                }

                lines.add(line);
            }
        }

        return lines;
    }

    private List<Draft_party> parseRespondentBlock(
            String[] pages,
            int[] versusPosition) {

        List<Draft_party> parties =
                new ArrayList<>();

        List<String> respondentLines =
                collectRespondentLines(
                        pages,
                        versusPosition
                );

        if (respondentLines.isEmpty()) {
            return parties;
        }

        boolean numberedMode = false;

        for (String line : respondentLines) {

            if (parseSerial(line) != null) {
                numberedMode = true;
                break;
            }
        }

        if (numberedMode) {

            StringBuilder currentName =
                    new StringBuilder();

            int currentSerial = 0;

            for (String line : respondentLines) {

                Integer serial =
                        parseSerial(line);

                if (serial != null) {

                    if (currentSerial > 0
                            && !currentName.isEmpty()) {

                        addParty(
                                parties,
                                "R",
                                currentSerial,
                                currentName.toString()
                        );
                    }

                    currentSerial = serial;

                    currentName =
                            new StringBuilder(
                                    removeNumber(line)
                            );

                    continue;
                }

                if (currentSerial > 0) {

                    currentName
                            .append(" ")
                            .append(line);
                }
            }

            if (currentSerial > 0
                    && !currentName.isEmpty()) {

                addParty(
                        parties,
                        "R",
                        currentSerial,
                        currentName.toString()
                );
            }

            return parties;
        }

        StringBuilder unnumberedParty =
                new StringBuilder();

        for (String line : respondentLines) {

            if (!unnumberedParty.isEmpty()) {
                unnumberedParty.append(" ");
            }

            unnumberedParty.append(line);
        }

        if (!unnumberedParty.isEmpty()) {

            addParty(
                    parties,
                    "R",
                    1,
                    unnumberedParty.toString()
            );
        }

        return parties;
    }
    private List<String> collectRespondentLines(
            String[] pages,
            int[] versusPosition) {

        List<String> lines =
                new ArrayList<>();

        int pageIndex =
                versusPosition[0];

        int lineIndex =
                versusPosition[1] + 1;

        for (; pageIndex < pages.length; pageIndex++) {

            String[] pageLines =
                    pages[pageIndex].split("\\R");

            int start =
                    pageIndex == versusPosition[0]
                            ? lineIndex
                            : 0;

            for (int i = start;
                 i < pageLines.length;
                 i++) {

                String line =
                        cleanLine(pageLines[i]);

                if (line.isBlank()) {
                    continue;
                }

                if (isListOfDatesLine(line)) {
                    return lines;
                }

                if (isOppositePartyLine(line)) {
                    return lines;
                }

                lines.add(line);
            }
        }

        return lines;
    }


    private void addParty(
            List<Draft_party> parties,
            String type,
            int serial,
            String name) {

        String cleanedName =
                cleanLine(name);

        if (cleanedName.isBlank()) {
            return;
        }

        String address = "";
        String district = "";

        if ("P".equals(type)) {
            address = extractAddress(cleanedName);
            district = extractDistrictFromAddress(address);
        }

        Draft_party party =
                new Draft_party();

        party.setType(type);
        party.setName(cleanedName);
        party.setSerialNo(serial);
        party.setAddress(address);
        party.setDistrict(district);

        parties.add(party);
    }
    private boolean containsCaseYear(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        if (line.matches(".*\\b20\\d{2}\\b.*")) {
            return true;
        }

        return line.matches(".*\\b\\d{1,6}(20\\d{2})\\b.*");
    }

    private String extractAddress(String petitionerText) {

        if (petitionerText == null || petitionerText.isBlank()) {
            return "";
        }

        String[] markers = {
                "resident of",
                "resident at",
                "residents of",
                "residents at",
                "residing at",
                "residing in",
                "r/o",
                "r.o.",
                "r o"
        };

        String lowerText = petitionerText.toLowerCase();

        for (String marker : markers) {

            int index = lowerText.indexOf(marker);

            if (index != -1) {
                return petitionerText
                        .substring(index + marker.length())
                        .trim();
            }
        }

        String[] words = petitionerText.split("\\s+");

        for (int i = 0; i < words.length; i++) {

            String word =
                    words[i].replaceAll(
                            "[^A-Za-z]",
                            ""
                    );

            if (fuzzyMatchingService.matches(
                    word,
                    "resident",
                    0.80
            ) || fuzzyMatchingService.matches(
                    word,
                    "residing",
                    0.80
            )) {

                if (i + 1 < words.length) {

                    String next =
                            words[i + 1]
                                    .replaceAll(
                                            "[^A-Za-z]",
                                            ""
                                    );

                    if (fuzzyMatchingService.matches(
                            next,
                            "of",
                            0.80
                    ) || fuzzyMatchingService.matches(
                            next,
                            "at",
                            0.80
                    ) || fuzzyMatchingService.matches(
                            next,
                            "in",
                            0.80
                    )) {

                        StringBuilder address =
                                new StringBuilder();

                        for (int j = i + 2;
                             j < words.length;
                             j++) {

                            if (address.length() > 0) {
                                address.append(" ");
                            }

                            address.append(words[j]);
                        }

                        return address.toString().trim();
                    }
                }
            }
        }

        return "";
    }
    private String extractDistrictFromAddress(String address) {

        if (address == null || address.isBlank()) {
            System.out.println("ADDRESS IS EMPTY");
            return "Not found";
        }


        String[] words =
                address.split("\\s+");

        for (int i = 0; i < words.length; i++) {

            String word =
                    words[i]
                            .replaceAll(
                                    "[^A-Za-z]",
                                    ""
                            );

            if (word.isBlank()) {
                continue;
            }

            if (fuzzyMatchingService.matches(
                    word,
                    "district",
                    0.80
            )) {

                StringBuilder district =
                        new StringBuilder();

                for (int j = i + 1;
                     j < Math.min(i + 5, words.length);
                     j++) {

                    String nextWord =
                            words[j].replaceAll(
                                    "[^A-Za-z,.-]",
                                    ""
                            );

                    if (nextWord.isBlank()) {
                        continue;
                    }

                    if (district.length() > 0) {
                        district.append(" ");
                    }

                    district.append(
                            nextWord.replaceAll(
                                    "[,\\.]+$",
                                    ""
                            )
                    );

                    if (words[j].contains(",")
                            || words[j].contains(".")) {

                        break;
                    }
                }

                if (!district.isEmpty()) {

                    return district.toString().trim();
                }
            }
        }

        return "Not found";
    }


    private Integer parseSerial(String line) {

        if (line == null || line.isBlank()) {
            return null;
        }

        String cleaned = line.trim();

        cleaned = cleaned.replaceFirst(
                "^[|}\\[\\]\\\\]+\\s*",
                ""
        );

        Matcher matcher = Pattern.compile(
                "^(\\d{1,2})\\s*[.,)\\-:]\\s*.+$"
        ).matcher(cleaned);

        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }

        Matcher romanMatcher = Pattern.compile(
                "^([IVXLCDMivxlcdm]{1,6})\\s*[.)\\-:]\\s+.+$"
        ).matcher(cleaned);

        if (romanMatcher.matches()) {
            return romanToInt(romanMatcher.group(1));
        }

        return null;
    }

    private int romanToInt(
            String roman) {

        if (roman == null || roman.isBlank()) {
            return -1;
        }

        roman =
                roman.toUpperCase();

        int total = 0;
        int previous = 0;

        for (int i = roman.length() - 1;
             i >= 0;
             i--) {

            char c =
                    roman.charAt(i);

            int value;

            switch (c) {

                case 'I':
                    value = 1;
                    break;

                case 'V':
                    value = 5;
                    break;

                case 'X':
                    value = 10;
                    break;

                case 'L':
                    value = 50;
                    break;

                case 'C':
                    value = 100;
                    break;

                case 'D':
                    value = 500;
                    break;

                case 'M':
                    value = 1000;
                    break;

                default:
                    return -1;
            }

            if (value < previous) {
                total -= value;
            } else {
                total += value;
            }

            previous = value;
        }

        return total;
    }

    private String removeNumber(
            String line) {

        if (line == null) {
            return "";
        }

        return line.replaceFirst(
                "^\\s*[|}\\[\\]\\\\]*\\s*(?:\\d{1,2}|[IVXLCDMivxlcdm]+)\\s*[.,)\\-]?\\s*",
                ""
        ).trim();
    }

    private String removePartyRole(String line) {

        if (line == null || line.isBlank()) {
            return "";
        }

        String cleaned = line
                .replaceAll("[^A-Za-z/() ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        Matcher matcher =
                Pattern.compile(
                        "([A-Za-z]+(?:\\s*/\\s*[A-Za-z]+|\\s*\\(\\s*[A-Za-z]+\\s*\\))?)\\s*$"
                ).matcher(cleaned);

        if (!matcher.find()) {
            return cleaned;
        }

        String role = matcher.group(1).trim();

        role = role
                .replaceAll("[()]", " / ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] roles =
                role.split("\\s*/\\s*");

        for (String currentRole : roles) {

            currentRole = currentRole.trim();

            if (fuzzyMatchingService.matches(
                    currentRole,
                    "petitioner",
                    0.80
            ) || fuzzyMatchingService.matches(
                    currentRole,
                    "applicant",
                    0.80
            ) || fuzzyMatchingService.matches(
                    currentRole,
                    "appellant",
                    0.80
            ) || fuzzyMatchingService.matches(
                    currentRole,
                    "revisionist",
                    0.80
            )) {

                return cleaned
                        .substring(0, matcher.start())
                        .trim();
            }
        }

        return cleaned;
    }
    private boolean isOppositePartyLine(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String cleaned = line
                .replaceAll("[^A-Za-z0-9/ ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        cleaned = cleaned
                .replaceAll("\\b0pposite\\b", "opposite")
                .replaceAll("\\b0pp\\b", "opp");

        String[] words = cleaned.split("\\s+");

        for (String word : words) {

            if (fuzzyMatchingService.matches(
                    word,
                    "respondent",
                    0.80
            )
                    || fuzzyMatchingService.matches(
                    word,
                    "respondents",
                    0.80
            )
                    || fuzzyMatchingService.matches(
                    word,
                    "opposite",
                    0.80
            )
                    || fuzzyMatchingService.matches(
                    word,
                    "opp",
                    0.80
            )) {

                if (fuzzyMatchingService.containsKeyword(
                        cleaned,
                        "party",
                        0.80
                )
                        || fuzzyMatchingService.containsKeyword(
                        cleaned,
                        "parties",
                        0.80
                )) {

                    return true;
                }

                if (fuzzyMatchingService.matches(
                        word,
                        "respondent",
                        0.80
                )
                        || fuzzyMatchingService.matches(
                        word,
                        "respondents",
                        0.80
                )) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isListOfDatesLine(String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String cleaned = line
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();

        return fuzzyMatchingService.containsKeyword(
                cleaned,
                "list of dates",
                0.80
        )
                || fuzzyMatchingService.containsKeyword(
                cleaned,
                "dates of events",
                0.80
        )
                || fuzzyMatchingService.containsKeyword(
                cleaned,
                "date of events",
                0.80
        );
    }

    private boolean isBracketLine(
            String line) {

        if (line == null || line.isBlank()) {
            return false;
        }

        String cleaned =
                cleanLine(line);

        return cleaned.matches(
                "^[\\(\\[\\{<].*[\\)\\]\\}>]$"
        );
    }

    private String cleanLine(
            String line) {

        if (line == null) {
            return "";
        }

        return line
                .replace('\u00A0', ' ')
                .replaceAll(
                        "(?i)---\\s*PAGE\\s*\\d+\\s*---",
                        ""
                )
                .replaceAll("\\s+", " ")
                .trim();
    }
}