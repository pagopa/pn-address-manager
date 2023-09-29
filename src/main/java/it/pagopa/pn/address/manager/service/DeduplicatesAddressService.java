package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DeduplicatesAddressService {

    private final AddressUtils addressUtils;
    private final boolean flagCsv;
    private final PagoPaClient pagoPaClient;
    private final AddressConverter addressConverter;

    public DeduplicatesAddressService(AddressUtils addressUtils, @Value("${pn.address.manager.flag.csv}") boolean flagCsv, PagoPaClient pagoPaClient, AddressConverter addressConverter){
        this.addressUtils = addressUtils;
        this.flagCsv = flagCsv;
        this.pagoPaClient = pagoPaClient;
        this.addressConverter = addressConverter;
    }

    public Mono<DeduplicatesResponse> deduplicates(DeduplicatesRequest request) {
        if(flagCsv){
            return Mono.just(createDeduplicatesResponseByDeduplicatesRequest(request));
        }
        return pagoPaClient
                    .deduplicaOnline(addressConverter.createDeduplicaRequestFromDeduplicatesRequest(request))
                    .map(addressConverter::createDeduplicatesResponseFromDeduplicaResponse);
    }

    private DeduplicatesResponse createDeduplicatesResponseByDeduplicatesRequest(DeduplicatesRequest request){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(request.getCorrelationId());
        NormalizedAddressResponse normalizeAddressResponse = addressUtils.normalizeAddress(request.getTargetAddress(), null);
        deduplicatesResponse.setEqualityResult(addressUtils.compareAddress(request.getBaseAddress(), request.getTargetAddress(), normalizeAddressResponse.isItalian()));
        deduplicatesResponse.setError(normalizeAddressResponse.getError());
        deduplicatesResponse.setNormalizedAddress(normalizeAddressResponse.getNormalizedAddress());
        return deduplicatesResponse;
    }

}
