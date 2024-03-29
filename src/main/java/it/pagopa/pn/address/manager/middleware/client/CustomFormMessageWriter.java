package it.pagopa.pn.address.manager.middleware.client;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;

import java.util.Objects;

public class CustomFormMessageWriter extends FormHttpMessageWriter {

    @Override
    protected @NotNull MediaType getMediaType(MediaType mediaType) {
        return Objects.requireNonNullElseGet(mediaType, () -> new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET));
    }

}
