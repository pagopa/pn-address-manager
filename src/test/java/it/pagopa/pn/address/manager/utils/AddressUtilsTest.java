package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.service.CsvService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddressUtilsTest {

    @Mock
    private CsvService csvService;

    @Mock
    private PnAddressManagerConfig pnAddressManagerConfig;

    @BeforeAll
    void setUp() {
        when(csvService.capList()).thenReturn(getMockedCapMap());
        when(csvService.countryMap()).thenReturn(getMockedCountryMap());
        when(pnAddressManagerConfig.getEnableValidation()).thenReturn(true);
        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(true);
        when(pnAddressManagerConfig.getValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
    }

    private Map<String, String> getMockedCountryMap() {
        Map<String, String> mockedCountryMap = new HashMap<>();
        mockedCountryMap.put("ITALIA","ITALIA");
        mockedCountryMap.put("AFRICA DEL SUD","SUDAFRICA");
        mockedCountryMap.put("AMERICA","STATI UNITI D'AMERICA");
        return mockedCountryMap;
    }

    private List<CapModel> getMockedCapMap() {
        List<CapModel> mockedCapList = new ArrayList<>();
        mockedCapList.add(new CapModel("00010","Roma","RM"));
        mockedCapList.add( new CapModel("00011","Roma","RM"));
        mockedCapList.add(new CapModel("00012","Roma","RM"));
        return mockedCapList;
    }

    @Test
    void compareAddress() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertTrue(addressUtils.compareAddress(base, base, true));
    }

    @Test
    void compareAddress1() {
        AnalogAddress base = new AnalogAddress();
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertTrue(addressUtils.compareAddress(base, base, false));
    }

    @Test
    void compareAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        AnalogAddress target = new AnalogAddress();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertFalse(addressUtils.compareAddress(base, target, true));
    }

    @Test
    void normalizeAddress() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("00010");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertNotNull(addressUtils.normalizeAddress(base,"1"));
    }

    @Test
    void normalizeAddress1() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma ");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("RM  ");
        base.setCap("00010");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertNotNull(addressUtils.normalizeAddress(base,"1"));
    }

    @Test
    void normalizeAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("ARUBA");
        base.setCountry("ARUBA");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        assertNotNull(addressUtils.normalizeAddress(base, "1"));
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
     */
    @Test
    void testNormalizeAddresses3() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);

        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
     */
    @Test
    void testNormalizeAddresses4() {

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(new AnalogAddress());
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
     */
    @Test
    void testNormalizeAddresses6() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00010");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        when(analogAddress.getPr()).thenReturn("RM");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    @Test
    void testNormalizeAddresses7() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00010");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }
    @Test
    void testNormalizeAddresses8() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00015");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
     */
    @Test
    void testNormalizeAddresses9() {

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getAddressRow2()).thenReturn("42 Main St");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCity2()).thenReturn("Oxford");
        when(analogAddress.getCountry()).thenReturn("GB");
        when(analogAddress.getPr()).thenReturn("Pr");
        when(analogAddress.getCity()).thenReturn("Oxford");
        when(analogAddress.getAddressRow()).thenReturn("42 Main St");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        assertEquals("42", resultItems.get(0).getId());
    }
}

