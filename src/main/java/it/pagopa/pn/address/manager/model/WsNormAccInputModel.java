package it.pagopa.pn.address.manager.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsNormAccInputModel {

    @CsvBindByName(column = "IdCodiceCliente")
    //id del cliente
    private String idCodiceCliente;

    @CsvBindByName(column = "Provincia")
    //Sigla Provincia
    private String provincia;

    @CsvBindByName(column = "Cap")
    //cap
    private String cap;

    @CsvBindByName(column = "localita", required = true)
    //ocalità/comune
    private String localita;

    @CsvBindByName(column = "localitaAggiuntiva")
    //frazione
    private String localitaAggiuntiva;

    @CsvBindByName(column = "dug")
    //dug
    private String dug;

    @CsvBindByName(column = "indirizzo", required = true)
    //svia
    private String indirizzo;

    @CsvBindByName(column = "civico")
    //scivico
    private String civico;
}
