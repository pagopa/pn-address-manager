package it.pagopa.pn.address.manager.constant;

public enum ExternalDeduplicatesError {
    DED400("BAD REQUEST. Mi passa un input errato sia dal punto di vista del contenuto o formale"),
    DED401("UNAUTHORIZED. Mi passa delle credenziali vuote o errate"),
    DED404("NOT FOUND. Il servizio nel server Postel non è disponibile"),
    DED500("INTERNAL_SERVER_ERROR. Il servizio nei server Postel non è raggiungibile"),
    DED998("SERVIZIO DI NORMALIZZAZIONE NON DISPONIBILE"),
    DED997("SERVIZIO ORACLE NON DISPONIBILE"),
    DED992("ERRORE_GENERICO"),
    DED003("ENTRAMBI SCARTATI"),
    DED001("INDIRIZZO MASTER SCARTATO"),
    DED002("INDIRIZZO SLAVE SCARTATO");

    private final String descrizione;

    ExternalDeduplicatesError(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }
}
