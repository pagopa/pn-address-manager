package it.pagopa.pn.template.service;

import it.pagopa.pn.template.rest.v1.dto.AnalogAddress;
import it.pagopa.pn.template.rest.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.template.rest.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.template.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
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
        deduplicatesResponse.setNormalizedAddress(target);
        deduplicatesResponse.setError(null);

        when(addressUtils.compareAddress(any(),any())).thenReturn(false);
        when(addressUtils.normalizeAddress(any())).thenReturn(target);

        StepVerifier.create(deduplicatesAddressService.deduplicates("cxid","apikey",Mono.just(deduplicatesRequest)))
                .expectNext(deduplicatesResponse).verifyComplete();
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
        deduplicatesResponse.setError("Target Address Not Found");

        when(addressUtils.compareAddress(any(),any())).thenReturn(false);
        when(addressUtils.normalizeAddress(any())).thenReturn(null);

        StepVerifier.create(deduplicatesAddressService.deduplicates("cxid","apikey",Mono.just(deduplicatesRequest)))
                .expectNext(deduplicatesResponse).verifyComplete();
    }
}

