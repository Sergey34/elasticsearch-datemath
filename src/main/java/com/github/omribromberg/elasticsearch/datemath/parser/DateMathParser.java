package com.github.omribromberg.elasticsearch.datemath.parser;

import lombok.Builder;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Builder
public class DateMathParser {
    private static final Map<Character, ChronoUnit> mathUnits = new HashMap<Character, ChronoUnit>() {{
        put('y', ChronoUnit.YEARS);
        put('M', ChronoUnit.MONTHS);
        put('w', ChronoUnit.WEEKS);
        put('d', ChronoUnit.DAYS);
        put('H', ChronoUnit.HOURS);
        put('h', ChronoUnit.HOURS);
        put('m', ChronoUnit.MINUTES);
        put('s', ChronoUnit.SECONDS);
    }};
    public static final String DEFAULT_PATTERN = "yyyy.MM.dd";

    @Builder.Default
    private String pattern = DEFAULT_PATTERN;
    @Builder.Default
    private ZoneId zone = ZoneOffset.UTC;
    @Builder.Default
    private Supplier<ZonedDateTime> nowSupplier = () -> ZonedDateTime.now(ZoneOffset.UTC);
    @Builder.Default
    private String nowPattern = "now";
    @Builder.Default
    private DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern(DEFAULT_PATTERN)
            .withZone(ZoneOffset.UTC);

    public ZonedDateTime resolveExpression(String expression) {
        return expression.startsWith(nowPattern) ? resolveNowExpression(expression) : resolveDateTimeExpression(expression);
    }

    private ZonedDateTime parseMathExpression(String mathExpression, ZonedDateTime time) {
        for (int i = 0; i < mathExpression.length(); ) {
            final int sign;
            final boolean round;
            char current = mathExpression.charAt(i++);
            if (current == '/') {
                round = true;
                sign = 1;
            } else {
                round = false;
                sign = getSign(mathExpression, current);
            }
            if (i >= mathExpression.length()) {
                throw new DateMathParseException(String.format("truncated date math %s", mathExpression));
            }
            final long num;
            if (!Character.isDigit(mathExpression.charAt(i))) {
                num = 1;
            } else {
                int numFrom = i;
                i = getLastDigitChar(mathExpression, i);
                if (i >= mathExpression.length()) {
                    throw new DateMathParseException(String.format("truncated date math %s", mathExpression));
                }
                num = Integer.parseInt(mathExpression.substring(numFrom, i));
            }
            if (round && num != 1) {
                throw new DateMathParseException(String.format("rounding `/` can only be used on single unit types %s", mathExpression));
            }
            char unit = mathExpression.charAt(i++);
            ChronoUnit mathUnit = mathUnits.get(unit);
            if (Objects.isNull(mathUnit)) {
                throw new DateMathParseException(String.format("unit %s not supported for date math %s", unit, mathExpression));
            }
            time = round ? time.truncatedTo(mathUnit) : time.plus(sign * num, mathUnit);
        }
        return time;
    }

    private int getLastDigitChar(String mathExpression, int i) {
        while (i < mathExpression.length() && Character.isDigit(mathExpression.charAt(i))) {
            i++;
        }
        return i;
    }

    private int getSign(String mathExpression, char current) {
        final int sign;
        if (current == '+') {
            sign = 1;
        } else if (current == '-') {
            sign = -1;
        } else {
            throw new DateMathParseException(String.format("operator not supported for date math %s", mathExpression));
        }
        return sign;
    }

    private ZonedDateTime parseDateTimeExpression(String dateTimeExpression) {
        try {
            return getDateTimeWithDefaults(dateTimeExpression);
        } catch (IllegalArgumentException e) {
            throw new DateMathParseException(String.format("failed to parse date field %s with format %s", dateTimeExpression, pattern), e);
        }
    }

    private ZonedDateTime resolveDateTimeExpression(String dateTimeExpression) {
        int index = dateTimeExpression.indexOf("||");

        if (index == -1) {
            return parseDateTimeExpression(dateTimeExpression);
        }

        final ZonedDateTime time = parseDateTimeExpression(dateTimeExpression.substring(0, index));
        final String mathString = dateTimeExpression.substring(index + 2);

        return parseMathExpression(mathString, time);
    }

    private ZonedDateTime resolveNowExpression(String expression) {
        return parseMathExpression(expression.substring(nowPattern.length()).trim(), nowSupplier.get());
    }

    private ZonedDateTime getDateTimeWithDefaults(String dateTimeExpression) {
        TemporalAccessor parsed = formatter.parse(dateTimeExpression);
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern(pattern);

        if (!parsed.isSupported(ChronoField.YEAR)) {
            builder.parseDefaulting(ChronoField.YEAR, 1970);
        }
        if (!parsed.isSupported(ChronoField.MONTH_OF_YEAR)) {
            builder.parseDefaulting(ChronoField.MONTH_OF_YEAR, 1);
        }
        if (!parsed.isSupported(ChronoField.DAY_OF_MONTH)) {
            builder.parseDefaulting(ChronoField.DAY_OF_MONTH, 1);
        }
        if (!parsed.isSupported(ChronoField.HOUR_OF_DAY)) {
            builder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0);
        }
        if (!parsed.isSupported(ChronoField.MINUTE_OF_HOUR)) {
            builder.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0);
        }
        if (!parsed.isSupported(ChronoField.INSTANT_SECONDS)) {
            builder.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0);
        }
        if (!parsed.isSupported(ChronoField.MILLI_OF_SECOND)) {
            builder.parseDefaulting(ChronoField.MILLI_OF_SECOND, 0);
        }

        return ZonedDateTime.parse(dateTimeExpression, builder.toFormatter().withZone(zone));
    }
}
