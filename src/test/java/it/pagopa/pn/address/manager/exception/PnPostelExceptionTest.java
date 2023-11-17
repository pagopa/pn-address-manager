package it.pagopa.pn.address.manager.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;

class PnPostelExceptionTest {
	/**
	 * Method under test: {@link PnPostelException#PnPostelException(String, String)}
	 */
	@Test
	void testConstructor () {
		// Arrange and Act
		PnPostelException actualPnPostelException = new PnPostelException("An error occurred", "An error occurred");

		// Assert
		assertEquals("An error occurred", actualPnPostelException.getError());
		Problem problem = actualPnPostelException.getProblem();
		assertEquals("An error occurred", problem.getDetail());
		List<ProblemError> errors = problem.getErrors();
		ProblemError getResult = errors.get(0);
		assertEquals("An error occurred", getResult.getCode());
		assertEquals("GENERIC_ERROR", problem.getType());
		assertEquals("Internal Server Error", problem.getTitle());
		assertEquals("Z", problem.getTimestamp().getOffset().toString());
		assertEquals("none", getResult.getDetail());
		assertNull(problem.getTraceId());
		assertNull(getResult.getElement());
		assertEquals(1, errors.size());
		assertEquals(500, actualPnPostelException.getStatus());
	}

	/**
	 * Method under test: {@link PnPostelException#PnPostelException(String, String, Throwable)}
	 */
	@Test
	void testConstructor2 () {
		// Arrange and Act
		PnPostelException actualPnPostelException = new PnPostelException("An error occurred", "An error occurred", null);

		// Assert
		Problem problem = actualPnPostelException.getProblem();
		assertEquals("An error occurred", problem.getDetail());
		List<ProblemError> errors = problem.getErrors();
		ProblemError getResult = errors.get(0);
		assertEquals("An error occurred", getResult.getCode());
		assertEquals("GENERIC_ERROR", problem.getType());
		assertEquals("Internal Server Error", problem.getTitle());
		assertEquals("Z", problem.getTimestamp().getOffset().toString());
		assertEquals("none", getResult.getDetail());
		assertNull(problem.getTraceId());
		assertNull(getResult.getElement());
		assertEquals(1, errors.size());
		assertEquals(500, actualPnPostelException.getStatus());
	}
}
