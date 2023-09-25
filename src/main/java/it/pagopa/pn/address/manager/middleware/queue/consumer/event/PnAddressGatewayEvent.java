package it.pagopa.pn.address.manager.middleware.queue.consumer.event;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import java.util.Date;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PnAddressGatewayEvent implements GenericEvent<StandardEventHeader, PnAddressGatewayEvent.Payload> {

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
