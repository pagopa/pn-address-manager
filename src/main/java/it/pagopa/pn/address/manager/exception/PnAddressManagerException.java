package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;

public class PnAddressManagerException extends PnRuntimeException {

    @Getter
    private final String description;

    public PnAddressManagerException(String message, String description, int statusCode, String errorCode){
        super(message, description, statusCode, errorCode, null, null);
        this.description = description;
    }
}
