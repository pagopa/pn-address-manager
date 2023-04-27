package it.pagopa.pn.template.model;

import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDetail {
    private NormalizeItemsResult body;
    private String cxId;
}
