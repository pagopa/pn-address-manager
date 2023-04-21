package it.pagopa.pn.template.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PnAddressManagerExceptionTest {
    /**
     * Method under test: {@link PnAddressManagerException#PnAddressManagerException(String, HttpStatus)}
     */
    @Test
    void testConstructor() {
        PnAddressManagerException actualPnAddressManagerException = new PnAddressManagerException("An error occurred",
                HttpStatus.CONTINUE);

        assertNull(actualPnAddressManagerException.getCause());
        assertEquals(0, actualPnAddressManagerException.getSuppressed().length);
        assertEquals(HttpStatus.CONTINUE, actualPnAddressManagerException.getStatus());
        assertEquals("An error occurred", actualPnAddressManagerException.getMessage());
        assertEquals("An error occurred", actualPnAddressManagerException.getLocalizedMessage());
    }
}

