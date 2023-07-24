package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.model.CapModel;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (MockitoExtension.class)
class CsvServiceTest {
    @Autowired
    private CsvService csvService;
    @Mock
    private ResourceLoader resourceLoader;

    @Test
    void testCountryMap () throws IOException {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv",resourceLoader);
        when(resourceLoader.getResource(anyString())).thenReturn(new ClassPathResource("Mock-Lista-Nazioni.csv"));
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN", "AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD", "SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD", "SUDAFRICA");

        Map<String, String> actualCountryMap = csvService.countryMap();
        assertEquals(expectedCountryMap, actualCountryMap);
    }
    @Test
    void testCapMap () {
        ClassPathResource classPathResource = new ClassPathResource("Mock-ListaCAP.csv");
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv",resourceLoader);
        when(resourceLoader.getResource(anyString())).thenReturn(new ClassPathResource("Mock-Lista-Nazioni.csv"));
        Map<String, Object> expectedCapMap = new HashMap<>();
        expectedCapMap.put("00010", new CapModel("00010", "Lazio", "Roma"));
        expectedCapMap.put("00013", new CapModel("00013", "Lazio", "Roma"));
        expectedCapMap.put("00012", new CapModel("00012", "Lazio", "Roma"));
        Map<String, Object> actualCapMap = csvService.capMap();
        Assertions.assertEquals(expectedCapMap, actualCapMap);
    }
}
