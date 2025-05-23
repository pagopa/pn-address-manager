package it.pagopa.pn.address.manager.constant;

import lombok.CustomLog;
import lombok.Getter;

import java.util.Objects;
@Getter
@CustomLog
public enum PostelNErrorNorm {
	ERROR_001(1, "IL CAP NON E' PRESENTE IN INPUT"),
	ERROR_002(2, "LA PROVINCIA NON E' PRESENTE IN INPUT"),
	ERROR_003(3, "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELLA LOCALITA"),
	ERROR_004(4, "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELLA VIA"),
	ERROR_005(5, "IL CAP IN INPUT RISULTA DIVERSO DAL CAP DELL'ARCO DI CAP DELLA VIA"),
	ERROR_006(6, "LA SIGLA PROVINCIA IN INPUT RISULTA DIVERSA DA QUELLA DETERMINATA"),
	ERROR_007(7, "IL CAP NON E' PRESENTE NEL RANGE DI CAP CONSENTITI"),
	ERROR_008(8, "LA PROVINCIA NON E' PRESENTE SULLA TABELLA PROVINCIE"),
	ERROR_009(9, "LA PROVINCIA E IL CAP INDIVIDUANO DUE PROVINCIE DIVERSE"),
	ERROR_019(19, "I DATI DI PROVINCE E/O CAP NON SONO VALIDI"),
	ERROR_101(101, "LA LOCALITA' NON E' PRESENTE IN INPUT"),
	ERROR_102(102, "NON E' STATO TROVATO NESSUN CANDIDATO PER LA LOCALITA"),
	ERROR_103(103, "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA LOCALITA', MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATO"),
	ERROR_104(104, "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA LOCALITA"),
	ERROR_105(105, "LA LOCALITA CANDIDATA NON E' PUBBLICATA"),
	ERROR_106(106, "LA LOCALITA' NON CONGRUENTE CON LO STATO"),
	ERROR_107(107, "LOCALITA' NON TROVATA NELLA PROVINCIA"),
	ERROR_108(108, "LISTA DI LOCALITA' AMBIGUE TRONCATA"),
	ERROR_109(109, "I DATI DELLE LOCALITA' NON SONO VALIDI"),
	ERROR_201(201, "LA FRAZIONE NON E' PRESENTE IN INPUT"),
	ERROR_202(202, "NON E' STATO TROVATO NESSUN CANDIDATO PER LA FRAZIONE"),
	ERROR_203(203, "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA FRAZIONE, MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATO"),
	ERROR_204(204, "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA FRAZIONE"),
	ERROR_301(301, "LA LOCALITA E LA FRAZIONE INDIVIDUANO DUE COMUNI DIVERSI"),
	ERROR_302(302, "LOCALITA' E FRAZIONE INDICANO FRAZIONI DIVERSE"),
	ERROR_303(303, "LA FRAZIONE NON E' CONGRUENTE CON LA VIA"),
	ERROR_401(401, "LA DUG NON E' PRESENTE IN INPUT"),
	ERROR_402(402, "NON E' STATA TROVATA NESSUN CANDIDATO PER LA DUG"),
	ERROR_404(404, "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA DUG"),
	ERROR_409(409, "I DATI DELLE DUG E COMPLEMENTI A DUG NON SONO VALIDI"),
	ERROR_421(421, "LA VIA NON E' PRESENTE IN INPUT"),
	ERROR_422(422, "NON E' STATA TROVATA NESSUN CANDIDATO PER LA VIA"),
	ERROR_423(423, "E' STATO TROVATO ALMENO UN CANDIDATO PER QUESTA VIA, MA NESSUNO AVEVA UN PUNTEGGIO TALE DA ESSERE VALIDATA"),
	ERROR_424(424, "SONO STATI TROVATI PIU' CANDIDATI VALIDI PER QUESTA VIA"),
	ERROR_425(425, "SONO STATE TROVATE IN INPUT ALMENO DUE VIRGOLE"),
	ERROR_426(426, "LA DUG IN INPUT E QUELLA DEL CANDIDATO HANNO DIVERSA MACRODUG"),
	ERROR_427(427, "LA VIA CANDIDATA NON E' PUBBLICATA"),
	ERROR_429(429, "I DATI DELLE VIE NON SONO VALIDI"),
	ERROR_430(430, "LA VIA IN INPUT CORRISPONDE ESATTAMENTE AD UNA FRAZIONE"),
	ERROR_431(431, "LA VIA NON E' STATA TROVATA ALL'INTERNO DI UNA CITTA' ZONATA E IL CAP DI INPUT NON è UN CAP VALIDO PER QUELLA CITTA'"),
	ERROR_441(441, "VIA, CAP, CIV INCONGRUENTI, SOLO SULLE VIE MULTICAP: NON E' STATO TROVATO IL CIVICO SULL'ARCO DI CAP DELLA VIA"),
	ERROR_442(442, "IL CAMP TROVATO SULLA VIA DI UNA LOCALITA' E' INCONGRUENTE CON IL CAP DELLA LOCALITA' TROVATA"),
	ERROR_443(443, "LA VIA NON E' UNIVOCA E ALMENO UNA HA CAP E CIVICO INCONGRUENTI"),
	ERROR_449(449, "I DATI DEGLI ARCHI NON SONO VALIDI"),
	ERROR_451(451, "CASELLA POSTALE CON CAP GENERICO"),
	ERROR_452(452, "LA VIA E' UNA CLAUSOLA DI PRESSO"),
	ERROR_459(459, "INDIRZZO CON CASELLA POSTALE NON VALIDO"),
	ERROR_461(461, "INDIRIZZO NON POSTABILIZZABILE PER RIGA DI STAMPA TROPPO LUNGA"),
	ERROR_501(501, "ALCUNI ELEMENTI DELL'INDIRIZZO HANNO UNA LUNGHEZZA SUPERIORE A QUELLA STAMPABILE"),
	ERROR_601(601, "NON E' STATO TROVATO NESSUN CANDIDATO PER LO STATO"),
	ERROR_609(609, "I DATI DEGLI STATI ESTERI NON SONO VALIDI"),
	ERROR_701(701, "È STATA SUPERATA LA LUNGHEZZA MASSIMA PER UN SINGOLO CAMPO DI INPUT"),
    ERROR_702(702, "È STATA SUPERATA LA LUNGHEZZA MASSIMA DELL'INTERO QUERY DI INPUT"),
	ERROR_901(901, "ERRORE INTERNO NON CLASSIFICABILE"),
	ERROR_997(997, "ERRORE NEL SERVIZIO DI NORMALIZZAZIONE"),
	ERROR_998(998, "ERRORE FORMATTAZIONE DATI"),
	ERROR_999(999, "CARATTERI NON ASCII PRESENTI NELL'INDIRIZZO"),
	ERROR_000(000,"ERRORE GENERICO");

	private final Integer code;
	private final String description;

	PostelNErrorNorm(Integer code, String description) {
		this.code = code;
		this.description = description;
	}

	public static PostelNErrorNorm fromCode(Integer value) {
		for (PostelNErrorNorm b : PostelNErrorNorm.values()) {
			if (Objects.equals(b.getCode(), value)) {
				return b;
			}
		}
		log.warn("Unexpected NErroreNorm: {}", value);
		return ERROR_000;
	}
}