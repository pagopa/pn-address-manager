package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.common.rest.error.v1.dto.ProblemError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PnAddressManagerExceptionTest {
    /**
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("An error occurred", "",
                HttpStatus.CONTINUE.value(), "");

        assertNull(actualPnAddressManagerException.getCause());
        assertEquals(0, actualPnAddressManagerException.getSuppressed().length);
        assertEquals(HttpStatus.CONTINUE.value(), actualPnAddressManagerException.getStatus());
        assertEquals("An error occurred", actualPnAddressManagerException.getMessage());
        assertEquals("An error occurred", actualPnAddressManagerException.getLocalizedMessage());
    }

    /**
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor2() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("An error occurred",
                "The characteristics of someone or something", 2, "An error occurred");

        assertEquals(100, actualPnAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something", actualPnAddressManagerException.getDescription());
        Problem problem = actualPnAddressManagerException.getProblem();
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
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor6() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("foo", "foo", 1, "foo");

        assertEquals(100, actualPnAddressManagerException.getStatus());
        assertEquals("foo", actualPnAddressManagerException.getDescription());
        Problem problem = actualPnAddressManagerException.getProblem();
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
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor7() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("",
                "The characteristics of someone or something", 2, "An error occurred");

        assertEquals(100, actualPnAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something", actualPnAddressManagerException.getDescription());
        Problem problem = actualPnAddressManagerException.getProblem();
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
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor8() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("An error occurred", "",
                2, "An error occurred");

        assertEquals(100, actualPnAddressManagerException.getStatus());
        assertEquals("", actualPnAddressManagerException.getDescription());
        Problem problem = actualPnAddressManagerException.getProblem();
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
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, String, int, String)}
     */
    @Test
    void testConstructor9() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("An error occurred",
                "The characteristics of someone or something", 4096, "An error occurred");

        assertEquals(600, actualPnAddressManagerException.getStatus());
        assertEquals("The characteristics of someone or something", actualPnAddressManagerException.getDescription());
        Problem problem = actualPnAddressManagerException.getProblem();
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

