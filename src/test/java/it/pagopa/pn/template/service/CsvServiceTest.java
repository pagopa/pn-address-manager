package it.pagopa.pn.template.service;


import it.pagopa.pn.template.model.Cap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CsvServiceTest {

    @InjectMocks
    CsvService csvService;


    @Mock
    InputStream inputStreamMock;

    @Mock
    BufferedReader br;

    @Test
    public void testCountryMap() throws Exception {
        String csvData = "IT, Italy\n"
                + "FR, France\n"
                + "DE, Germany\n";


        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("IT", "Italy");
        expectedCountryMap.put("FR", "France");
        expectedCountryMap.put("DE", "Germany");

        Map<String, String> actualCountryMap = csvService.countryMap();

        assertEquals(expectedCountryMap, actualCountryMap);
    }

    @Test
    public void testCapMap() throws Exception {
        String csvData = "00010, Lazio, Roma\n"
                + "00011, Lazio, Roma\n"
                + "00012, Lazio, Roma\n";


        Map<String, Object> expectedCapMap = new HashMap<>();
        expectedCapMap.put("00010", new Cap("00010", "Lazio", "Roma"));
        expectedCapMap.put("00011", new Cap("00011", "Lazio", "Roma"));
        expectedCapMap.put("00012", new Cap("00012", "Lazio", "Roma"));

        Map<String, Object> actualCapMap = csvService.capMap();

        assertEquals(expectedCapMap, actualCapMap);
    }
}
