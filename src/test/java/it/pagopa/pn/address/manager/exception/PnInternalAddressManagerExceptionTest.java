package it.pagopa.pn.address.manager.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PnInternalAddressManagerExceptionTest {
    /**
     * Method under test: {@link PnInternalAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException("An error occurred", "",
                HttpStatus.CONTINUE.value(), "");

        assertNull(actualPnInternalAddressManagerException.getCause());
        assertEquals(0, actualPnInternalAddressManagerException.getSuppressed().length);
        assertEquals(HttpStatus.CONTINUE.value(), actualPnInternalAddressManagerException.getStatus());
        assertEquals("An error occurred", actualPnInternalAddressManagerException.getMessage());
        assertEquals("An error occurred", actualPnInternalAddressManagerException.getLocalizedMessage());
    }

    /**
     * Method under test: {@link PnInternalAddressManagerException#PnInternalAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor2() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException(
                "An error occurred", "The characteristics of someone or something", 2, "An error occurred");

        assertEquals(100, actualPnInternalAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something",
                actualPnInternalAddressManagerException.getDescription());
        Problem problem = actualPnInternalAddressManagerException.getProblem();
        assertEquals("An error occurred", problem.getTitle());
        assertEquals(100, problem.getStatus().intValue());
        assertEquals("GENERIC_ERROR", problem.getType());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertNull(problem.getTraceId());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        ProblemError getResult = errors.get(0);
        assertEquals("none", getResult.getDetail());
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
    }

    /**
     * Method under test: {@link PnInternalAddressManagerException#PnInternalAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor3() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException(
                "foo", "foo", 1, "foo");

        assertEquals(100, actualPnInternalAddressManagerException.getStatus());
        assertEquals("foo", actualPnInternalAddressManagerException.getDescription());
        Problem problem = actualPnInternalAddressManagerException.getProblem();
        assertEquals("foo", problem.getTitle());
        assertEquals(100, problem.getStatus().intValue());
        assertEquals("GENERIC_ERROR", problem.getType());
        assertEquals("foo", problem.getDetail());
        assertNull(problem.getTraceId());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        ProblemError getResult = errors.get(0);
        assertEquals("none", getResult.getDetail());
        assertEquals("foo", getResult.getCode());
        assertNull(getResult.getElement());
    }

    /**
     * Method under test: {@link PnInternalAddressManagerException#PnInternalAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor4() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException(
                "", "The characteristics of someone or something", 2, "An error occurred");

        assertEquals(100, actualPnInternalAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something",
                actualPnInternalAddressManagerException.getDescription());
        Problem problem = actualPnInternalAddressManagerException.getProblem();
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals(100, problem.getStatus().intValue());
        assertEquals("GENERIC_ERROR", problem.getType());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertNull(problem.getTraceId());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        ProblemError getResult = errors.get(0);
        assertEquals("none", getResult.getDetail());
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
    }

    /**
     * Method under test: {@link PnInternalAddressManagerException#PnInternalAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor5() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException(
                "An error occurred", "", 2, "An error occurred");

        assertEquals(100, actualPnInternalAddressManagerException.getStatus());
        assertEquals("", actualPnInternalAddressManagerException.getDescription());
        Problem problem = actualPnInternalAddressManagerException.getProblem();
        assertEquals("An error occurred", problem.getTitle());
        assertEquals(100, problem.getStatus().intValue());
        assertEquals("GENERIC_ERROR", problem.getType());
        assertEquals("Internal Server Error", problem.getDetail());
        assertNull(problem.getTraceId());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        ProblemError getResult = errors.get(0);
        assertEquals("none", getResult.getDetail());
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
    }

    /**
     * Method under test: {@link PnInternalAddressManagerException#PnInternalAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor6() {
        PnInternalAddressManagerException actualPnInternalAddressManagerException = new PnInternalAddressManagerException(
                "An error occurred", "The characteristics of someone or something", 4096, "An error occurred");

        assertEquals(600, actualPnInternalAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something",
                actualPnInternalAddressManagerException.getDescription());
        Problem problem = actualPnInternalAddressManagerException.getProblem();
        assertEquals("An error occurred", problem.getTitle());
        assertEquals(600, problem.getStatus().intValue());
        assertEquals("GENERIC_ERROR", problem.getType());
        assertEquals("The characteristics of someone or something", problem.getDetail());
        assertNull(problem.getTraceId());
        List<ProblemError> errors = problem.getErrors();
        assertEquals(1, errors.size());
        ProblemError getResult = errors.get(0);
        assertEquals("none", getResult.getDetail());
        assertEquals("An error occurred", getResult.getCode());
        assertNull(getResult.getElement());
    }
}

