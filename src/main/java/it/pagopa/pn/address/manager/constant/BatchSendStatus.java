package it.pagopa.pn.address.manager.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BatchSendStatus {
    NOT_SENT("NOT_SENT"),
    SENT("SENT"),
    ERROR("ERROR"),
    SENT_TO_DLQ("SENT_TO_DLQ");

    private final String value;

    BatchSendStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static BatchSendStatus fromValue(String value) {
        for (BatchSendStatus b : BatchSendStatus.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
