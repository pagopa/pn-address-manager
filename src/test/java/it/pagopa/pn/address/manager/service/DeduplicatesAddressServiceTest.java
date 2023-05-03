package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.rest.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DeduplicatesAddressServiceTest {

    @Mock
    private AddressUtils addressUtils;

    @InjectMocks
    private DeduplicatesAddressService deduplicatesAddressService;

    @Test
    void deduplicates1(){
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        AnalogAddress target = new AnalogAddress();
        target.setCity("42");
        target.setCity2("42");
        target.setAddressRow("42");
        target.setAddressRow2("42");
        target.setPr("42");
        target.setCountry("42");
        target.setCap("42");
        normalizedAddressResponse.setNormalizedAddress(target);
        deduplicatesRequest.setBaseAddress(base);
        deduplicatesRequest.setTargetAddress(target);
        deduplicatesRequest.setCorrelationId("42");

        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setEqualityResult(true);
        deduplicatesResponse.setCorrelationId("42");
        deduplicatesResponse.setNormalizedAddress(target);
        deduplicatesResponse.setError(null);

        when(addressUtils.compareAddress(any(),any(), anyBoolean())).thenReturn(true);
        when(addressUtils.normalizeAddress(any(), any())).thenReturn(normalizedAddressResponse);

        DeduplicatesResponse response = deduplicatesAddressService.deduplicates(deduplicatesRequest);
        assertEquals(deduplicatesResponse.getEqualityResult(), response.getEqualityResult());
    }

    @Test
    void deduplicates2(){
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AnalogAddress target = new AnalogAddress();
        target.setCity("42");
        target.setCity2("42");
        target.setAddressRow("42");
        target.setAddressRow2("42");
        target.setPr("42");
        target.setCountry("42");
        target.setCap("42");
        deduplicatesRequest.setBaseAddress(base);
        deduplicatesRequest.setTargetAddress(target);
        deduplicatesRequest.setCorrelationId("42");

        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setEqualityResult(false);
        deduplicatesResponse.setCorrelationId("42");
        deduplicatesResponse.setNormalizedAddress(null);
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        when(addressUtils.compareAddress(any(),any(), anyBoolean())).thenReturn(false);
        when(addressUtils.normalizeAddress(any(), any())).thenReturn(normalizedAddressResponse);

        DeduplicatesResponse response = deduplicatesAddressService.deduplicates(deduplicatesRequest);
        assertEquals(response.getEqualityResult(), deduplicatesResponse.getEqualityResult());
    }
}

