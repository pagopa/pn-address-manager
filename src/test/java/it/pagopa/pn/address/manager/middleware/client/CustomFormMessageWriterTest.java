package it.pagopa.pn.address.manager.middleware.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertSame;

class CustomFormMessageWriterTest {

    @Test
    void testGetMediaType() {
        CustomFormMessageWriter customFormMessageWriter = new CustomFormMessageWriter();
        MediaType mediaType = new MediaType("Type");
        assertSame(mediaType, customFormMessageWriter.getMediaType(mediaType));
    }
}

