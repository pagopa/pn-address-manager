package it.pagopa.pn.address.manager.converter;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

public class LocalDateTimeToInstant implements AttributeConverter<LocalDateTime> {

    @Override
    public AttributeValue transformFrom(LocalDateTime o) {
        return AttributeValue.builder().s(o.toInstant(ZoneOffset.UTC).toString()).build();
    }

    @Override
    public LocalDateTime transformTo(AttributeValue attributeValue) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendOptional(DateTimeFormatter.ISO_INSTANT)
                .appendOptional(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toFormatter();

        TemporalAccessor temporalAccessor = formatter.parseBest(attributeValue.s(), Instant::from, LocalDateTime::from);

        if(temporalAccessor instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDateTime();
        }
        return (LocalDateTime) temporalAccessor;
    }

    @Override
    public EnhancedType<LocalDateTime> type() {
        return EnhancedType.of(LocalDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
