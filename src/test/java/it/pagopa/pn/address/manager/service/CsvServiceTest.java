package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @InjectMocks
    private CsvService csvService;

    @Mock
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Test
    void readItemsFromCsv() throws IOException {
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setCsvIncludeEscape(true);
        normalizer.setPostel(postel);
        when(pnAddressManagerConfig.getNormalizer()).thenReturn(normalizer);
        CsvService csvService = new CsvService(pnAddressManagerConfig);
        File file = new File("src/test/resources", "test-escape.csv");
        InputStream inputStream = new FileInputStream(file);
        byte[] bytes = inputStream.readAllBytes();

        assertNotNull(csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 0));

    }

    @Test
    void readItemsFromCsvError() throws IOException {
        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setCsvIncludeEscape(false);
        normalizer.setPostel(postel);
        when(pnAddressManagerConfig.getNormalizer()).thenReturn(normalizer);
        CsvService csvService = new CsvService(pnAddressManagerConfig);
        File file = new File("src/test/resources", "test-escape.csv");
        InputStream inputStream = new FileInputStream(file);
        byte[] bytes = inputStream.readAllBytes();

        Assertions.assertThrows(PnInternalAddressManagerException.class, () -> csvService.readItemsFromCsv(NormalizedAddress.class, bytes, 0));

    }

    @Test
    void writeItemsOnCsv() {
        CapModel capModel = new CapModel("00100", "ROMA", "RM");
        List<CapModel> capModels = new ArrayList<>();
        capModels.add(capModel);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP.csv");
        csv.setPathCountry("Mock-Lista-Nazioni.csv");
        pnAddressManagerConfig.setCsv(csv);

        CsvService csvService = new CsvService(pnAddressManagerConfig);
        assertDoesNotThrow(() -> csvService.writeItemsOnCsv(capModels, "test.csv", "src/test/resources"));
    }

    @Test
    void writeItemsOnCsvToString() {
        CapModel capModel = new CapModel("00100", "ROMA", "RM");
        List<CapModel> capModels = new ArrayList<>();
        capModels.add(capModel);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP.csv");
        csv.setPathCountry("Mock-Lista-Nazioni.csv");
        pnAddressManagerConfig.setCsv(csv);

        CsvService csvService = new CsvService(pnAddressManagerConfig);
        assertDoesNotThrow(() -> csvService.writeItemsOnCsvToString(capModels));
    }

    @Test
    void testCountryMapNull() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP1.csv");
        csv.setPathCountry("Mock-Lista-Nazioni1.csv");
        pnAddressManagerConfig.setCsv(csv);
        CsvService csvService = new CsvService(pnAddressManagerConfig);
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN", "AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD", "SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD", "SUDAFRICA");

        Assertions.assertThrows(PnInternalAddressManagerException.class, csvService::countryMap);

    }


    @Test
    void testCountryMap() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP.csv");
        csv.setPathCountry("Mock-Lista-Nazioni.csv");
        pnAddressManagerConfig.setCsv(csv);
        CsvService csvService = new CsvService(pnAddressManagerConfig);
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN", "AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD", "SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD", "SUDAFRICA");

        Map<String, String> actualCountryMap = csvService.countryMap();
        assertEquals(expectedCountryMap, actualCountryMap);
    }

    @Test
    void testCapMap() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP.csv");
        csv.setPathCountry("Mock-Lista-Nazioni.csv");
        pnAddressManagerConfig.setCsv(csv);

        CsvService csvService = new CsvService(pnAddressManagerConfig);

        List<CapModel> expectedCapMap = new ArrayList<>();
        expectedCapMap.add(new CapModel("00100", "ROMA", "RM"));
        expectedCapMap.add(new CapModel("20019", "MILANO", "MI"));

        List<CapModel> actualCapMap = csvService.capList();

        assertEquals(expectedCapMap, actualCapMap);
    }

    @Test
    void testCapMapNull() {
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Mock-ListaCLP1.csv");
        csv.setPathCountry("Mock-Lista-Nazioni1.csv");
        pnAddressManagerConfig.setCsv(csv);

        CsvService csvService = new CsvService(pnAddressManagerConfig);

        List<CapModel> expectedCapMap = new ArrayList<>();
        expectedCapMap.add(new CapModel("00100", "ROMA", "RM"));
        expectedCapMap.add(new CapModel("20019", "MILANO", "MI"));

        Assertions.assertThrows(PnInternalAddressManagerException.class, csvService::capList);
    }
}
