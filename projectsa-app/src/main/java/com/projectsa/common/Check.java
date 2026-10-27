package com.projectsa.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Small helpers shared by the DetectBlank classes. Collects one Thai message per wrong field. */
public final class Check {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final Map<String, String> errors = new LinkedHashMap<>();

    public static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    public Check required(String field, String value, String message) {
        if (blank(value)) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public Check email(String field, String value) {
        if (!blank(value) && !EMAIL.matcher(value.strip()).matches()) {
            errors.putIfAbsent(field, "รูปแบบอีเมลไม่ถูกต้อง");
        }
        return this;
    }

    public Check rule(boolean ok, String field, String message) {
        if (!ok) {
            errors.putIfAbsent(field, message);
        }
        return this;
    }

    public LocalDate date(String field, String value, String message) {
        if (blank(value)) {
            errors.putIfAbsent(field, message);
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException e) {
            errors.putIfAbsent(field, "รูปแบบวันที่ไม่ถูกต้อง");
            return null;
        }
    }

    public LocalTime time(String field, String value, String message) {
        if (blank(value)) {
            errors.putIfAbsent(field, message);
            return null;
        }
        try {
            return LocalTime.parse(value.strip());
        } catch (DateTimeParseException e) {
            errors.putIfAbsent(field, "รูปแบบเวลาไม่ถูกต้อง");
            return null;
        }
    }

    public Map<String, String> errors() {
        return errors;
    }

    public void throwIfAny() {
        ApiException.throwIfAny(errors);
    }

    public static String trim(String s) {
        return s == null ? null : s.strip();
    }
}
