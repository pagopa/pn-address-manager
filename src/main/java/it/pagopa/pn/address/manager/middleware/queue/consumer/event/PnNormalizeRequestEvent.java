package it.pagopa.pn.address.manager.middleware.queue.consumer.event;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

import javax.validation.constraints.NotEmpty;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PnNormalizeRequestEvent implements GenericEvent<StandardEventHeader, PnNormalizeRequestEvent.Payload> {

    private StandardEventHeader header;

    private Payload payload;

    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        @NotEmpty
        private NormalizeItemsRequest normalizeItemsRequest;

        @NotEmpty
        private String pnAddressManagerCxId;

        @NotEmpty
        private String xApiKey;
    }
}
