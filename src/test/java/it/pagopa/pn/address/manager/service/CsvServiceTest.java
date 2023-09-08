package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.model.CapModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @Test
    void testCountryMap() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCLP.csv");
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN","AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD","SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD","SUDAFRICA");

        Map<String, String> actualCountryMap = csvService.countryMap();
        assertEquals(expectedCountryMap, actualCountryMap);
    }

    @Test
    void testCapMap(){
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCLP.csv");

        List<CapModel> expectedCapMap = new ArrayList<>();
        expectedCapMap.add(new CapModel("00100", "ROMA", "RM"));
        expectedCapMap.add(new CapModel("20019", "MILANO", "MI"));

        List<CapModel> actualCapMap = csvService.capList();

        assertEquals(expectedCapMap, actualCapMap);
    }
}
