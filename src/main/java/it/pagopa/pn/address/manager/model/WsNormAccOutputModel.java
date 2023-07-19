package it.pagopa.pn.address.manager.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsNormAccOutputModel {

    @CsvBindByName(column = "IdCodiceCliente")
    //id del cliente
    private String idCodiceCliente;

    @CsvBindByName(column = "NrisultatoNorm")
    //Risultato di normalizzazione (0  scartato/ 1  normalizzato)
    private String risultato;

    @CsvBindByName(column = "nErroreNorm")
    //Codice di errore ( valorizzato solo se nrisultatoNorm =0 )
    private String codiceErrore;

    @ToString.Exclude
    @CsvBindByName(column = "SSIGLAPROV")
    //Sigla provincia normalizzata
    private String siglaProvincia;

    @ToString.Exclude
    @CsvBindByName(column = "SSTATOUFF")
    //Stato normalizzato
    private String stato;

    @ToString.Exclude
    @CsvBindByName(column = "SSTATOABB")
    //Stato abbreviato normalizzato
    private String statoAbbreviato;

    @ToString.Exclude
    @CsvBindByName(column = "SCOMUNEUFF")
    //Comune normalizzato
    private String comune;

    @ToString.Exclude
    @CsvBindByName(column = "SCOMUNEABB")
    //Comune Abbreviato normalizzato
    private String comuneAbbreviato;

    @ToString.Exclude
    @CsvBindByName(column = "SFRAZIONEUFF")
    //Frazione normalizzata
    private String frazione;

    @ToString.Exclude
    @CsvBindByName(column = "SFRAZIONEABB")
    //Frazione Abbreviata normalizzata
    private String frazioneAbbreviata;

    @ToString.Exclude
    @CsvBindByName(column = "SLOCASPEDIZIONEUFF")
    //località di spedizione postale normalizzato
    private String localitaSpedizionePostale;

    @ToString.Exclude
    @CsvBindByName(column = "SLOCASPEDIZIONEABB")
    //località di spedizione postale normalizzato abbreviata
    private String localitaSpedizionePostaleAbbreviata;

    @ToString.Exclude
    @CsvBindByName(column = "SDUGUFF")
    //dug normalizzato
    private String dug;

    @ToString.Exclude
    @CsvBindByName(column = "SDUGABB")
    //dug normalizzato abbreviata
    private String dugAbbreviata;

    @ToString.Exclude
    @CsvBindByName(column = "SCOMPLUFF")
    //complemento a dug normalizzato
    private String complementoDug;

    @ToString.Exclude
    @CsvBindByName(column = "SCOMUNEABB")
    //complemento a dug normalizzato
    private String complementoDugNormalizzato;

    @ToString.Exclude
    @CsvBindByName(column = "STOPONIMOUFF")
    //Toponimo normalizzato
    private String toponimo;

    @ToString.Exclude
    @CsvBindByName(column = "STOPONIMOABB")
    //Toponimo normalizzato abbreviato
    private String toponimoAbbreviato;

    @ToString.Exclude
    @CsvBindByName(column = "SCIVICOPOSTALE")
    //civico in formato postale (solo numero civico + esponente)
    private String civicoPostale;

    @ToString.Exclude
    @CsvBindByName(column = "SCIVICOALTRO")
    //altri elementi del civico (interno, piano, scala, palazzo ….)
    private String civicoInformazioni;

    @ToString.Exclude
    @CsvBindByName(column = "SCAP")
    //cap normalizzato
    private String cap;

    @ToString.Exclude
    @CsvBindByName(column = "SPRESSO")
    //informazioni di presso e casella postale (C.P 123, Presso sig. rossi ….)
    private String pressoCasellaPostale;

    @ToString.Exclude
    @CsvBindByName(column = "SVIACOMPLETAUFF")
    //via completa normalizzata
    private String viaCompleta;

    @ToString.Exclude
    @CsvBindByName(column = "SVIACOMPLETAABB")
    //via completa normalizzata abbreviata
    private String viaCompletaAbbreviata;
}
