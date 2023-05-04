package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchAddressConstant {

    //CONSTANT ADDRESS
    public static final String PK = "id";
    public static final String COL_ADDRESS_ID = "addressId";
    public static final String COL_CORRELATION_ID = "correlationId";
    public static final String COL_CX_ID = "cxId";
    public static final String COL_ADDRESS_ROW = "addressRow";
    public static final String COL_ADDRESS_ROW_2 = "addressRow2";
    public static final String COL_CAP = "cap";
    public static final String COL_CITY = "city";
    public static final String COL_CITY_2 = "city2";
    public static final String COL_PR = "pr";
    public static final String COL_COUNTRY = "country";

    //CONSTANT BATCH
    public static final String COL_BATCH_ID = "batchId";
    public static final String COL_RETRY = "retry";
    public static final String COL_TTL = "ttl";
    public static final String COL_STATUS = "status";
    public static final String COL_LAST_RESERVED = "lastReserved";
    public static final String COL_RESERVATION_ID = "reservationId";
    public static final String COL_TIMESTAMP = "timeStamp";
    public static final String COL_SEND_STATUS = "sendStatus";
    public static final String COL_MESSAGE = "message";

    //CONSTANT INDEX
    public static final String GSI_BL = "batchId-lastReserved-index";
    public static final String GSI_S = "status-index";
    public static final String GSI_SSL = "sendStatus-lastReserved-index";
}
