package it.pagopa.pn.address.manager.middleware.queue.consumer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.MessageHeaders;

class HandleEventUtilsTest {
    /**
     * Method under test: {@link HandleEventUtils#handleException(MessageHeaders, Throwable)}
     */
    @Test
    void testHandleException() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenReturn(null);
        HandleEventUtils.handleException(headers, new Throwable());
        verify(headers, atLeast(1)).get(Mockito.<Object>any());
    }

    /**
     * Method under test: {@link HandleEventUtils#handleException(MessageHeaders, Throwable)}
     */
    @Test
    void testHandleException2() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenThrow(new PnInternalException("An error occurred"));
        assertThrows(PnInternalException.class, () -> HandleEventUtils.handleException(headers, new Throwable()));
        verify(headers).get(Mockito.<Object>any());
    }

    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader() {
        assertThrows(PnInternalException.class, () -> HandleEventUtils.mapStandardEventHeader(null));
    }

    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader2() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenReturn(null);
        StandardEventHeader actualMapStandardEventHeaderResult = HandleEventUtils.mapStandardEventHeader(headers);
        assertNull(actualMapStandardEventHeaderResult.getCreatedAt());
        assertNull(actualMapStandardEventHeaderResult.getPublisher());
        assertNull(actualMapStandardEventHeaderResult.getIun());
        assertNull(actualMapStandardEventHeaderResult.getEventType());
        assertNull(actualMapStandardEventHeaderResult.getEventId());
        verify(headers, atLeast(1)).get(Mockito.<Object>any());
    }

    /**
     * Method under test: {@link HandleEventUtils#mapStandardEventHeader(MessageHeaders)}
     */
    @Test
    void testMapStandardEventHeader3() {
        MessageHeaders headers = mock(MessageHeaders.class);
        when(headers.get(Mockito.<Object>any())).thenThrow(new PnInternalException("An error occurred"));
        assertThrows(PnInternalException.class, () -> HandleEventUtils.mapStandardEventHeader(headers));
        verify(headers).get(Mockito.<Object>any());
    }
}

