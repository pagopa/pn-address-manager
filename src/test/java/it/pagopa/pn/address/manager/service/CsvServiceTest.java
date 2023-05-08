package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.AddressModel;
import it.pagopa.pn.address.manager.model.CapModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @InjectMocks
    private CsvService csvService;

    /**
     * Method under test: {@link CsvService#createAddressCsvByCorrelationId(List, String)}
     */
    @Test
    void testCreateAddressCsvByCorrelationId() {
        List<AddressModel> addressModels = new ArrayList<>();
        AddressModel addressModel = new AddressModel();
        addressModel.setAddressId("addressId");
        addressModel.setCorrelationId("correlationId");
        addressModel.setAddressRow("addressRow");
        addressModel.setAddressRow2("addressRow2");
        addressModel.setPr("pr");
        addressModel.setCity("city");
        addressModel.setCity2("city2");
        addressModel.setCxid("cxId");
        addressModel.setCountry("country");
        addressModel.setCap("cap");
        AddressModel addressModel1 = new AddressModel();
        addressModel1.setAddressId("addressId");
        addressModel1.setCorrelationId("correlationId");
        addressModel1.setAddressRow("addressRow");
        addressModel1.setAddressRow2("addressRow2");
        addressModel1.setPr("pr");
        addressModel1.setCity("city");
        addressModel1.setCity2("city2");
        addressModel1.setCxid("cxId1");
        addressModel1.setCountry("country");
        addressModel1.setCap("cap");
        AddressModel addressModel2 = new AddressModel();
        addressModel2.setAddressId("addressId");
        addressModel2.setCorrelationId("correlationId1");
        addressModel2.setAddressRow("addressRow");
        addressModel2.setAddressRow2("addressRow2");
        addressModel2.setPr("pr");
        addressModel2.setCity("city");
        addressModel2.setCity2("city2");
        addressModel2.setCxid("cxId2");
        addressModel2.setCountry("country");
        addressModel2.setCap("cap");
        addressModels.add(addressModel);
        addressModels.add(addressModel1);
        addressModels.add(addressModel2);
        addressModels.add(addressModel);
        addressModels.add(addressModel1);
        addressModels.add(addressModel2);
        assertDoesNotThrow( () -> csvService.createAddressCsvByCorrelationId(addressModels, "42"));
    }

    @Test
    void testReadNormalizeItemsResultFromCsv(){
        assertDoesNotThrow( () -> csvService.readNormalizeItemsResultFromCsv());

    }

    @Test
    void testCountryMap() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv");
        Map<String, String> expectedCountryMap = new HashMap<>();
        expectedCountryMap.put("AFGHANISTAN", "AFGHANISTAN");
        expectedCountryMap.put("AFRICA DEL SUD", "SUDAFRICA");
        expectedCountryMap.put("AFRIQUE DU SUD", "SUDAFRICA");

        Map<String, String> actualCountryMap = csvService.countryMap();
        assertEquals(expectedCountryMap, actualCountryMap);
    }

    @Test
    void testCountryMapException() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni1.csv", "Mock-ListaCAP1.csv");

        assertThrows(PnAddressManagerException.class, csvService::countryMap);

    }

    @Test
    void testCapMap() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni.csv", "Mock-ListaCAP.csv");

        Map<String, Object> expectedCapMap = new HashMap<>();
        expectedCapMap.put("00010", new CapModel("00010", "Lazio", "Roma"));
        expectedCapMap.put("00013", new CapModel("00013", "Lazio", "Roma"));
        expectedCapMap.put("00012", new CapModel("00012", "Lazio", "Roma"));

        Map<String, Object> actualCapMap = csvService.capMap();

        assertEquals(expectedCapMap, actualCapMap);
    }

    @Test
    void testCapMapException() {
        CsvService csvService = new CsvService("Mock-Lista-Nazioni1.csv", "Mock-ListaCAP1.csv");

        assertThrows(PnAddressManagerException.class, csvService::capMap);

    }
}
