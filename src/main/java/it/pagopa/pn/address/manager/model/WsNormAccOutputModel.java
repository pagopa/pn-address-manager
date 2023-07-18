package it.pagopa.pn.address.manager.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @CsvBindByName(column = "SSIGLAPROV")
    //Sigla provincia normalizzata
    private String siglaProvincia;

    @CsvBindByName(column = "SSTATOUFF")
    //Stato normalizzato
    private String stato;

    @CsvBindByName(column = "SSTATOABB")
    //Stato abbreviato normalizzato
    private String statoAbbreviato;

    @CsvBindByName(column = "SCOMUNEUFF")
    //Comune normalizzato
    private String comune;

    @CsvBindByName(column = "SCOMUNEABB")
    //Comune Abbreviato normalizzato
    private String comuneAbbreviato;

    @CsvBindByName(column = "SFRAZIONEUFF")
    //Frazione normalizzata
    private String frazione;

    @CsvBindByName(column = "SFRAZIONEABB")
    //Frazione Abbreviata normalizzata
    private String frazioneAbbreviata;

    @CsvBindByName(column = "SLOCASPEDIZIONEUFF")
    //località di spedizione postale normalizzato
    private String localitaSpedizionePostale;

    @CsvBindByName(column = "SLOCASPEDIZIONEABB")
    //località di spedizione postale normalizzato abbreviata
    private String localitaSpedizionePostaleAbbreviata;

    @CsvBindByName(column = "SDUGUFF")
    //dug normalizzato
    private String dug;

    @CsvBindByName(column = "SDUGABB")
    //dug normalizzato abbreviata
    private String dugAbbreviata;

    @CsvBindByName(column = "SCOMPLUFF")
    //complemento a dug normalizzato
    private String complementoDug;

    @CsvBindByName(column = "SCOMUNEABB")
    //complemento a dug normalizzato
    private String complementoDugNormalizzato;

    @CsvBindByName(column = "STOPONIMOUFF")
    //Toponimo normalizzato
    private String toponimo;

    @CsvBindByName(column = "STOPONIMOABB")
    //Toponimo normalizzato abbreviato
    private String toponimoAbbreviato;

    @CsvBindByName(column = "SCIVICOPOSTALE")
    //civico in formato postale (solo numero civico + esponente)
    private String civicoPostale;

    @CsvBindByName(column = "SCIVICOALTRO")
    //altri elementi del civico (interno, piano, scala, palazzo ….)
    private String civicoInformazioni;

    @CsvBindByName(column = "SCAP")
    //cap normalizzato
    private String cap;

    @CsvBindByName(column = "SPRESSO")
    //informazioni di presso e casella postale (C.P 123, Presso sig. rossi ….)
    private String pressoCasellaPostale;

    @CsvBindByName(column = "SVIACOMPLETAUFF")
    //via completa normalizzata
    private String viaCompleta;

    @CsvBindByName(column = "SVIACOMPLETAABB")
    //via completa normalizzata abbreviata
    private String viaCompletaAbbreviata;
}
