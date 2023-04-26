package it.pagopa.pn.template.utils;

import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeResult;

import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.template.service.CsvService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AddressUtilsTest {

    @Mock
    private CsvService csvService;

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
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        assertTrue(addressUtils.compareAddress(base, base));
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
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        assertTrue(addressUtils.compareAddress(base, base));
    }

    @Test
    void compareAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        AnalogAddress target = new AnalogAddress();
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        assertFalse(addressUtils.compareAddress(base, target));
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
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        assertNotNull(addressUtils.normalizeAddress(base));
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
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        assertNotNull(addressUtils.normalizeAddress(base));
    }


    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses() {
        AddressUtils addressUtils = new AddressUtils(true,csvService);
        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses3() {
        AddressUtils addressUtils = new AddressUtils(true,csvService);

        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
        List<NormalizeResult> resultItems = actualNormalizeAddressesResult.getResultItems();
        assertEquals(1, resultItems.size());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses4() {

        AddressUtils addressUtils = new AddressUtils(true, csvService);
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(new AnalogAddress());
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
        List<NormalizeResult> resultItems = actualNormalizeAddressesResult.getResultItems();
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses6() {
        AddressUtils addressUtils = new AddressUtils(true, csvService);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("GB");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
        List<NormalizeResult> resultItems = actualNormalizeAddressesResult.getResultItems();
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
        verify(analogAddress, atLeast(1)).getCountry();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses7() {

        AddressUtils addressUtils = new AddressUtils(false, csvService);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        doNothing().when(analogAddress).setAddressRow2(any());
        doNothing().when(analogAddress).setCap(any());
        doNothing().when(analogAddress).setCity2(any());
        doNothing().when(analogAddress).setCountry(any());
        doNothing().when(analogAddress).setPr(any());
        when(analogAddress.getAddressRow2()).thenReturn("42 Main St");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCity2()).thenReturn("Oxford");
        when(analogAddress.getCountry()).thenReturn("GB");
        when(analogAddress.getPr()).thenReturn("Pr");
        doNothing().when(analogAddress).setCity(any());
        when(analogAddress.getCity()).thenReturn("Oxford");
        doNothing().when(analogAddress).setAddressRow(any());
        when(analogAddress.getAddressRow()).thenReturn("42 Main St");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
        List<NormalizeResult> resultItems = actualNormalizeAddressesResult.getResultItems();
        assertEquals(1, resultItems.size());
        assertEquals("42", resultItems.get(0).getId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses8() {

        AddressUtils addressUtils = new AddressUtils(false,csvService);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setAddressRow2(any());
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setCap(any());
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setCity2(any());
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setCountry(any());
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setPr(any());
        when(analogAddress.getAddressRow2()).thenReturn("42 Main St");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCity2()).thenReturn("Oxford");
        when(analogAddress.getCountry()).thenReturn("GB");
        when(analogAddress.getPr()).thenReturn("Pr");
        doNothing().when(analogAddress).setCity(any());
        when(analogAddress.getCity()).thenReturn("Oxford");
        doNothing().when(analogAddress).setAddressRow(any());
        when(analogAddress.getAddressRow()).thenReturn("42 Main St");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        assertThrows(PnAddressManagerException.class, () -> addressUtils.normalizeAddresses("42", normalizeRequestList));
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
        verify(analogAddress).getAddressRow();
        verify(analogAddress, atLeast(1)).getCap();
        verify(analogAddress).getCity();
        verify(analogAddress).setAddressRow(any());
        verify(analogAddress).setCap(any());
        verify(analogAddress).setCity(any());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses9() {
        AddressUtils addressUtils = new AddressUtils(false,csvService);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getAddressRow2())
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(analogAddress.getCap()).thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(analogAddress.getCity2()).thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(analogAddress.getCountry())
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(analogAddress.getPr()).thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        doThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE)).when(analogAddress)
                .setCity(any());
        when(analogAddress.getCity()).thenReturn("Oxford");
        doNothing().when(analogAddress).setAddressRow(any());
        when(analogAddress.getAddressRow()).thenReturn("42 Main St");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        assertThrows(PnAddressManagerException.class,
                () -> addressUtils.normalizeAddresses("Correlation Id", normalizeRequestList));
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
        verify(analogAddress).getAddressRow();
        verify(analogAddress).getCity();
        verify(analogAddress).setAddressRow(any());
        verify(analogAddress).setCity(any());
    }
}

