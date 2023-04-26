package it.pagopa.pn.template.utils;

import it.pagopa.pn.template.exception.PnAddressManagerException;
import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.template.rest.v1.dto.NormalizeItemsResult;
import it.pagopa.pn.template.rest.v1.dto.NormalizeRequest;
import it.pagopa.pn.template.rest.v1.dto.NormalizeResult;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        AddressUtils addressUtils = new AddressUtils(true);
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
        AddressUtils addressUtils = new AddressUtils(true);
        assertTrue(addressUtils.compareAddress(base, base));
    }

    @Test
    void compareAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        AnalogAddress target = new AnalogAddress();
        AddressUtils addressUtils = new AddressUtils(true);
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
        AddressUtils addressUtils = new AddressUtils(true);
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
        AddressUtils addressUtils = new AddressUtils(true);
        assertNotNull(addressUtils.normalizeAddress(base));
    }


    @Test
    void normalizeAddress3() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("ARUBAA");
        base.setCountry("ARUBAA");
        AddressUtils addressUtils = new AddressUtils(true);
        assertNull(addressUtils.normalizeAddress(base));
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses() {
        AddressUtils addressUtils = new AddressUtils(true);
        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses3() {
        AddressUtils addressUtils = new AddressUtils(true);

        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        NormalizeItemsResult actualNormalizeAddressesResult = addressUtils.normalizeAddresses("42", normalizeRequestList);
        assertEquals("42", actualNormalizeAddressesResult.getCorrelationId());
        List<NormalizeResult> resultItems = actualNormalizeAddressesResult.getResultItems();
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertNull(getResult.getNormalizedAddress());
        assertNull(getResult.getId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses4() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R027 Missing beans when creating Spring context.
        //   Failed to create Spring context due to missing beans
        //   in the current Spring profile:
        //       it.pagopa.pn.template.utils.AddressUtils
        //   See https://diff.blue/R027 to resolve this issue.

        AddressUtils addressUtils = new AddressUtils(true);
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
        assertNull(getResult.getNormalizedAddress());
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
        AddressUtils addressUtils = new AddressUtils(true);
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
        assertNull(getResult.getNormalizedAddress());
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
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R027 Missing beans when creating Spring context.
        //   Failed to create Spring context due to missing beans
        //   in the current Spring profile:
        //       it.pagopa.pn.template.utils.AddressUtils
        //   See https://diff.blue/R027 to resolve this issue.

        AddressUtils addressUtils = new AddressUtils(false);
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
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
        verify(analogAddress).getAddressRow();
        verify(analogAddress, atLeast(1)).getAddressRow2();
        verify(analogAddress, atLeast(1)).getCap();
        verify(analogAddress).getCity();
        verify(analogAddress, atLeast(1)).getCity2();
        verify(analogAddress, atLeast(1)).getCountry();
        verify(analogAddress, atLeast(1)).getPr();
        verify(analogAddress).setAddressRow(any());
        verify(analogAddress).setAddressRow2(any());
        verify(analogAddress).setCap(any());
        verify(analogAddress).setCity(any());
        verify(analogAddress).setCity2(any());
        verify(analogAddress).setCountry(any());
        verify(analogAddress).setPr(any());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(String, List)}
     */
    @Test
    void testNormalizeAddresses8() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R027 Missing beans when creating Spring context.
        //   Failed to create Spring context due to missing beans
        //   in the current Spring profile:
        //       it.pagopa.pn.template.utils.AddressUtils
        //   See https://diff.blue/R027 to resolve this issue.

        AddressUtils addressUtils = new AddressUtils(false);
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
        AddressUtils addressUtils = new AddressUtils(false);
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

