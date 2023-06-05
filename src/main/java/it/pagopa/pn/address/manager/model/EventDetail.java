package it.pagopa.pn.address.manager.model;

import it.pagopa.pn.address.manager.server.v1.dto.NormalizeItemsResult;
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
