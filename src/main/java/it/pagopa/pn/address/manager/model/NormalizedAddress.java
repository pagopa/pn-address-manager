package it.pagopa.pn.address.manager.model;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressOut;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data
@JsonPropertyOrder({
        AddressOut.JSON_PROPERTY_ID,
        AddressOut.JSON_PROPERTY_N_RISULTATO_NORM,
        AddressOut.JSON_PROPERTY_N_ERRORE_NORM,
        AddressOut.JSON_PROPERTY_S_SIGLA_PROV,
        AddressOut.JSON_PROPERTY_F_POSTALIZZABILE,
        AddressOut.JSON_PROPERTY_S_STATO_UFF,
        AddressOut.JSON_PROPERTY_S_STATO_ABB,
        AddressOut.JSON_PROPERTY_S_STATO_SPEDIZIONE,
        AddressOut.JSON_PROPERTY_S_COMUNE_UFF,
        AddressOut.JSON_PROPERTY_S_COMUNE_ABB,
        AddressOut.JSON_PROPERTY_S_COMUNE_SPEDIZIONE,
        AddressOut.JSON_PROPERTY_S_FRAZIONE_UFF,
        AddressOut.JSON_PROPERTY_S_FRAZIONE_ABB,
        AddressOut.JSON_PROPERTY_S_FRAZIONE_SPEDIZIONE,
        AddressOut.JSON_PROPERTY_S_CIVICO_ALTRO,
        AddressOut.JSON_PROPERTY_S_CAP,
        AddressOut.JSON_PROPERTY_S_PRESSO,
        AddressOut.JSON_PROPERTY_S_VIA_COMPLETA_UFF,
        AddressOut.JSON_PROPERTY_S_VIA_COMPLETA_ABB,
        AddressOut.JSON_PROPERTY_S_VIA_COMPLETA_SPEDIZIONE
})
@JsonTypeName("NormalizedAddress")
public class NormalizedAddress {

    public static final String JSON_PROPERTY_ID = "id";
    @CsvBindByPosition(position = 0)
    private String id;

    public static final String JSON_PROPERTY_N_RISULTATO_NORM = "nRisultatoNorm";
    @CsvBindByPosition(position = 1)
    private Integer nRisultatoNorm;

    public static final String JSON_PROPERTY_F_POSTALIZZABILE = "fPostalizzabile";
    @CsvBindByPosition(position = 2)
    private Integer fPostalizzabile;

    public static final String JSON_PROPERTY_N_ERRORE_NORM = "nErroreNorm";
    @CsvBindByPosition(position = 3)
    private Integer nErroreNorm;

    public static final String JSON_PROPERTY_S_SIGLA_PROV = "sSiglaProv";
    @CsvBindByPosition(position = 4)
    private String sSiglaProv;

    public static final String JSON_PROPERTY_S_STATO_UFF = "sStatoUff";
    @CsvBindByPosition(position = 5)
    private String sStatoUff;

    public static final String JSON_PROPERTY_S_STATO_ABB = "sStatoAbb";
    @CsvBindByPosition(position = 6)
    private String sStatoAbb;

    public static final String JSON_PROPERTY_S_STATO_SPEDIZIONE = "sStatoSpedizione";
    @CsvBindByPosition(position = 7)
    private String sStatoSpedizione;

    public static final String JSON_PROPERTY_S_COMUNE_UFF = "sComuneUff";
    @CsvBindByPosition(position = 8)
    private String sComuneUff;

    public static final String JSON_PROPERTY_S_COMUNE_ABB = "sComuneAbb";
    @CsvBindByPosition(position = 9)
    private String sComuneAbb;

    public static final String JSON_PROPERTY_S_COMUNE_SPEDIZIONE = "sComuneSpedizione";
    @CsvBindByPosition(position = 10)
    private String sComuneSpedizione;

    public static final String JSON_PROPERTY_S_FRAZIONE_UFF = "sFrazioneUff";
    @CsvBindByPosition(position = 11)
    private String sFrazioneUff;

    public static final String JSON_PROPERTY_S_FRAZIONE_ABB = "sFrazioneAbb";
    @CsvBindByPosition(position = 12)
    private String sFrazioneAbb;

    public static final String JSON_PROPERTY_S_FRAZIONE_SPEDIZIONE = "sFrazioneSpedizione";
    @CsvBindByPosition(position = 13)
    private String sFrazioneSpedizione;

    public static final String JSON_PROPERTY_S_CIVICO_ALTRO = "sCivicoAltro";
    @CsvBindByPosition(position = 14)
    private String sCivicoAltro;

    public static final String JSON_PROPERTY_S_CAP = "sCap";
    @CsvBindByPosition(position = 15)
    private String sCap;

    public static final String JSON_PROPERTY_S_PRESSO = "sPresso";
    @CsvBindByPosition(position = 16)
    private String sPresso;

    public static final String JSON_PROPERTY_S_VIA_COMPLETA_UFF = "sViaCompletaUff";
    @CsvBindByPosition(position = 17)
    private String sViaCompletaUff;

    public static final String JSON_PROPERTY_S_VIA_COMPLETA_ABB = "sViaCompletaAbb";
    @CsvBindByPosition(position = 18)
    private String sViaCompletaAbb;

    public static final String JSON_PROPERTY_S_VIA_COMPLETA_SPEDIZIONE = "sViaCompletaSpedizione";
    @CsvBindByPosition(position = 19)
    private String sViaCompletaSpedizione;
}
