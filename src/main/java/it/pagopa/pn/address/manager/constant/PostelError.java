package it.pagopa.pn.address.manager.constant;

public enum PostelError {
        E001("001", "CAP ASSENTE", "IL CAP NON E' PRESENTE IN INPUT"),
        E002("002", "PROVINCIA ASSENTE", "LA PROVINCIA NON E' PRESENTE IN INPUT"),
        E003("003", "CAP ERRATO SU LOCA", "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELLA LOCALITA"),
        E004("004", "CAP ERRATO SU VIA", "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELLA VIA"),
        E005("005", "CAP ERRATO SU ARCO DI CAP", "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELL'ARCO DI CAP DELLA VIA"),
        E006("006", "PROVINCIA ERRATA", "LA SIGLA PROVINCIA IN INPUT RISULTA DIVERSA DA QUELLA DETERMINATA"),
        E007("007", "CAP NON TROVATO", "IL CAP NON E' PRESENTE NEL RANGE DI CAP CONSENTITI"),
        E008("008", "PROVINCIA NON TROVATA", "LA PROVINCIA NON E' PRESENTE SULLA TABELLA PROVINCIE"),
        E009("009", "PROV/CAP INCONGRUENTI", "LA PROVINCIA E IL CAP INDIVIDUANO DUE PROVINCIE DIVERSE"),
        E019("019", "PROV/CAP INVALIDI", "I DATI DI PROVINCE E/O CAP NON SONO VALIDI"),
        E101("101", "LOCALITA' ASSENTE", "LA LOCALITA' NON E' PRESENTE IN INPUT"),
        E102("102", "LOCALITA' NON TROVATA", "NON E' STATO TROVATO NESSUN CANDIDATO PER LA LOCALITA"),
        E103("103", "LOCALITA' ERRATA", "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA LOCALITA', MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATO"),
        E104("104", "LOCALITA' NON UNIVOCA", "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA LOCALITA"),
        E105("105", "LOCALITA NON PUBBLICATA", "LA LOCALITA CANDIDATA NON E' PUBBLICATA"),
        E106("106", "LOCALITA' STATO INCONGRUENTE", "LA LOCALITA' NON CONGRUENTE CON LO STATO"),
        E107("107", "LOCALITA' NON TROVATA IN PROV", "LOCALITA' NON TROVATA NELLA PROVINCIA"),
        E108("108", "LISTA LOCA AMBIGUA TRONCATA", "LISTA DI LOCALITA' AMBIGUE TRONCATA"),
        E109("109", "LOCALITA' INVALIDE", "I DATI DELLE LOCALITA' NON SONO VALIDI"),
        E201("201", "FRAZIONE ASSENTE", "LA FRAZIONE NON E' PRESENTE IN INPUT"),
        E202("202", "FRAZIONE NON TROVATA", "NON E' STATO TROVATO NESSUN CANDIDATO PER LA FRAZIONE"),
        E203("203", "FRAZIONE ERRATA", "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA FRAZIONE, MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATO"),
        E204("204", "FRAZIONE NON UNIVOCA", "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA FRAZIONE"),
        E301("301", "LOCALITA' FRAZIONE INCONGRUENTI", "LA LOCALITA E LA FRAZIONE INDIVIDUANO DUE COMUNI DIVERSI"),
        E302("302", "LOCALITA' FRAZIONE INCONGRUENTI 2", "LOCALITA' E FRAZIONE INDICANO FRAZIONI DIVERSE"),
        E303("303", "FRAZIONE/VIA INCONGRUENTE", "LA FRAZIONE NON E' CONGRUENTE CON LA VIA"),
        E401("401", "DUG ASSENTE", "LA DUG NON E' PRESENTE IN INPUT"),
        E402("402", "DUG NON TROVATA", "NON E' STATA TROVATA NESSUN CANDIDATO PER LA DUG"),
        E404("404", "DUG NON UNIVOCA", "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA DUG"),
        E409("409", "DUG INVALIDE", "I DATI DELLE DUG E COMPLEMENTI A DUG NON SONO VALIDI"),
        E421("421", "VIA ASSENTE", "LA VIA NON E' PRESENTE IN INPUT"),
        E422("422", "VIA NON TROVATA", "NON E' STATA TROVATA NESSUN CANDIDATO PER LA VIA"),
        E423("423", "VIA ERRATA", "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA VIA, MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATA"),
        E424("424", "VIA NON UNIVOCA", "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA VIA"),
        E425("425", "VIA CON TROPPE VIRGOLE", "SONO STATE TROVATE IN INPUT ALMENO DUE VIRGOLE"),
        E426("426", "VIA, DIVERSA DA MACRODUG", "LA DUG IN INPUT E QUELLA DEL CANDIDATO HANNO DIVERSA MACRODUG"),
        E427("427", "VIA NON PUBBLICATA", "LA VIA CANDIDATA NON E' PUBBLICATA"),
        E429("429", "VIE INVALIDE", "I DATI DELLE VIE NON SONO VALIDI"),
        E430("430", "VIA UGUALE A FRAZIONE", "LA VIA IN INPUT CORRISPONDE ESATTAMENTE AD UNA FRAZIONE"),
        E441("441", "VIA, CAP, CIV INCONGRUENTI", "SOLO SULLE VIE MULTICAP: NON E' STATO TROVATO IL CIVICO SULL'ARCO DI CAP DELLA VIA"),
        E442("442", "CAP INCONGRUENTE CON LOCA", "IL CAMP TROVATO SULLA VIA DI UNA LOCALITA' E' INCONGRUENTE CON IL CAP DELLA LOCALITA' TROVATA"),
        E443("443", "CAP CIVICO INCONGRUENTI O/E AMBIGUI", "LA VIA NON E' UNIVOCA E ALMENO UNA HA CAP E CIVICO INCONGRUENTI"),
        E449("449", "ARCHI INVALIDI", "I DATI DEGLI ARCHI NON SONO VALIDI"),
        E451("451", "CASELLA POSTALE CON CAP GENERICO", "CASELLA POSTALE CON CAP GENERICO"),
        E452("452", "VIA PRESSO", "LA VIA E' UNA CLAUSOLA DI PRESSO"),
        E459("459", "CASELLA POSTALE NON VALIDA", "INDIRZZO CON CASELLA POSTALE NON VALIDO"),
        E601("601", "STATO NON RICONOSCIUTO", "NON E' STATO TROVATO NESSUN CANDIDATO PER LO STATO"),
        E609("609", "STATI INVALIDI", "I DATI DEGLI STATI ESTERI NON SONO VALIDI"),
        E901("901", "ERRORE_INTERNO", "ERRORE INTERNO NON CLASSIFICABILE"),
        E997("997", "ERRORE NEL SERVIZIO DI NORMALIZZAZIONE", "ERRORE NEL SERVIZIO DI NORMALIZZAZIONE"),
        E998("998", "ERRORE FORMATTAZIONE DATI", "ERRORE FORMATTAZIONE DATI"),
        E999("999", "CARATTERI NON CONSENTITI", "CARATTERI NON ASCII PRESENTI NELL'INDIRIZZO");

        private final String codice;
        private final String descrizioneBreve;
        private final String descrizioneLunga;

        PostelError(String codice, String descrizioneBreve, String descrizioneLunga) {
                this.codice = codice;
                this.descrizioneBreve = descrizioneBreve;
                this.descrizioneLunga = descrizioneLunga;
        }

        public String getCodice() {
                return codice;
        }

        public String getDescrizioneBreve() {
                return descrizioneBreve;
        }

        public String getDescrizioneLunga() {
                return descrizioneLunga;
        }
}