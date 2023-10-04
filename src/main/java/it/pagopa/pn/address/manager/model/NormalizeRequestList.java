package it.pagopa.pn.address.manager.model;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import lombok.Data;

import java.util.List;

@Data
public class NormalizeRequestList {
    private List<NormalizeRequest> normalizeRequests;
}
