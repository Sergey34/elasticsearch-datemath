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
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Matcher matcher = Pattern.compile("([-,+][0-9]+)([a-z])/?([a-z])?").matcher(mathExpression);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        int num = Integer.parseInt(matcher.group(1));
        ChronoUnit mathUnit = mathUnits.get(matcher.group(2).charAt(0));
        ChronoUnit roundMathUnit = getRoundMathUnit(matcher);
        time = roundMathUnit != null ? time.plus(num, mathUnit).truncatedTo(mathUnit) : time.plus(num, mathUnit);
        return time;
    }

    private ChronoUnit getRoundMathUnit(Matcher matcher) {
        String group = matcher.group(3);
        if (group == null) {
            return null;
        }
        return mathUnits.get(group.charAt(0));
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
