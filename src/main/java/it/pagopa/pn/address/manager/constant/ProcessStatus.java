package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessStatus {
    public static final String PROCESS_NAME_DEDUPLICATES_ADDRESS_DEDUPLICATES = "[DEDUPLICATE ADDRESS] deduplicates";

    public static final String PROCESS_NAME_NORMALIZE_ADDRESS_NORMALIZE = "[NORMALIZE ADDRESS] normalize";

    public static final String PROCESS_VERIFY_ADDRESS = "[VERIFY] verify address";
}
