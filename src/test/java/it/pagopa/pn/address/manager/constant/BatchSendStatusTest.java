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
	@Test
	void testFromValue1 () {
		// Arrange, Act and Assert
		assertThrows(IllegalArgumentException.class, () -> BatchStatus.fromValue("42"));
		assertEquals(BatchStatus.NO_BATCH_ID, BatchStatus.fromValue("NO_BATCH_ID"));
		assertEquals(BatchStatus.NOT_WORKED, BatchStatus.fromValue("NOT_WORKED"));
		assertEquals(BatchStatus.WORKING, BatchStatus.fromValue("WORKING"));
		assertEquals(BatchStatus.WORKED, BatchStatus.fromValue("WORKED"));
		assertEquals(BatchStatus.ERROR, BatchStatus.fromValue("ERROR"));
		assertEquals(BatchStatus.TAKEN_CHARGE, BatchStatus.fromValue("TAKEN_CHARGE"));
	}
}

