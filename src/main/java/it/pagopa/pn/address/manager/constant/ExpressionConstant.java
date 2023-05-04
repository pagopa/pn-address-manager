package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExpressionConstant {

    public static final String STATUS_ALIAS = "#status";
    public static final String STATUS_PLACEHOLDER = ":status";
    public static final String STATUS_EQ = STATUS_ALIAS + " = " + STATUS_PLACEHOLDER;

    public static final String BATCH_ALIAS = "#batchId";
    public static final String BATCH_PLACEHOLDER = ":batchId";
    public static final String BATCH_EQ = BATCH_ALIAS + " = " + BATCH_PLACEHOLDER;
}
