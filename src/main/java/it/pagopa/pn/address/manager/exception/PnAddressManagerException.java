package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import lombok.Getter;

@Getter
public class PnAddressManagerException extends PnRuntimeException {

    private final String code;

    public PnAddressManagerException(String message, int status, String code){
        super(message, message, status, code, null, null);
        this.code = code;
    }


}
