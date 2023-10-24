package it.pagopa.pn.address.manager.constant;

import lombok.Getter;

@Getter
public enum PostelDeduError {
    DED400("BAD REQUEST.Input errato"),
    DED401("UNAUTHORIZED. Credenziali vuote o errate"),
    DED404("NOT FOUND. Il servizio nel server Postel non è disponibile"),
    DED500("INTERNAL_SERVER_ERROR. Il servizio nei server Postel non è raggiungibile"),
    DED998("SERVIZIO DI NORMALIZZAZIONE NON DISPONIBILE"),
    DED997("SERVIZIO ORACLE NON DISPONIBILE"),
    DED992("ERRORE_GENERICO"),
    DED003("ENTRAMBI SCARTATI"),
    DED001("INDIRIZZO MASTER SCARTATO"),
    DED002("INDIRIZZO SLAVE SCARTATO");

    private final String descrizione;

    PostelDeduError(String descrizione) {
        this.descrizione = descrizione;
    }

}
