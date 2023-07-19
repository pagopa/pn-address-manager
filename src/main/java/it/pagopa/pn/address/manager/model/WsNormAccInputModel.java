package it.pagopa.pn.address.manager.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsNormAccInputModel {

    @CsvBindByName(column = "IdCodiceCliente")
    //id del cliente
    private String idCodiceCliente;

    @ToString.Exclude
    @CsvBindByName(column = "Provincia")
    //Sigla Provincia
    private String provincia;

    @ToString.Exclude
    @CsvBindByName(column = "Cap")
    //cap
    private String cap;

    @ToString.Exclude
    @CsvBindByName(column = "localita", required = true)
    //ocalità/comune
    private String localita;

    @ToString.Exclude
    @CsvBindByName(column = "localitaAggiuntiva")
    //frazione
    private String localitaAggiuntiva;

    @ToString.Exclude
    @CsvBindByName(column = "dug")
    //dug
    private String dug;

    @ToString.Exclude
    @CsvBindByName(column = "indirizzo", required = true)
    //svia
    private String indirizzo;

    @ToString.Exclude
    @CsvBindByName(column = "civico")
    //scivico
    private String civico;
}
