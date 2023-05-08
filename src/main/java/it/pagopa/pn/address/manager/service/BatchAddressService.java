package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.BatchAddress;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.AddressModel;
import it.pagopa.pn.address.manager.model.NormalizeItemsResultModel;
import it.pagopa.pn.address.manager.repository.BatchAddressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_BATCH_ADDRESS;
import static it.pagopa.pn.commons.log.MDCWebFilter.MDC_TRACE_ID_KEY;

@Service
@Slf4j
public class BatchAddressService {

    private final BatchAddressRepository batchAddressRepository;
    private final CsvService csvService;
    private final NormalizeAddressService normalizeAddressService;
    private final int maxRetry;

    private static final int MAX_BATCH_REQUEST_SIZE = 100;

    public BatchAddressService(BatchAddressRepository batchAddressRepository,
                               CsvService csvService,
                               NormalizeAddressService normalizeAddressService,
                               @Value("${pn.address.manager.batch.max-retry}") int maxRetry) {
        this.batchAddressRepository = batchAddressRepository;
        this.csvService = csvService;
        this.normalizeAddressService = normalizeAddressService;
        this.maxRetry = maxRetry;
    }

    @Scheduled(fixedDelayString = "${pn.address.manager.batch.delay}")
    public void batchAddress() {
        log.trace("AddressManager - batchAddress start");
        Page<BatchAddress> page;
        Map<String, AttributeValue> lastEvaluatedKey = new HashMap<>();
        do {
            page = getBatchAddress(lastEvaluatedKey);
            lastEvaluatedKey = page.lastEvaluatedKey();
            if (!page.items().isEmpty()) {
                String batchId = UUID.randomUUID().toString();
                execBatchAddress(page.items(), batchId)
                        .contextWrite(context -> context.put(MDC_TRACE_ID_KEY, "batch_id:" + batchId))
                        .block();
            } else {
                log.info("AddressManager - no batch address available");
            }
        } while (!CollectionUtils.isEmpty(lastEvaluatedKey));
        log.trace("AddressManager - batchAddress end");
    }

    private Page<BatchAddress> getBatchAddress(Map<String, AttributeValue> lastEvaluatedKey) {
        return batchAddressRepository.getBatchAddressByNotBatchId(lastEvaluatedKey, MAX_BATCH_REQUEST_SIZE)
                .blockOptional()
                .orElseThrow(() -> {
                    log.warn("BatchAddress - can not get batch address - DynamoDB Mono<Page> is null");
                    return new PnAddressManagerException("Error during get batch address", "Error during get batch address", HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_BATCH_ADDRESS);
                });

    }

    private Mono<Void> execBatchAddress(List<BatchAddress> items, String batchId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return Flux.fromStream(items.stream())
                .doOnNext(item -> {
                    item.setStatus(BatchStatus.WORKING.getValue());
                    item.setBatchId(batchId);
                    item.setLastReserved(now);
                })
                .flatMap(item -> batchAddressRepository.setNewBatchIdToBatchAddress(item)
                        .doOnError(ConditionalCheckFailedException.class,
                                e -> log.info("AddressManager - conditional check failed - skip correlationId: {} - addressId: {} - id: {}", item.getCorrelationId(),item.getAddressId(),item.getId(), e))
                        .onErrorResume(ConditionalCheckFailedException.class, e -> Mono.empty()))
                .collectList()
                .filter(requests -> !requests.isEmpty())
                .flatMap(requests -> {
                    log.info("AddressManager - batchId {} - calling with {} address", batchId, requests.size());
                    return createExcel(requests)
                            .onErrorResume(t -> incrementAndCheckRetry(requests, batchId).then(Mono.error(t)))
                            .thenReturn(requests);
                })
                .doOnNext(requests -> log.info("AddressManager - batchId {} - create excel and batch address size is: {}", batchId, requests.size()))
                .doOnError(e -> log.error("AddressManager - batchId {} - failed to execute batch", batchId, e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    private Mono<Void> createExcel(List<BatchAddress> requests) {
        List<AddressModel> addressModels = createAddressModelList(requests);
        Map<String , List<AddressModel>> addressModelsByCorrelationId = addressModels
                .stream()
                .collect(Collectors.groupingBy(AddressModel::getCorrelationId));
        addressModelsByCorrelationId.forEach((correlationId, addressModels1) -> csvService.createAddressCsvByCorrelationId(addressModels, correlationId));
        return Mono.empty();
    }

    private List<AddressModel> createAddressModelList(List<BatchAddress> batchAddresses){
        return batchAddresses.stream().map(batchAddress -> {
            AddressModel addressModel = new AddressModel();
            addressModel.setAddressId(batchAddress.getAddressId());
            addressModel.setCorrelationId(batchAddress.getCorrelationId());
            addressModel.setAddressId(batchAddress.getAddressId());
            addressModel.setAddressRow(batchAddress.getAddressRow());
            addressModel.setAddressRow2(batchAddress.getAddressRow2());
            addressModel.setPr(batchAddress.getPr());
            addressModel.setCity(batchAddress.getCity());
            addressModel.setCity2(batchAddress.getCity2());
            addressModel.setCap(batchAddress.getCap());
            addressModel.setCountry(batchAddress.getCountry());
            addressModel.setCxId(batchAddress.getCxId());
            return addressModel;
        }).collect(Collectors.toList());
    }

    private Mono<Void> incrementAndCheckRetry(List<BatchAddress> requests, String batchId) {
        return Flux.fromStream(requests.stream())
                .doOnNext(r -> {
                    int nextRetry = r.getRetry() != null ? r.getRetry() + 1 : 1;
                    r.setRetry(nextRetry);
                    if(nextRetry>3){
                        r.setStatus(BatchStatus.ERROR.getValue());
                    }
                })
                .flatMap(batchAddressRepository::update)
                .doOnNext(r -> log.debug("AddressManager - batchId {} - retry incremented for batchId: {}", batchId, r.getCorrelationId()))
                .doOnError(e -> log.warn("AddressManager - batchId {} - failed to increment retry", batchId, e)).then();
    }


    @Scheduled(fixedDelayString = "${pn.address.manager.batch.polling.delay}")
    public void batchPolling(){
        log.trace("AddressManager - batchPolling start");

        List<NormalizeItemsResultModel> results = csvService.readNormalizeItemsResultFromCsv();

        results.forEach(normalizeItemsResultModel -> {
            try {
                normalizeAddressService.sendEvents(normalizeItemsResultModel.getNormalizeItemsResult(),normalizeItemsResultModel.getCxId());
                //TO-DO update status to worked
            } catch (JsonProcessingException e) {
                throw new PnAddressManagerException("Error during send event", "Error during send event", HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_CODE_ADDRESS_MANAGER_BATCH_ADDRESS);
            }
        });

        log.trace("AddressManager - batchPolling end");
    }
}
