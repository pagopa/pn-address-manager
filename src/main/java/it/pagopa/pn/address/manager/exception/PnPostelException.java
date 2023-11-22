package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PnPostelException extends PnRuntimeException {
    private final String error;

    public PnPostelException(String message, String error){
        this(message, error, null);
    }

    public PnPostelException(String message, String error, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), message, HttpStatus.INTERNAL_SERVER_ERROR.value(), error, null, null, cause);
        this.error = error;
    }
}
