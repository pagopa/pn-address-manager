package it.pagopa.pn.address.manager.model;

import it.pagopa.pn.address.manager.rest.v1.dto.NormalizeItemsResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NormalizeItemsResultModel {

    private String cxId;
    private NormalizeItemsResult normalizeItemsResult;

    public NormalizeItemsResultModel(String cxId, NormalizeItemsResult normalizeItemsResult){
        this.cxId = cxId;
        this.normalizeItemsResult = normalizeItemsResult;
    }
}
