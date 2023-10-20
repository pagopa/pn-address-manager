package it.pagopa.pn.address.manager.constant;

public class AddressmanagerConstant {
	//NormalizeAddressService
	public static final String ADDRESS_NORMALIZER_SYNC = "ADDRESS NORMALIZER SYNC - ";

    //NormalizeAddressService,HandleEventUtils,AddressBatchRequestService
    public static final String ADDRESS_NORMALIZER_ASYNC = "ADDRESS NORMALIZER ASYNC - ";

	//NormalizzatoreConverter
	public static final String PN_ADDRESSES_NORMALIZED = "PN_ADDRESSES_NORMALIZED";

	//NormalizzatoreConverter
	public static final String SAVED = "SAVED";

	//NormalizzatoreConverter, SafeStorageService
	public static final String SAFE_STORAGE_URL_PREFIX = "safestorage://";

	//SafeStorageClient
	public static final String SHA256 = "SHA-256";

	//PostelClient
	public static final String POSTEL = "POSTEL";

	//NormalizeAddressService
	public static final String AM_NORMALIZE_INPUT_EVENTTYPE = "AM_NORMALIZE_INPUT";

	//NormalizzatoreService
	public static final String AM_POSTEL_CALLBACK_EVENTTYPE = "AM_POSTEL_CALLBACK";

	//AddressUtils
	public static final String CONTENT_TYPE = "text/csv";

	//AddressUtils
	public static final String SAFE_STORAGE_STATUS = "SAVED";

	//AddressUtils
	public static final String DOCUMENT_TYPE = "PN_ADDRESSES_RAW";

	//PnWebExceptionHandler
	public static final String SYNTAX_ERROR = "Syntax error";
	public static final String SEMANTIC_ERROR = "Semantic error";
	public static final String SYNTAX_ERROR_CODE = "400.01";
	public static final String SEMANTIC_ERROR_CODE = "400.02";
	public static final String PNADDR001 = "PNADDR001";
	public static final String PNADDR002 = "PNADDR002";
	public static final String PNADDR999 = "PNADDR999";
	public static final String PNADDR001_MESSAGE = "Address declared non-mailable by normalizer";
	public static final String PNADDR002_MESSAGE = "Address with not Enabled POSTAL CODE/ foreign STATE as destination";
	public static final String PNADDR999_MESSAGE = "Normalization service error";
	public static final String RD01 = "RD01";
	public static final String RD02 = "RD02";
	public static final String RD03 = "RD03";
	public static final String RD01_MESSAGE = "Process not complete due to errors during the base address normalization phase";
	public static final String RD02_MESSAGE = "Process not complete due to errors during the target address normalization phase";
	public static final String RD03_MESSAGE = "Process not complete due to errors during both addresses normalization phases";
}
