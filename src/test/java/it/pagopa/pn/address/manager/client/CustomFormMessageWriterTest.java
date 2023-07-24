package it.pagopa.pn.address.manager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CustomFormMessageWriterTest {
	/**
	 * Method under test: {@link CustomFormMessageWriter#getMediaType(MediaType)}
	 */
	@Test
	void testGetMediaType2 () {
		// Arrange
		CustomFormMessageWriter customFormMessageWriter = new CustomFormMessageWriter();

		// Act
		MediaType actualMediaType = customFormMessageWriter.getMediaType(null);

		// Assert
		Charset expectedCharset = customFormMessageWriter.DEFAULT_CHARSET;
		assertSame(expectedCharset, actualMediaType.getCharset());
		assertFalse(actualMediaType.isWildcardType());
		assertEquals(1, actualMediaType.getParameters().size());
		assertNull(actualMediaType.getSubtypeSuffix());
		assertEquals(1.0d, actualMediaType.getQualityValue());
	}

	/**
	 * Method under test: {@link CustomFormMessageWriter#getMediaType(MediaType)}
	 */
	@Test
	void testGetMediaType3 () {
		// Arrange
		CustomFormMessageWriter customFormMessageWriter = new CustomFormMessageWriter();
		MediaType mediaType = new MediaType("Type");

		// Act and Assert
		assertSame(mediaType, customFormMessageWriter.getMediaType(mediaType));
	}
}

