package it.pagopa.pn.address.manager.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PostelExceptionTest {
    /**
     * Method under test: {@link PostelException#PostelException(String)}
     */
    @Test
    void testConstructor() {
        PostelException actualPostelException = new PostelException("foo");
        assertNull(actualPostelException.getCause());
        assertEquals(0, actualPostelException.getSuppressed().length);
        assertEquals("foo", actualPostelException.getMessage());
        assertEquals("foo", actualPostelException.getLocalizedMessage());
    }
}

