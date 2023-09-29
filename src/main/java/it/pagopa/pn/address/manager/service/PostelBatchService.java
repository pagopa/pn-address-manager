package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostelBatchService {

    private final AddressBatchRequestRepository addressBatchRequestRepository;
    private final PostelBatchRepository postelBatchRepository;
    private final CsvService csvService;
    private final ObjectMapper objectMapper;

    public PostelBatchService(AddressBatchRequestRepository addressBatchRequestRepository,
                              PostelBatchRepository postelBatchRepository,
                              CsvService csvService,
                              ObjectMapper objectMapper) {
        this.addressBatchRequestRepository = addressBatchRequestRepository;
        this.postelBatchRepository = postelBatchRepository;
        this.csvService = csvService;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> getResponsesFromCsv(byte[] csvFile, String fileKeyInput) {

        List<NormalizedAddress> normalizedAddressList = csvService.readItemsFromCsv(NormalizedAddress.class, csvFile, 1);

        return postelBatchRepository.findByFileKey(fileKeyInput)
                .flatMap(v -> addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(v.getBatchId(), BatchStatus.WORKED))
                .map(v -> {
                    List<String> correlationsId = v.stream().map(BatchRequest::getCorrelationId).toList();
                    Map<String, NormalizeItemsRequest> normalizeItemsRequestMap = new HashMap<>();
                    for(BatchRequest batchRequest: v) {
                        NormalizeItemsRequest normalizeItemsRequest;
                        try {
                            normalizeItemsRequest = objectMapper.readValue(batchRequest.getAddresses(), NormalizeItemsRequest.class);
                        } catch (JsonProcessingException e) {
                            throw new PnAddressManagerException("", "", 500, ""); // TODO: valorizzare i campi
                        }
                        normalizeItemsRequestMap.put(normalizeItemsRequest.getCorrelationId(), normalizeItemsRequest);
                    }
                    normalizedAddressList.forEach(z -> {
                        if(!correlationsId.contains(z.getId())) {
                            // send correlationId request list to input queue for retry
                        }
                    });

                    for(String correlationId : correlationsId) {
                        long batchRequestCount = normalizedAddressList.stream()
                                .filter(z -> z.getId().equals(correlationId))
                                .count();
                        if(batchRequestCount != normalizeItemsRequestMap.get(correlationId).getRequestItems().size()) {
                            // send correlationId request list to input queue for retry
                        }
                    }
                    return null;
                });

    }
}
