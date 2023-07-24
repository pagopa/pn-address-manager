package it.pagopa.pn.address.manager.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration (classes = {ActivateSINIConverter.class})
@ExtendWith (SpringExtension.class)
class ActivateSINIConverterTest {
	@Autowired
	private ActivateSINIConverter activateSINIConverter;

	/**
	 * Method under test: {@link ActivateSINIConverter#mapResponse(Object)}
	 */
	@Test
	void testMapResponse () {
		// Arrange, Act and Assert
		assertEquals("Resp", activateSINIConverter.mapResponse("Resp"));
	}
}

