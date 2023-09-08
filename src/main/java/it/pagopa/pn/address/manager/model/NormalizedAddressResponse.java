package it.pagopa.pn.address.manager.model;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedAddressResponse {
    private String id;
    private String error;
    private AnalogAddress normalizedAddress;
    private boolean isItalian;
}
