package it.pagopa.pn.template.service;

import it.pagopa.pn.template.rest.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.template.rest.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.template.utils.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;

    public DeduplicatesAddressService(AddressUtils addressUtils){
        this.addressUtils = addressUtils;
    }

    public Mono<DeduplicatesResponse> deduplicates(Mono<DeduplicatesRequest> request){
        return request.map(this::createDeduplicatesResponseByDeduplicatesRequest);
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress()));
        deduplicatesResponse.setNormalizedAddress(addressUtils.normalizeAddress(request.getTargetAddress()));
        return deduplicatesResponse;
    }

}
