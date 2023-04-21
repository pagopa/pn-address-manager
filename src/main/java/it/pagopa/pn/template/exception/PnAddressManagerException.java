package it.pagopa.pn.template.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class PnAddressManagerException extends RuntimeException {

    @Getter
    private final HttpStatus status;

    public PnAddressManagerException(String message, HttpStatus status){
        super(message);
        this.status = status;
    }
}
