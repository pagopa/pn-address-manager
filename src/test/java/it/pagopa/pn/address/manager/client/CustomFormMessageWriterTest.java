package it.pagopa.pn.address.manager.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.*;

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

