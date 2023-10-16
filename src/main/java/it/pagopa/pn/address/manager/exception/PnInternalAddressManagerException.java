package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;

@Getter
public class PnInternalAddressManagerException extends PnRuntimeException {

    private final String description;

    public PnInternalAddressManagerException(String message, String description, int status, String errorCode){
        super(message, description, status, errorCode, null, null);
        this.description = description;
    }


}
