package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.Getter;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESSMANAGER_FILE_NOT_FOUND;

@Getter
public class PnFileNotFoundException extends PnInternalException {

    public PnFileNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE_ADDRESSMANAGER_FILE_NOT_FOUND, cause);
    }

}
