package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.model.CapModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @Test
    void testCountryMap() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv");
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN","AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD","SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD","SUDAFRICA");

        Map<String, String> actualCountryMap = csvService.countryMap();
        assertEquals(expectedCountryMap, actualCountryMap);
    }

    @Test
    void testCapMap(){
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv");

        Map<String, Object> expectedCapMap = new HashMap<>();
        expectedCapMap.put("00010", new CapModel("00010", "Lazio", "Roma"));
        expectedCapMap.put("00013", new CapModel("00013", "Lazio", "Roma"));
        expectedCapMap.put("00012", new CapModel("00012", "Lazio", "Roma"));

        Map<String, Object> actualCapMap = csvService.capMap();

        assertEquals(expectedCapMap, actualCapMap);
    }
}
