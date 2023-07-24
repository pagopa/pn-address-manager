package it.pagopa.pn.address.manager.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;

import java.nio.file.FileSystems;

import org.junit.jupiter.api.Test;

class JsonUtilsTest {
	/**
	 * Method under test: {@link JsonUtils#fromJson(String, Class)}
	 */
	@Test
	void testFromJson () {
		// Arrange, Act and Assert
		assertThrows(PnAddressManagerException.class, () -> JsonUtils.fromJson("Json", Object.class));
		assertThrows(PnAddressManagerException.class, () -> JsonUtils.fromJson(null, Object.class));
		assertThrows(PnAddressManagerException.class, () -> JsonUtils.fromJson("", Object.class));
		assertThrows(PnAddressManagerException.class, () -> JsonUtils.fromJson("Json", null));
	}
	/**
	 * Method under test: {@link JsonUtils#writeValueAsString(Object)}
	 */
	@Test
	void testWriteValueAsString () {
		// Arrange, Act and Assert
		assertEquals("\"Value\"", JsonUtils.writeValueAsString("Value"));
		assertThrows(PnAddressManagerException.class, () -> JsonUtils.writeValueAsString(FileSystems.getDefault()));
		assertEquals("42", JsonUtils.writeValueAsString(42));
		assertEquals("19088743", JsonUtils.writeValueAsString(19088743));
	}
}

