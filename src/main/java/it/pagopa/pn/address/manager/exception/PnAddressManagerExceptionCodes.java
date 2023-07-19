package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PnAddressManagerExceptionCodes extends PnExceptionsCodes {
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV = "Error writing CSV";
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION = "An error occurred while writing the CSV file";
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE = "CSV_WRITE_ERROR";

    public static final String ERROR_ADDRESS_MANAGER_READING_CSV = "Error reading CSV";
    public static final String ERROR_ADDRESS_MANAGER_READING_CSV_DESCRIPTION = "An error occurred while reading the CSV file";
    public static final String ERROR_ADDRESS_MANAGER_READING_CSV_ERROR_CODE = "CSV_READ_ERROR";

    public static final String ERROR_ADDRESS_MANAGER_VERIFY_CSV = "Error verify CSV";
    public static final String ERROR_ADDRESS_MANAGER_VERIFY_CSV_DESCRIPTION = "An error occurred while verify the CSV file";
    public static final String ERROR_ADDRESS_MANAGER_VERIFY_CSV_ERROR_CODE = "CSV_VERIFY_ERROR";

    public static final String ERROR_ADDRESS_MANAGER_DURING_VERIFY_CSV = "Error during verify csv";
    public static final String ERROR_ADDRESS_MANAGER_DURING_CAP_MANDATORY_DESCRIPTION = "CAP is mandatory";
    public static final String ERROR_ADDRESS_MANAGER_DURING_CAP_NOT_FOUND_DESCRIPTION = "Cap %s not found";
    public static final String ERROR_ADDRESS_MANAGER_DURING_COUNTRY_NOT_FOUND_DESCRIPTION = "Country %s not found";
    public static final String ERROR_ADDRESS_MANAGER_CAP_NOT_FOUND_ERROR = "CAP_NOT_FOUND";
    public static final String ERROR_ADDRESS_MANAGER_COUNTRY_NOT_FOUND_ERROR = "COUNTRY_NOT_FOUND";

    public static final String ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE = "DEDUPLICA_ONLINE_ERROR";
    public static final String ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI = "POSTEL_ATTIVAZIONE_SINI_ERROR";


}
