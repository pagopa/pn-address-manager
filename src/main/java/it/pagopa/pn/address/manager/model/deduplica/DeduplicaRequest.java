package it.pagopa.pn.address.manager.model.deduplica;

import lombok.Data;
import lombok.ToString;

@Data
public class DeduplicaRequest {

    private String idSlave;

    @ToString.Exclude
    private String provinciaSlave;

    @ToString.Exclude
    private String capSlave;

    @ToString.Exclude
    private String localitaSlave;

    @ToString.Exclude
    private String localitaAggiuntivaSlave;

    @ToString.Exclude
    private String dugSlave;

    @ToString.Exclude
    private String indirizzoSlave;

    @ToString.Exclude
    private String civicoSlave;

    private String idMaster;

    @ToString.Exclude
    private String provinciaMaster;

    @ToString.Exclude
    private String capMaster;

    @ToString.Exclude
    private String localitaMaster;

    @ToString.Exclude
    private String localitaAggiuntivaMaster;

    @ToString.Exclude
    private String dugMaster;

    @ToString.Exclude
    private String indirizzoMaster;

    @ToString.Exclude
    private String civicoMaster;

    @ToString.Exclude
    private String authKey;

    private String configurazioneDeduplica;

    private String configurazioneNormalizzazione;

}
