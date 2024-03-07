package it.pagopa.pn.address.manager.converter;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalDateTimeToInstantTest {

    private final LocalDateTimeToInstant converter = new LocalDateTimeToInstant();

    @Test
    void testTransformFrom() {
        // Arrange
        LocalDateTime dateTime = LocalDateTime.of(2022, 1, 1, 12, 0, 0);
        String expected = "2022-01-01T12:00:00Z";

        // Act
        String result = converter.transformFrom(dateTime).s();

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void testTransformToWithInstant() {
        // Arrange
        String instantString = "2022-01-01T12:00:00Z";
        Instant instant = Instant.parse(instantString);
        LocalDateTime expected = LocalDateTime.of(2022, 1, 1, 12, 0, 0);

        // Act
        LocalDateTime result = converter.transformTo(AttributeValue.builder().s(instantString).build());

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void testTransformToWithLocalDateTime() {
        // Arrange
        String dateTimeString = "2022-01-01T12:00:00";
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
        LocalDateTime expected = LocalDateTime.of(2022, 1, 1, 12, 0, 0);

        // Act
        LocalDateTime result = converter.transformTo(AttributeValue.builder().s(dateTimeString).build());

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void testTransformToWithInvalidValue() {
        // Arrange
        String invalidValue = "invalid";

        // Act & Assert
        assertThrows(DateTimeParseException.class, () -> converter.transformTo(AttributeValue.builder().s(invalidValue).build()));
    }

    @Test
    void testType() {
        // Arrange
        EnhancedType<LocalDateTime> expectedType = EnhancedType.of(LocalDateTime.class);

        // Act
        EnhancedType<LocalDateTime> resultType = converter.type();

        // Assert
        assertEquals(expectedType, resultType);
    }

    @Test
    void testAttributeValueType() {
        // Arrange
        AttributeValueType expectedType = AttributeValueType.S;

        // Act
        AttributeValueType resultType = converter.attributeValueType();

        // Assert
        assertEquals(expectedType, resultType);
    }
}