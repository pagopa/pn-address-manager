package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PostelBatchConstant {

    public static final String COL_FILE_KEY = "fileKey";
    public static final String COL_OUTPUT_FILE_KEY = "outputFileKey";

    public static final String COL_RETRY = "retry";
    public static final String COL_BATCH_ID = "batchId";

    public static final String COL_SHA256 = "sha256";
    public static final String COL_TTL = "ttl";
    public static final String COL_STATUS = "status";
    public static final String COL_LAST_RESERVED = "lastReserved";

    public static final String COL_WORKINGTTL = "workingTtl";
    public static final String COL_TIMESTAMP = "timeStamp";
    public static final String COL_CALLBACK_TIMESTAMP = "callbackTimeStamp";
    public static final String COL_ERROR = "error";

    public static final String GSI_S = "status-index";

    public static final String GSI_SWT = "status-workingTtl-index";
}
