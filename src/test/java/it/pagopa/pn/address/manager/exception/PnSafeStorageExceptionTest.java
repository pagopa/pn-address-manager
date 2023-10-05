package it.pagopa.pn.address.manager.exception;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class PnSafeStorageExceptionTest {
    /**
     * Method under test: {@link PnSafeStorageException#PnSafeStorageException(WebClientResponseException)}
     */
    @Test
    void testConstructor() throws UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        WebClientResponseException ex = new WebClientResponseException(1, "Status Text", headers,
                "AXAXAXAX".getBytes("UTF-8"), null);

        PnSafeStorageException actualPnSafeStorageException = new PnSafeStorageException(ex);
        Throwable cause = actualPnSafeStorageException.getCause();
        assertSame(ex, cause);
        assertTrue(cause instanceof WebClientResponseException);
        Throwable[] suppressed = actualPnSafeStorageException.getSuppressed();
        assertEquals(0, suppressed.length);
        assertSame(ex, cause);
        assertEquals("org.springframework.web.reactive.function.client.WebClientResponseException: 1 Status Text",
                actualPnSafeStorageException.getMessage());
        assertEquals("org.springframework.web.reactive.function.client.WebClientResponseException: 1 Status Text",
                actualPnSafeStorageException.getLocalizedMessage());
        assertEquals("Status Text", ((WebClientResponseException) cause).getStatusText());
        assertNull(((WebClientResponseException) cause).getRootCause());
        assertEquals("AXAXAXAX", ((WebClientResponseException) cause).getResponseBodyAsString());
        byte[] expectedResponseBodyAsByteArray = "AXAXAXAX".getBytes("UTF-8");
        assertArrayEquals(expectedResponseBodyAsByteArray,
                ((WebClientResponseException) cause).getResponseBodyAsByteArray());
        assertNull(((WebClientResponseException) cause).getRequest());
        assertEquals(1, ((WebClientResponseException) cause).getRawStatusCode());
        assertSame(cause, ((WebClientResponseException) cause).getMostSpecificCause());
        assertEquals("1 Status Text", cause.getMessage());
        assertEquals("1 Status Text", cause.getLocalizedMessage());
        HttpHeaders headers2 = ((WebClientResponseException) cause).getHeaders();
        assertEquals(headers, headers2);
        assertTrue(headers2.isEmpty());
        assertNull(cause.getCause());
        assertSame(suppressed, cause.getSuppressed());
    }
}

