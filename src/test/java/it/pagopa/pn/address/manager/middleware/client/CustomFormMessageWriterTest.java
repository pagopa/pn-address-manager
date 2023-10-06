package it.pagopa.pn.address.manager.middleware.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CustomFormMessageWriterTest {

    @Test
    void testGetMediaType() {
        CustomFormMessageWriter customFormMessageWriter = new CustomFormMessageWriter();
        MediaType mediaType = new MediaType("Type");
        assertSame(mediaType, customFormMessageWriter.getMediaType(mediaType));
    }
}

