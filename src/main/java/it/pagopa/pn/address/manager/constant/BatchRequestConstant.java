package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BatchRequestConstant {

    public static final String PK = "correlationId";

    public static final String COL_ADDRESSES = "addresses";
    public static final String COL_BATCH_ID = "batchId";
    public static final String COL_RETRY = "retry";
    public static final String COL_TTL = "ttl";
    public static final String COL_CLIENT_ID = "clientId";
    public static final String COL_STATUS = "status";
    public static final String COL_LAST_RESERVED = "lastReserved";
    public static final String COL_RESERVATION_ID = "reservationId";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_SEND_STATUS = "sendStatus";
    public static final String COL_MESSAGE = "message";
    public static final String COL_XAPIKEY = "xApiKey";
    public static final String COL_CXID = "cxId";
    public static final String COL_AWS_MESSAGE_ID = "aws-messageId";

    public static final String GSI_BL = "batchId-lastReserved-index";
    public static final String GSI_S = "status-index";
    public static final String GSI_SSL = "sendStatus-lastReserved-index";
}
