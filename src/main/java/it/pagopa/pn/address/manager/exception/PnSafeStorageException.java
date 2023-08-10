package it.pagopa.pn.address.manager.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class PnSafeStorageException extends RuntimeException{

    public PnSafeStorageException(WebClientResponseException ex) {
        super(ex);
    }
}
