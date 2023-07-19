package it.pagopa.pn.address.manager.model.deduplica;

import lombok.Data;

@Data
public class DeduplicaResponse {

    private int numeroRecords;
    private int rowFetched;
    private String errorMessage;
    private int errorCode;
    private String result;
    private boolean isErrore = false;
    private long nextValue;

}
