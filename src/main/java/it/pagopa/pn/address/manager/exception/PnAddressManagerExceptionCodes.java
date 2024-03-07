package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PnAddressManagerExceptionCodes extends PnExceptionsCodes {
    public static  final String ERROR_CODE_ADDRESS_MANAGER_CSVERROR = "PN_ADDRESS_MANAGER_CSVERROR";
    public static final String ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND = "PN_ADDRESS_MANAGER_CAPNOTFOUND";
    public static final String PN_ADDRESS_MANAGER_INVALID_CHARACTERS = "PN_ADDRESS_MANAGER_INVALID_CHARACTERS";

    public static final String ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND = "PN_ADDRESS_MANAGER_COUNTRYNOTFOUND";
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV_ERROR_CODE = "CSV_WRITING_ERROR";
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV = "Error writing CSV";
    public static final String ERROR_ADDRESS_MANAGER_WRITING_CSV_DESCRIPTION = "An error occurred while writing the CSV file";
    public static final String ERROR_ADDRESS_MANAGER_READING_CSV_ERROR_CODE = "CSV_READING_ERROR";
    public static final String ERROR_ADDRESS_MANAGER_READING_CSV = "Error reading CSV";
    public static final String ERROR_ADDRESS_MANAGER_READING_CSV_DESCRIPTION = "An error occurred while reading the CSV file";
    public static final String ERROR_ADDRESS_MANAGER_DEDUPLICA_ONLINE_ERROR_CODE = "DEDUPLICA_ONLINE_ERROR";

    public static final String APIKEY_DOES_NOT_EXISTS = "ApiKey does not exist";

    public static final String ERROR_CLIENT_ID_MESSAGE = "ClientId does not exist";

    public static final String ERROR_CLIENT_ID = "CLIENTID_NOT_FOUND";
    public static final String CAP_DOES_NOT_EXISTS = "Cap %s does not exist or is not valid";
    public static final String COUNTRY_DOES_NOT_EXISTS = "Country %s does not exist";

    public static final String ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_CODE = "CSV_UPLOAD_FAILED";
    public static final String ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_CODE = "CSV_DOWNLOAD_FAILED";

    public static final String ERROR_ADDRESS_MANAGER_CSV_UPLOAD_FAILED_ERROR_DESCRIPTION = "Failed to upload the csv to the Safe Storage presigned uri";
    public static final String ERROR_ADDRESS_MANAGER_CSV_DOWNLOAD_FAILED_ERROR_DESCRIPTION = "Failed to download the csv from the Safe Storage presigned uri";

    public static final String ERROR_CODE_POSTEL_CLIENT = "ERROR_POSTEL_CLIENT";
    public static final String ERROR_MESSAGE_POSTEL_CLIENT = "There was an error on the Postel client";

    public static final String ERROR_CODE_ADDRESS_MANAGER_HANDLEEVENTFAILED = "ERROR_ADDRESS_MANAGER_HANDLEEVENTFAILED";
    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_HANDLEEVENTFAILED = "Error during handle normalize request";

    public static final String ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL = "ERROR_ADDRESS_MANAGER_NORMALIZEDADDRESSEMPTY";
    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL = "Normalized Address from postel is empty";

    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_POSTELBATCHNOTFOUND = "Postel batch for requestId %s does not exist";

    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_POSTELOUTPUTFILEKEYNOTFOUND = "fileKey %s does not exist";

    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_POSTELINVALIDCHECKSUM = "Checksum for fileKey %s is invalid";

    public static final String ERROR_CODE_ADDRESS_MANAGER_NORMALIZE_ADDRESS = "ERROR_ADDRESS_MANAGER_NORMALIZEDADDRESS";
    public static final String ERROR_MESSAGE_ADDRESS_MANAGER_NORMALIZE_ADDRESS = "Error during processing request";

    public static final String ERROR_CODE_ADDRESSMANAGER_FILE_NOT_FOUND = "PN_ADDRESSMANAGER_FILE_NOT_FOUND";

    public static final String ERROR_CODE_ADDRESSMANAGER_BATCHREQUEST = "PN_ADDRESSMANAGER_GETBATCHREQUEST_ERROR";
    public static final String ERROR_MESSAGE_ADDRESSMANAGER_BATCHREQUEST = "can not get batch request";

    public static final String INVALID_ADDRESS_FIELD_LENGTH = "At least on Address field has an invalid length";
    public static final String INVALID_ADDRESS_FIELD_LENGTH_CODE = "INVALID_ADDRESS_FIELD_LENGTH";
    public static final String ERROR_MESSAGE_FOREIGN_COUNTRY_ADDRESS_NOT_VALIDATED_WITH_PATTERN = "Error while validating a foreign country address through the pattern.";
    public static final String ERROR_CODE_FOREIGN_COUNTRY_ADDRESS_NOT_VALIDATED_WITH_PATTERN = "FOREIGN_COUNTRY_ADDRESS_NOT_VALIDATED_WITH_PATTERN";

}
