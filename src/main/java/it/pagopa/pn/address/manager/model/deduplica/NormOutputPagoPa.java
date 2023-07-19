package it.pagopa.pn.address.manager.model.deduplica;

import lombok.Data;
import lombok.ToString;

@Data
public class NormOutputPagoPa {

    private String idSlave;

    private int flagNormalizzatoSlave;

    private int nErroreSlave;

    private String idMaster;

    private int flagNormalizzatoMaster;

    private int nErroreMaster;

    private int risultatoDedu;

    private int erroreDedu;

    @ToString.Exclude
    private String sSiglaProvincia;

    @ToString.Exclude
    private String sStatoUfficiale;

    @ToString.Exclude
    private String sStatoAbbreviato;

    @ToString.Exclude
    private String sComuneUfficiale;

    @ToString.Exclude
    private String sComuneAbbreviato;

    @ToString.Exclude
    private String sFrazioneUfficiale;

    @ToString.Exclude
    private String sFrazioneAbbreviata;

    @ToString.Exclude
    private String sLocalitaSpedizioneUfficiale;

    @ToString.Exclude
    private String sLocalitaSpedizioneAbbreviata;

    @ToString.Exclude
    private String sDUGUfficiale;

    @ToString.Exclude
    private String sDUGAbbreviata;

    @ToString.Exclude
    private String sComplementoDUGUfficiale;

    @ToString.Exclude
    private String sComplementoDUGAbbreviato;

    @ToString.Exclude
    private String sToponimoUfficiale;

    @ToString.Exclude
    private String sToponimoAbbreviato;

    @ToString.Exclude
    private String sCivicoPostale;

    @ToString.Exclude
    private String sCivicoAltro;

    @ToString.Exclude
    private String sCAP;

    @ToString.Exclude
    private String sPresso;

    @ToString.Exclude
    private String sViaCompletaUfficiale;

    @ToString.Exclude
    private String sViaCompletaAbbreviata;

}
