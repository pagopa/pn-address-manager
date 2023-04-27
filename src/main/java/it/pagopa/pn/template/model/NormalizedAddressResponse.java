package it.pagopa.pn.template.model;

import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
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
}
