package it.pagopa.pn.address.manager.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProcessStatus {

    public static final String PROCESS_VERIFY_ADDRESS = "[VERIFY] verify address";
    public static final String PROCESS_SERVICE_DEDUPLICA_ONLINE = "[DEDUPLICA] verify address slave and master";
    public static final String PROCESS_SERVICE_POSTEL_ATTIVAZIONE_SINI = "[ACTIVATE_SINI_COMPONENT] activate SINI component";
    public static final String PROCESS_CHECKING_POSTEL_ATTIVAZIONE_SINI = "[ACTIVATE_SINI_COMPONENT] checking response";
}
