package com.app.expiry_system.ocr.service;

import com.app.expiry_system.auth.entity.AppUser;
import com.app.expiry_system.ocr.dto.OcrScanRequest;
import com.app.expiry_system.ocr.dto.OcrScanResponse;
import com.app.expiry_system.ocr.dto.SuggestedIngredientPreview;
import com.app.expiry_system.restaurant.repository.RestaurantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OcrScanService {

    private static final Pattern DMY_DATE = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])([/-])(0?[1-9]|1[0-2])\\2(\\d{2}|\\d{4})\\b");
    private static final Pattern YMD_DATE = Pattern.compile("\\b(\\d{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12]\\d|3[01])\\b");
    private static final Pattern MONTH_YEAR = Pattern.compile("(?<![\\d/-])(0?[1-9]|1[0-2])([/-])(\\d{2}|\\d{4})(?![\\d/-])");
    private static final Pattern TEXT_MONTH = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])\\s+([A-Z]{3,9})\\s+(\\d{2}|\\d{4})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPACT_DATE = Pattern.compile("(?<!\\d)(\\d{8}|\\d{6})(?!\\d)");
    private static final Pattern EXPIRY_KEYWORD = Pattern.compile("(?i)(EXP|EXPIRY|EXPIRES|BBF|BBE|BEST\\s*BEFORE|USE\\s*BY|วันหมดอายุ|หมดอายุ|ควรบริโภคก่อน)");
    private static final Pattern MFG_KEYWORD = Pattern.compile("(?i)(MFG|MFD|MANUFACTURED|PROD|ผลิต|วันที่ผลิต)");

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("JANUARY", 1),
            Map.entry("FEB", 2), Map.entry("FEBRUARY", 2),
            Map.entry("MAR", 3), Map.entry("MARCH", 3),
            Map.entry("APR", 4), Map.entry("APRIL", 4),
            Map.entry("MAY", 5),
            Map.entry("JUN", 6), Map.entry("JUNE", 6),
            Map.entry("JUL", 7), Map.entry("JULY", 7),
            Map.entry("AUG", 8), Map.entry("AUGUST", 8),
            Map.entry("SEP", 9), Map.entry("SEPT", 9), Map.entry("SEPTEMBER", 9),
            Map.entry("OCT", 10), Map.entry("OCTOBER", 10),
            Map.entry("NOV", 11), Map.entry("NOVEMBER", 11),
            Map.entry("DEC", 12), Map.entry("DECEMBER", 12)
    );

    private final RestaurantRepository restaurantRepository;

    public OcrScanService(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public OcrScanResponse extractExpiryDate(OcrScanRequest request, AppUser currentUser) {
        validateRestaurantAccess(request.getRestaurantId(), currentUser);

        String rawText = request.getRawText().trim();
        List<DateCandidate> candidates = new ArrayList<>();
        collectDmy(rawText, candidates);
        collectYmd(rawText, candidates);
        collectMonthYear(rawText, candidates);
        collectTextMonth(rawText, candidates);
        collectCompact(rawText, candidates);

        if (candidates.isEmpty()) {
            return new OcrScanResponse(
                    rawText,
                    null,
                    null,
                    "LOW",
                    List.of("ไม่พบวันที่หมดอายุจากข้อความ OCR"),
                    emptyIngredientPreview(),
                    Instant.now(),
                    currentUser.getId()
            );
        }

        candidates.sort(Comparator.comparingInt(DateCandidate::score).reversed()
                .thenComparing(DateCandidate::start));
        DateCandidate selected = candidates.get(0);
        List<String> warnings = new ArrayList<>();
        if (selected.expiryDate.isBefore(LocalDate.now())) {
            warnings.add("วันที่หมดอายุผ่านไปแล้ว");
        }
        if (hasMfgKeywordNear(rawText, selected.start) && !hasExpiryKeywordNear(rawText, selected.start)) {
            warnings.add("วันที่ที่พบอยู่ใกล้ keyword วันที่ผลิต ควรตรวจสอบก่อนบันทึก");
        }

        return new OcrScanResponse(
                rawText,
                selected.expiryDate,
                selected.matchedText,
                resolveConfidence(selected, warnings),
                warnings,
                emptyIngredientPreview(),
                Instant.now(),
                currentUser.getId()
        );
    }

    public OcrScanResponse scan(OcrScanRequest request, AppUser currentUser) {
        return extractExpiryDate(request, currentUser);
    }

    public void validateImageScanAccess(String restaurantId, AppUser currentUser) {
        validateRestaurantAccess(restaurantId, currentUser);
    }

    private void collectDmy(String rawText, List<DateCandidate> candidates) {
        Matcher matcher = DMY_DATE.matcher(rawText);
        while (matcher.find()) {
            parseDate(
                    normalizeYear(Integer.parseInt(matcher.group(4))),
                    Integer.parseInt(matcher.group(3)),
                    Integer.parseInt(matcher.group(1))
            ).ifPresent(date -> candidates.add(new DateCandidate(date, matcher.group(), matcher.start(),
                    score(rawText, matcher.start(), false))));
        }
    }

    private void collectYmd(String rawText, List<DateCandidate> candidates) {
        Matcher matcher = YMD_DATE.matcher(rawText);
        while (matcher.find()) {
            parseDate(
                    normalizeYear(Integer.parseInt(matcher.group(1))),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            ).ifPresent(date -> candidates.add(new DateCandidate(date, matcher.group(), matcher.start(),
                    score(rawText, matcher.start(), false))));
        }
    }

    private void collectMonthYear(String rawText, List<DateCandidate> candidates) {
        Matcher matcher = MONTH_YEAR.matcher(rawText);
        while (matcher.find()) {
            int year = normalizeYear(Integer.parseInt(matcher.group(3)));
            int month = Integer.parseInt(matcher.group(1));
            try {
                LocalDate date = YearMonth.of(year, month).atEndOfMonth();
                candidates.add(new DateCandidate(date, matcher.group(), matcher.start(),
                        score(rawText, matcher.start(), true)));
            } catch (Exception ignored) {
                // Invalid OCR date candidates are skipped.
            }
        }
    }

    private void collectTextMonth(String rawText, List<DateCandidate> candidates) {
        Matcher matcher = TEXT_MONTH.matcher(rawText);
        while (matcher.find()) {
            Integer month = MONTHS.get(matcher.group(2).toUpperCase(Locale.ROOT));
            if (month == null) {
                continue;
            }
            parseDate(
                    normalizeYear(Integer.parseInt(matcher.group(3))),
                    month,
                    Integer.parseInt(matcher.group(1))
            ).ifPresent(date -> candidates.add(new DateCandidate(date, matcher.group(), matcher.start(),
                    score(rawText, matcher.start(), false))));
        }
    }

    /**
     * รองรับฉลากที่พิมพ์วันที่ติดกันโดยไม่มีตัวคั่น เช่น "MFG200526" หรือ "EXP20250529".
     * ตัวเลขติดกันมีโอกาสเป็น lot code สูง จึงรับเฉพาะกลุ่มที่มี keyword วันที่กำกับอยู่บรรทัดเดียวกัน.
     */
    private void collectCompact(String rawText, List<DateCandidate> candidates) {
        Matcher matcher = COMPACT_DATE.matcher(rawText);
        while (matcher.find()) {
            int start = matcher.start();
            if (!hasExpiryKeywordNear(rawText, start) && !hasMfgKeywordNear(rawText, start)) {
                continue;
            }
            parseCompactDigits(matcher.group(1))
                    .ifPresent(date -> candidates.add(new DateCandidate(date, matcher.group(), start,
                            score(rawText, start, false))));
        }
    }

    private Optional<LocalDate> parseCompactDigits(String digits) {
        int first = Integer.parseInt(digits.substring(0, 2));
        int middle = Integer.parseInt(digits.substring(2, 4));

        if (digits.length() == 8) {
            int leadingYear = Integer.parseInt(digits.substring(0, 4));
            int trailingYear = Integer.parseInt(digits.substring(4, 8));
            int lastPair = Integer.parseInt(digits.substring(6, 8));
            return firstValid(
                    parseDate(normalizeYear(leadingYear), Integer.parseInt(digits.substring(4, 6)), lastPair),
                    parseDate(normalizeYear(trailingYear), middle, first)
            );
        }

        int last = Integer.parseInt(digits.substring(4, 6));
        return firstValid(
                parseDate(normalizeYear(last), middle, first),
                parseDate(normalizeYear(first), middle, last),
                parseDate(normalizeYear(last), first, middle)
        );
    }

    @SafeVarargs
    private Optional<LocalDate> firstValid(Optional<LocalDate>... options) {
        for (Optional<LocalDate> option : options) {
            if (option.isPresent()) {
                return option;
            }
        }
        return Optional.empty();
    }

    private Optional<LocalDate> parseDate(int year, int month, int day) {
        try {
            return Optional.of(LocalDate.of(year, month, day));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private int normalizeYear(int year) {
        if (year < 100) {
            return 2000 + year;
        }
        if (year >= 2400 && year <= 2600) {
            return year - 543;
        }
        return year;
    }

    private int score(String rawText, int dateStart, boolean monthOnly) {
        int score = monthOnly ? 40 : 60;
        if (hasExpiryKeywordNear(rawText, dateStart)) {
            score += 100;
        }
        if (hasMfgKeywordNear(rawText, dateStart)) {
            score -= 80;
        }
        return score;
    }

    private boolean hasExpiryKeywordNear(String rawText, int dateStart) {
        return hasKeywordNear(EXPIRY_KEYWORD, rawText, dateStart);
    }

    private boolean hasMfgKeywordNear(String rawText, int dateStart) {
        return hasKeywordNear(MFG_KEYWORD, rawText, dateStart);
    }

    private boolean hasKeywordNear(Pattern pattern, String rawText, int dateStart) {
        int lineStart = rawText.lastIndexOf('\n', Math.max(0, dateStart - 1)) + 1;
        int from = Math.max(lineStart, dateStart - 40);
        String prefix = rawText.substring(from, dateStart);
        return pattern.matcher(prefix).find();
    }

    private String resolveConfidence(DateCandidate selected, List<String> warnings) {
        if (selected.score < 0) {
            return "LOW";
        }
        if (!warnings.isEmpty()) {
            return "MEDIUM";
        }
        if (selected.score >= 140) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private SuggestedIngredientPreview emptyIngredientPreview() {
        return new SuggestedIngredientPreview(null, null, null, null);
    }

    private void validateRestaurantAccess(String restaurantId, AppUser currentUser) {
        if (restaurantId == null || restaurantId.isBlank()) {
            throw new IllegalArgumentException("restaurantId is required");
        }
        if (currentUser.getRestaurantId() == null || currentUser.getRestaurantId().isBlank()) {
            throw new IllegalArgumentException("User is not assigned to any restaurant");
        }
        if (!restaurantId.equals(currentUser.getRestaurantId())) {
            throw new IllegalArgumentException("Unauthorized to access this restaurant");
        }
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new IllegalArgumentException("Restaurant not found");
        }
    }

    private record DateCandidate(LocalDate expiryDate, String matchedText, int start, int score) {
    }
}
