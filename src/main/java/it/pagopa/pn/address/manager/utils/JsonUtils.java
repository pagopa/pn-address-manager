package it.pagopa.pn.address.manager.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T fromJson(String json, Class<T> destinationClass) {
        try {
            return objectMapper.readValue(json, destinationClass);
        } catch (Exception e) {
            throw new PnAddressManagerException(e.getMessage(), "Unable to deserialize object", 500, ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    public static <T> String writeValueAsString(Object value){
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new PnAddressManagerException(e.getMessage(), "Unable to serialize any Java value as a String.", 500, ERROR_CODE_PN_GENERIC_ERROR);
        }
    }
}
