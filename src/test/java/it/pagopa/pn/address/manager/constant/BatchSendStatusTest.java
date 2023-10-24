package it.pagopa.pn.address.manager.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BatchSendStatusTest {
	/**
	 * Method under test: {@link BatchSendStatus#fromValue(String)}
	 */
	@Test
	void testFromValue () {
		// Arrange, Act and Assert
		assertThrows(IllegalArgumentException.class, () -> BatchSendStatus.fromValue("42"));
		assertEquals(BatchSendStatus.NOT_SENT, BatchSendStatus.fromValue("NOT_SENT"));
		assertEquals(BatchSendStatus.SENT, BatchSendStatus.fromValue("SENT"));
		assertEquals(BatchSendStatus.ERROR, BatchSendStatus.fromValue("ERROR"));
		assertEquals(BatchSendStatus.SENT_TO_DLQ, BatchSendStatus.fromValue("SENT_TO_DLQ"));
	}

	/**
	 * Method under test: {@link BatchSendStatus#toString()}
	 */
	@Test
	void testToString () {
		// Arrange, Act and Assert
		assertEquals("NOT_SENT", BatchSendStatus.valueOf("NOT_SENT").toString());
	}
}

