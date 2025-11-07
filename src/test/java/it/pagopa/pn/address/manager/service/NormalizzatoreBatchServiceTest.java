package it.pagopa.pn.address.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.exception.PnFileNotFoundException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.middleware.client.safestorage.UploadDownloadClient;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.repository.PostelBatchRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadInfo;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.FileDownloadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.PNADDR003_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalizzatoreBatchServiceTest {

    @Mock
    private AddressBatchRequestRepository addressBatchRequestRepository;

    @Mock
    private PostelBatchRepository postelBatchRepository;

    @Mock
    private CsvService csvService;

    @Mock
    private UploadDownloadClient uploadDownloadClient;

    @Mock
    private PnRequestService pnRequestService;

    @Mock
    private SafeStorageService safeStorageService;

    @Mock
    private SqsService sqsService;

    @Mock
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Mock
    private CapRepository capRepo;

    @Mock
    private CountryRepository countryRepo;

    private ObjectMapper objectMapper;

    private Clock clock;
    private AddressUtils addressUtils;
    private CapAndCountryService capAndCountryService;

    private NormalizzatoreBatchService service;

    private static final String BATCH_ID = "batch-123";
    private static final String FILE_KEY = "file-key-abc";
    private static final String CORRELATION_ID = "corr-999";
    private static final String DOWNLOAD_URL = "https://test.download.url";
    private static final String CX_ID = "cx-123";
    private static final String CREATED_AT = "2025-01-15T10:00:00Z";

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        clock = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneId.systemDefault());

        // Config di base come nel test di DeduplicatesAddressService
        lenient().when(pnAddressManagerConfig.getEnableValidation()).thenReturn(false);
        lenient().when(pnAddressManagerConfig.getValidationPattern()).thenReturn("A-Za-z0-9\\s");
        lenient().when(pnAddressManagerConfig.getForeignValidationMode()).thenReturn(null);
        lenient().when(pnAddressManagerConfig.getForeignValidationPattern()).thenReturn(null);
        lenient().when(pnAddressManagerConfig.getFlagCsv()).thenReturn(false);
        lenient().when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);
        lenient().when(pnAddressManagerConfig.getPagoPaCxId()).thenReturn(CX_ID);

        // Creo istanze reali invece di mock
        addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, new ObjectMapper());
        capAndCountryService = new CapAndCountryService(capRepo, countryRepo, pnAddressManagerConfig);

        service = new NormalizzatoreBatchService(
                addressBatchRequestRepository,
                postelBatchRepository,
                csvService,
                addressUtils,
                uploadDownloadClient,
                pnRequestService,
                capAndCountryService,
                clock,
                safeStorageService,
                pnAddressManagerConfig
        );
    }

    private NormalizzatoreBatch createBatch(String batchId, String status) {
        NormalizzatoreBatch batch = new NormalizzatoreBatch();
        batch.setBatchId(batchId);
        batch.setStatus(status);
        batch.setOutputFileKey(FILE_KEY);
        return batch;
    }

    private PnRequest createPnRequest(String correlationId, String status, LocalDateTime createdAt) {
        PnRequest request = new PnRequest();
        request.setCorrelationId(correlationId);
        request.setStatus(status);
        request.setCreatedAt(createdAt);
        request.setBatchId(BATCH_ID);
        request.setMessage("{}");
        return request;
    }

    private NormalizedAddress createNormalizedAddress(String id, String address) {
        NormalizedAddress na = new NormalizedAddress();
        na.setId(id);
        na.setSViaCompletaSpedizione(address);
        na.setSCap("00100");
        na.setSComuneSpedizione("Roma");
        na.setSSiglaProv("RM");
        return na;
    }

    private FileDownloadResponse createFileDownloadResponse() {
        FileDownloadResponse response = new FileDownloadResponse();
        FileDownloadInfo downloadInfo = new FileDownloadInfo();
        downloadInfo.setUrl(DOWNLOAD_URL);
        response.setDownload(downloadInfo);
        return response;
    }

    @Test
    void resetRelatedBatchRequestForRetryTest(){
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        when(postelBatchRepository.update(any(NormalizzatoreBatch.class))).thenReturn(Mono.just(batch));
        PnRequest pnRequest = new PnRequest();
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(), anyString(), any()))
                .thenReturn(Mono.just(Page.create(List.of(pnRequest))));
        ArgumentCaptor<List<PnRequest>> captor = ArgumentCaptor.forClass(List.class);
        when(pnRequestService.incrementAndCheckRetry(anyList(), any(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(service.resetRelatedBatchRequestForRetry(batch))
                .verifyComplete();

        verify(pnRequestService).incrementAndCheckRetry(captor.capture(), any(), anyString());
        List<PnRequest> requestsToReset = captor.getValue();
        assertThat(requestsToReset.size()).isEqualTo(1);
        PnRequest request = requestsToReset.get(0);
        assertThat(request.getStatus()).isEqualTo(BatchStatus.TAKEN_CHARGE.name());
    }

    @Test
    void getFileFailed(){
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());

        when(safeStorageService.getFile(FILE_KEY, CX_ID))
                .thenReturn(Mono.error(new PnFileNotFoundException("File not found", mock(Throwable.class))));
        when(pnRequestService.incrementAndCheckRetry(eq(batch), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(pnRequestService).incrementAndCheckRetry(eq(batch), any());
        verifyNoInteractions(uploadDownloadClient);
    }

    @Test
    void downloadCsvFailed(){
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL))
                .thenReturn(Mono.error(new RuntimeException("Download failed")));
        when(pnRequestService.incrementAndCheckRetry(eq(batch), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(pnRequestService, times(2)).incrementAndCheckRetry(eq(batch), any());
    }

    @Test
    void readItemFromCsvFailed(){
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();
        byte[] csvBytes = "test,csv,data".getBytes();

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL)).thenReturn(Mono.just(csvBytes));
        when(csvService.readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0)))
                .thenThrow(new RuntimeException("error during read items from csv"));
        when(pnRequestService.incrementAndCheckRetry(eq(batch), any())).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(csvService).readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0));
        verify(pnRequestService).incrementAndCheckRetry(eq(batch), any());
    }

    @Test
    void getResponseKoMissingRequiredFieldsIt() throws JsonProcessingException {
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();
        byte[] csvBytes = "test,csv,data".getBytes();
        LocalDateTime createdAt = LocalDateTime.now();

        NormalizedAddress address = createNormalizedAddress(CORRELATION_ID + "#" + createdAt + "#0", "Via Roma 1");
        address.setSCap("");
        List<NormalizedAddress> addressList = Collections.singletonList(address);

        PnRequest request = createPnRequest(CORRELATION_ID, BatchStatus.WORKING.name(), createdAt);
        List<NormalizeRequest> list = new ArrayList<>();
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        list.add(normalizeRequest);
        request.setAddresses(objectMapper.writeValueAsString(list));

        Page<PnRequest> page = Page.create(Collections.singletonList(request), null);

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL)).thenReturn(Mono.just(csvBytes));
        when(csvService.readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0)))
                .thenReturn(addressList);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(), eq(BATCH_ID), eq(BatchStatus.WORKING)))
                .thenReturn(Mono.just(page));
        ArgumentCaptor<List<PnRequest>> captor = ArgumentCaptor.forClass(List.class);
        when(pnRequestService.updateBatchRequest(anyList(), eq(BATCH_ID))).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(csvService).readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0));
        verify(pnRequestService).updateBatchRequest(captor.capture(), eq(BATCH_ID));
        List<PnRequest> updatedRequests = captor.getValue();
        assertThat(updatedRequests).hasSize(1);
        PnRequest updatedRequest = updatedRequests.get(0);
        assertThat(updatedRequest.getStatus()).isEqualTo(BatchStatus.ERROR.name());
        assertThat(updatedRequest.getMessage()).isEqualTo(PNADDR003_MESSAGE);
        verifyNoInteractions(capRepo);
    }

    @Test
    void getResponseKoMissingRequiredFieldsForeign() throws JsonProcessingException {
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();
        byte[] csvBytes = "test,csv,data".getBytes();
        LocalDateTime createdAt = LocalDateTime.now();

        NormalizedAddress address = createNormalizedAddress(CORRELATION_ID + "#" + createdAt + "#0", "Via Roma 1");
        address.setSCap("");
        address.setSComuneSpedizione(null);
        address.setSStatoSpedizione("FRANCIA");
        List<NormalizedAddress> addressList = Collections.singletonList(address);

        PnRequest request = createPnRequest(CORRELATION_ID, BatchStatus.WORKING.name(), createdAt);
        List<NormalizeRequest> list = new ArrayList<>();
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        list.add(normalizeRequest);
        request.setAddresses(objectMapper.writeValueAsString(list));

        Page<PnRequest> page = Page.create(Collections.singletonList(request), null);

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL)).thenReturn(Mono.just(csvBytes));
        when(csvService.readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0)))
                .thenReturn(addressList);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(), eq(BATCH_ID), eq(BatchStatus.WORKING)))
                .thenReturn(Mono.just(page));
        ArgumentCaptor<List<PnRequest>> captor = ArgumentCaptor.forClass(List.class);
        when(pnRequestService.updateBatchRequest(anyList(), eq(BATCH_ID))).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(csvService).readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0));
        verify(pnRequestService).updateBatchRequest(captor.capture(), eq(BATCH_ID));
        List<PnRequest> updatedRequests = captor.getValue();
        assertThat(updatedRequests).hasSize(1);
        PnRequest updatedRequest = updatedRequests.get(0);
        assertThat(updatedRequest.getStatus()).isEqualTo(BatchStatus.ERROR.name());
        assertThat(updatedRequest.getMessage()).isEqualTo(PNADDR003_MESSAGE);
        verifyNoInteractions(countryRepo);
    }

    @Test
    void getResponseKoCorrelationIdNotComplete() throws JsonProcessingException {
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();
        byte[] csvBytes = "test,csv,data".getBytes();
        LocalDateTime createdAt = LocalDateTime.now();

        NormalizedAddress address = createNormalizedAddress(CORRELATION_ID + "#" + createdAt + "#0", "Via Roma 1");
        List<NormalizedAddress> addressList = Collections.singletonList(address);

        PnRequest request = createPnRequest(CORRELATION_ID, BatchStatus.WORKING.name(), createdAt);
        List<NormalizeRequest> list = new ArrayList<>();
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        list.add(normalizeRequest);
        list.add(normalizeRequest);
        request.setAddresses(objectMapper.writeValueAsString(list));

        Page<PnRequest> page = Page.create(Collections.singletonList(request), null);

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL)).thenReturn(Mono.just(csvBytes));
        when(csvService.readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0)))
                .thenReturn(addressList);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(), eq(BATCH_ID), eq(BatchStatus.WORKING)))
                .thenReturn(Mono.just(page));
        ArgumentCaptor<List<PnRequest>> captor = ArgumentCaptor.forClass(List.class);
        when(pnRequestService.updateBatchRequest(anyList(), eq(BATCH_ID))).thenReturn(Mono.empty());

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(csvService).readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0));
        verify(pnRequestService).updateBatchRequest(captor.capture(), eq(BATCH_ID));
        List<PnRequest> updatedRequests = captor.getValue();
        assertThat(updatedRequests).hasSize(1);
        PnRequest updatedRequest = updatedRequests.get(0);
        assertThat(updatedRequest.getStatus()).isEqualTo(BatchStatus.TAKEN_CHARGE.name());
        verifyNoInteractions(countryRepo);
    }


    @Test
    void getResponseOk() throws JsonProcessingException {
        NormalizzatoreBatch batch = createBatch(BATCH_ID, BatchStatus.WORKING.name());
        FileDownloadResponse fileResponse = createFileDownloadResponse();
        byte[] csvBytes = "test,csv,data".getBytes();
        LocalDateTime createdAt = LocalDateTime.now();

        NormalizedAddress address = createNormalizedAddress(CORRELATION_ID + "#" + createdAt + "#0", "Via Roma 1");
        List<NormalizedAddress> addressList = Collections.singletonList(address);

        PnRequest request = createPnRequest(CORRELATION_ID, BatchStatus.WORKING.name(), createdAt);
        List<NormalizeRequest> list = new ArrayList<>();
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        list.add(normalizeRequest);
        request.setAddresses(objectMapper.writeValueAsString(list));

        Page<PnRequest> page = Page.create(Collections.singletonList(request), null);

        when(safeStorageService.getFile(FILE_KEY, CX_ID)).thenReturn(Mono.just(fileResponse));
        when(uploadDownloadClient.downloadContent(DOWNLOAD_URL)).thenReturn(Mono.just(csvBytes));
        when(csvService.readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0)))
                .thenReturn(addressList);
        when(addressBatchRequestRepository.getBatchRequestByBatchIdAndStatus(any(), eq(BATCH_ID), eq(BatchStatus.WORKING)))
                .thenReturn(Mono.just(page));
        ArgumentCaptor<List<PnRequest>> captor = ArgumentCaptor.forClass(List.class);
        when(pnRequestService.updateBatchRequest(anyList(), eq(BATCH_ID))).thenReturn(Mono.empty());
        CapModel capModel = new CapModel();
        capModel.setCap("00100");
        when(capRepo.findValidCap(anyString())).thenReturn(Mono.just(capModel));

        StepVerifier.create(service.getResponse(FILE_KEY, batch))
                .verifyComplete();

        verify(safeStorageService).getFile(FILE_KEY, CX_ID);
        verify(uploadDownloadClient).downloadContent(DOWNLOAD_URL);
        verify(csvService).readItemsFromCsv(eq(NormalizedAddress.class), eq(csvBytes), eq(0));
        verify(pnRequestService).updateBatchRequest(captor.capture(), eq(BATCH_ID));
        List<PnRequest> updatedRequests = captor.getValue();
        assertThat(updatedRequests).hasSize(1);
        PnRequest updatedRequest = updatedRequests.get(0);
        assertThat(updatedRequest.getStatus()).isEqualTo(BatchStatus.WORKED.name());
        assertThat(updatedRequest.getMessage()).isNotBlank();
    }
}