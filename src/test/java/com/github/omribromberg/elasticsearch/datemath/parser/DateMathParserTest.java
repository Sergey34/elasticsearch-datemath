package com.github.omribromberg.elasticsearch.datemath.parser;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

class DateMathParserTest {
    @Test
    public void test() {
        DateMathParser dateMathParser = DateMathParser.builder()
                .pattern("yyyy-MM-dd HH-ss")
                .zone(ZoneId.of("America/New_York"))
                .formatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-ss").withZone(ZoneOffset.UTC))
                .build();
        ZonedDateTime zonedDateTime = dateMathParser.resolveExpression("1998-09-18 16-43||-4d");
        Assertions.assertThat(zonedDateTime.getDayOfMonth())
                .isEqualTo(14);
    }

    @Test
    public void now_minus_4d() {
        DateMathParser dateMathParser = DateMathParser.builder()
                .pattern("yyyy-MM-dd HH-ss")
                .zone(ZoneId.of("America/New_York"))
                .nowSupplier(() -> ZonedDateTime.parse("2011-10-05T14:48:00.000Z"))
                .build();
        ZonedDateTime zonedDateTime = dateMathParser.resolveExpression("now-4d");
        Assertions.assertThat(zonedDateTime.getDayOfMonth())
                .isEqualTo(1);
    }

    @Test
    public void now_minus_6m_plus_6y() {
        DateMathParser dateMathParser = DateMathParser.builder()
                .pattern("yyyy-MM-dd HH-ss")
                .zone(ZoneId.of("America/New_York"))
                .nowSupplier(() -> ZonedDateTime.parse("2011-10-05T14:48:00.000Z"))
                .build();
        ZonedDateTime zonedDateTime = dateMathParser.resolveExpression("now-6M+6y");
        Assertions.assertThat(zonedDateTime.getMonthValue())
                .isEqualTo(4);
        Assertions.assertThat(zonedDateTime.getYear())
                .isEqualTo(2017);
    }

    @Test
    public void now_minus_4d_slash_d() {
        DateMathParser dateMathParser = DateMathParser.builder()
                .pattern("yyyy-MM-dd HH-ss")
                .zone(ZoneId.of("America/New_York"))
                .nowSupplier(() -> ZonedDateTime.parse("2011-10-05T14:48:00.000Z"))
                .build();
        ZonedDateTime zonedDateTime = dateMathParser.resolveExpression("now-4d/d");
        Assertions.assertThat(zonedDateTime.getHour())
                .isEqualTo(0);
        Assertions.assertThat(zonedDateTime.getMinute())
                .isEqualTo(0);
    }
}