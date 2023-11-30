package it.pagopa.pn.address.manager.model;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class InternalCodeSqsDto {
    private NormalizeItemsRequest normalizeItemsRequest;
    private String pnAddressManagerCxId;
}
