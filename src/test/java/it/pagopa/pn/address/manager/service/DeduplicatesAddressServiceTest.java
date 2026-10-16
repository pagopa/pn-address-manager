package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.AddressOut;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.DeduplicaRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.DeduplicaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.middleware.queue.producer.SqsSender;
import it.pagopa.pn.address.manager.repository.AddressBatchRequestRepository;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.DEDUPLICATE_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeduplicatesAddressServiceTest {

    @Mock
    private DeduplicaClient postelClient;

    @Mock
    private ApiKeyUtils apiKeyUtils;

    @Mock
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Mock
    private CapRepository capRepo;

    @Mock
    private CountryRepository countryRepo;

    @Mock
    private SqsSender sqsSender;

    @Mock
    private AddressBatchRequestRepository addressBatchRequestRepository;

    private DeduplicatesAddressService service;
    private AddressUtils addressUtils;

    // valori comodi per i test
    private static final String CXID = "cx-123";
    private static final String X_API_KEY = "key-abc";
    private static final String CORR_ID = "corr-999";

    @BeforeEach
    void setup() {
        // Config di base: disabilito validazione caratteri, flagCsv lo setto per test
        lenient().when(pnAddressManagerConfig.getEnableValidation()).thenReturn(false);
        lenient().when(pnAddressManagerConfig.getValidationPattern()).thenReturn("A-Za-z0-9\\s"); // char-class
        lenient().when(pnAddressManagerConfig.getForeignValidationMode()).thenReturn(null);
        lenient().when(pnAddressManagerConfig.getForeignValidationPattern()).thenReturn(null);
        lenient().when(pnAddressManagerConfig.getFlagCsv()).thenReturn(false);
        lenient().when(pnAddressManagerConfig.getEnableWhitelisting()).thenReturn(true);

        PnAddressManagerConfig.Deduplicate deduplicate = mock(PnAddressManagerConfig.Deduplicate.class);
        lenient().when(pnAddressManagerConfig.getDeduplicate()).thenReturn(deduplicate);
        lenient().when(deduplicate.getTtl()).thenReturn(Duration.ofMinutes(10));

        CsvService csv = mock(CsvService.class);
        addressUtils = new AddressUtils(csv, pnAddressManagerConfig, new ObjectMapper());
        AddressConverter addressConverter = new AddressConverter();
        CapAndCountryService capAndCountryService = new CapAndCountryService(capRepo, countryRepo, pnAddressManagerConfig);
        service = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig, apiKeyUtils, capAndCountryService, addressConverter, sqsSender, addressBatchRequestRepository);

        // apiKey ok di default
        when(apiKeyUtils.checkApiKey(eq(CXID), eq(X_API_KEY))).thenReturn(Mono.just(new ApiKeyModel()));
    }

    private static AnalogAddress analogIt(String via, String cap, String city, String pr) {
        AnalogAddress a = new AnalogAddress();
        a.setAddressRow(via);
        a.setCap(cap);
        a.setCity(city);
        a.setPr(pr);
        return a;
    }

    private static AddressOut addressOutIt(String via, String cap, String city, String pr) {
        AddressOut a = new AddressOut();
        a.setsCap(cap);
        a.setsComuneSpedizione(city);
        a.setsViaCompletaSpedizione(via);
        a.setsSiglaProv(pr);
        a.setId(CORR_ID);
        return a;
    }

    private static AddressOut addressOutForeign(String via, String country, String city, String pr) {
        AddressOut a = new AddressOut();
        a.setsComuneSpedizione(city);
        a.setsViaCompletaSpedizione(via);
        a.setsStatoSpedizione(country);
        a.setId(CORR_ID);
        return a;
    }

    private static AnalogAddress analogForeign(String via, String city, String country) {
        AnalogAddress a = new AnalogAddress();
        a.setAddressRow(via);
        a.setCity(city);
        a.setCountry(country);
        return a;
    }

    private static DeduplicatesRequest makeReq(AnalogAddress base, AnalogAddress target, String corrId) {
        DeduplicatesRequest r = new DeduplicatesRequest();
        r.setBaseAddress(base);
        r.setTargetAddress(target);
        r.setCorrelationId(corrId);
        return r;
    }

    private void assertPersistedPnRequest(DeduplicatesRequest request, DeduplicatesResponse response, DeduplicaResponse postelResponse) {
        ArgumentCaptor<PnRequest> pnRequestCaptor = ArgumentCaptor.forClass(PnRequest.class);
        verify(addressBatchRequestRepository, times(1)).create(pnRequestCaptor.capture());

        PnRequest saved = pnRequestCaptor.getValue();
        assertThat(saved.getCorrelationId()).isEqualTo(CORR_ID);
        assertThat(saved.getClientId()).isEqualTo(CXID);
        assertThat(saved.getService()).isEqualTo(DEDUPLICATE_SERVICE);
        assertThat(saved.getStatus()).isEqualTo(BatchStatus.WORKED.name());
        assertThat(saved.getAddresses()).isEqualTo(addressUtils.toJson(request));
        assertThat(saved.getMessage()).isEqualTo(addressUtils.toJson(response));
        assertThat(saved.getBatchId()).isNull();
        assertThat(saved.getRetry()).isNull();
        assertThat(saved.getLastReserved()).isNull();
        assertThat(saved.getSendStatus()).isNull();
        assertThat(saved.getAwsMessageId()).isNull();

        if (postelResponse == null) {
            assertThat(saved.getPostelResponseCodes()).isNull();
            return;
        }

        assertThat(saved.getPostelResponseCodes()).hasSize(1);
        var codes = saved.getPostelResponseCodes().getFirst();
        assertThat(codes.getId()).isNull();
        assertThat(codes.getPostelResponseCodeRecipient()).isNotNull();

        AddressOut slaveOut = postelResponse.getSlaveOut();
        Integer expectedFPostalizzabile = slaveOut.getfPostalizzabile() == null || slaveOut.getfPostalizzabile().isBlank()
                ? null
                : Integer.valueOf(slaveOut.getfPostalizzabile().trim());

        assertThat(codes.getPostelResponseCodeRecipient().getFPostalizzabile()).isEqualTo(expectedFPostalizzabile);
        assertThat(codes.getPostelResponseCodeRecipient().getNRisultatoNorm()).isEqualTo(slaveOut.getnRisultatoNorm());
        assertThat(codes.getPostelResponseCodeRecipient().getNErroreNorm()).isEqualTo(slaveOut.getnErroreNorm());
    }


    @Test
    void deduplicaErrorPNADDR003() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003"); // settato da verifyRequiredFields
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorPNADDR003ForeignAddress() {
        AnalogAddress base = analogForeign("Via Roma 1", "Roma", "FRANCIA");
        AnalogAddress tgt = analogForeign("Via Roma 1", "Roma", "FRANCIA");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutForeign("Via Roma 1", "FRANCIA", "", null);
        AddressOut master = addressOutForeign("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003");
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verifyNoInteractions(countryRepo);
    }

    @Test
    void deduplicaErrorPNADDR001() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("0");
        slave.setnErroreNorm(19);

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR001");
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorPNADDR002() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any()))
                .thenReturn(Mono.error(new PnInternalException("","")));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR002");
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap(any());
    }

    @Test
    void deduplicaErrorPNADDR009() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        AddressOut slave = addressOutIt("Via Roma 1", "", "Roma", "RM");
        slave.setfPostalizzabile("0");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);
        postelResp.setErrore("DED998");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR999");
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorErrorRisultatoDEDU() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);
        postelResp.setErrore("DED001");

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any()))
                .thenReturn(Mono.just(capModel));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getResultDetails()).isEqualTo("RD01");
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap(any());
    }

    @Test
    void deduplicaOk() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any()))
                .thenReturn(Mono.just(capModel));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getResultDetails()).isNull();
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture());

        DeduplicaRequest requestEvent = requestCaptor.getValue();
        DeduplicaResponse responseEvent = responseCaptor.getValue();

        // Extract masterIn, slaveIn from requestEvent
        assertThat(requestEvent.getMasterIn()).isNotNull();
        assertThat(requestEvent.getSlaveIn()).isNotNull();
        // Extract masterOut, slaveOut from responseEvent
        assertThat(responseEvent.getMasterOut()).isNotNull();
        assertThat(responseEvent.getSlaveOut()).isNotNull();

        assertEquals(requestEvent.getMasterIn().getId(), responseEvent.getMasterOut().getId());
        assertEquals(requestEvent.getSlaveIn().getId(), responseEvent.getSlaveOut().getId());

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap(any());
    }

    @Test
    void deduplicaWithEqualityResultFalseShouldSetPNADDR003WhenNormalizedAddressMatchesMasterOut() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Milano 2", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt(" via   roma 1 ", "00100", " roma ", "rm");
        slave.setsCivicoAltro(" scala  A ");
        slave.setsFrazioneSpedizione(" centro storico ");

        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        master.setsCivicoAltro("Scala A");
        master.setsFrazioneSpedizione("Centro   Storico");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(false);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any()))
                .thenReturn(Mono.just(capModel));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getError()).isEqualTo("PNADDR003");
                    assertThat(resp.getNormalizedAddress()).isNull();
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap("00100");
        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(any());
    }

    @Test
    void deduplicaWithEqualityResultFalseShouldKeepNormalizedAddressWhenItDoesNotMatchMasterOut() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Milano 2", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Milano 2", "00100", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(false);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any()))
                .thenReturn(Mono.just(capModel));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getEqualityResult()).isFalse();
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                    assertThat(resp.getNormalizedAddress().getAddressRow()).isEqualTo("Via Milano 2");
                    assertPersistedPnRequest(req, resp, postelResp);
                })
                .verifyComplete();

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap("00100");
        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(any());
    }

    @Test
    void deduplicaOkWithFlagCsv() {
        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(true);
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getResultDetails()).isNull();
                    assertThat(resp.getError()).isEqualTo("Invalid Address, Cap, City and Province: [00100,Roma,RM]");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        verifyNoInteractions(sqsSender);
    }

    @Test
    void callDeduplicaFromPostelThrows() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        // Simulate Postel API failure
        when(postelClient.deduplica(any())).thenReturn(Mono.error(new RuntimeException("Postel API failure")));

        // Verify request event sent, response event NOT sent
        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .expectError(RuntimeException.class)
                .verify();

        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        // Request event must be sent
        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture());
        // Response event must NOT be sent
        verify(sqsSender, never()).pushDeduplicaResponseEvent(any());
    }

    @Test
    void pushDeduplicaRequestEventThrows() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        doThrow(new RuntimeException("SQS push request failed"))
                .when(sqsSender).pushDeduplicaRequestEvent(any());

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS push request failed"))
                .verify();

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, times(0)).pushDeduplicaResponseEvent(any());
    }

    @Test
    void pushDeduplicaResponseEventThrows() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        doThrow(new RuntimeException("SQS push response failed"))
                .when(sqsSender).pushDeduplicaResponseEvent(any());
        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS push response failed"))
                .verify();

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(any());
    }

    @Test
    void deduplicaWithoutCallingPostelBothEmptyStrings() {

        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(false);
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("", "00100", "", "RM");

        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .assertNext(response -> {
                    assertThat(response.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(response.getEqualityResult()).isFalse();
                    assertThat(response.getError()).isEqualTo("PNADDR001");
                    assertThat(response.getResultDetails()).isEqualTo("RD04");
                    assertPersistedPnRequest(request, response, null);
                })
                .verifyComplete();

        verifyNoInteractions(postelClient);
    }

    @Test
    void deduplicaWithoutCallingPostelBothNull() {

        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(false);
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt(null, "00100", null, "RM");

        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .assertNext(response -> {
                    assertThat(response.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(response.getEqualityResult()).isFalse();
                    assertThat(response.getError()).isEqualTo("PNADDR001");
                    assertThat(response.getResultDetails()).isEqualTo("RD04");
                    assertPersistedPnRequest(request, response, null);
                })
                .verifyComplete();

        verifyNoInteractions(postelClient);
    }

    @Test
    void deduplicaWithoutCallingPostelORCondition() {

        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(false);
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("Via Roma", "00100", null, "RM");

        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .assertNext(response -> {
                    assertThat(response.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(response.getEqualityResult()).isFalse();
                    assertThat(response.getError()).isEqualTo("PNADDR001");
                    assertThat(response.getResultDetails()).isEqualTo("RD04");
                    assertPersistedPnRequest(request, response, null);
                })
                .verifyComplete();

        verifyNoInteractions(postelClient);
    }

    @Test
    void deduplicates_whenTargetHasNoMinimumRequiredFields_shouldPersistCompletePnRequestWithoutPostelResponseCodes() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("", "00100", "", "RM");
        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        AtomicReference<DeduplicatesResponse> returnedResponse = new AtomicReference<>();
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .assertNext(response -> {
                    returnedResponse.set(response);
                    assertThat(response.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(response.getEqualityResult()).isFalse();
                    assertThat(response.getError()).isEqualTo("PNADDR001");
                    assertThat(response.getResultDetails()).isEqualTo("RD04");
                })
                .verifyComplete();

        verifyNoInteractions(postelClient);
        assertPersistedPnRequest(request, returnedResponse.get(), null);
    }


    @Test
    void deduplicates_whenPostelReturnsResponse_shouldPersistCompletePnRequestWithPostelResponseCodes() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");
        slave.setnRisultatoNorm(10);
        slave.setnErroreNorm(20);

        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any())).thenReturn(Mono.just(capModel));

        AtomicReference<DeduplicatesResponse> returnedResponse = new AtomicReference<>();

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .assertNext(resp -> {
                    returnedResponse.set(resp);
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                })
                .verifyComplete();

        verify(postelClient, times(1)).deduplica(any());
        assertPersistedPnRequest(request, returnedResponse.get(), postelResp);
    }

    @Test
    void deduplicates_whenPostelReturnsResponseAndFPostalizzabileIsEmpty_shouldPersistFPostalizzabileNull() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("");
        slave.setnRisultatoNorm(10);
        slave.setnErroreNorm(20);

        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        PnAddressManagerConfig.Deduplicate deduplicate = mock(PnAddressManagerConfig.Deduplicate.class);
        when(pnAddressManagerConfig.getDeduplicate()).thenReturn(deduplicate);
        when(deduplicate.getTtl()).thenReturn(Duration.ofMinutes(10));
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any())).thenReturn(Mono.just(capModel));

        AtomicReference<DeduplicatesResponse> returnedResponse = new AtomicReference<>();

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    returnedResponse.set(resp);
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getError()).isNull();
                })
                .verifyComplete();

        assertPersistedPnRequest(req, returnedResponse.get(), postelResp);
    }

    @Test
    void deduplicates_shouldPersistReturnedResponseInMessageField() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");
        when(addressBatchRequestRepository.create(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any())).thenReturn(Mono.just(capModel));

        AtomicReference<DeduplicatesResponse> returnedResponse = new AtomicReference<>();

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    returnedResponse.set(resp);
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                })
                .verifyComplete();

        assertPersistedPnRequest(req, returnedResponse.get(), postelResp);
    }

    @Test
    void deduplicates_whenPersistenceFails_shouldPropagateError() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("", "00100", "", "RM");
        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        when(addressBatchRequestRepository.create(any(PnRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Persistence failed")));

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Persistence failed"))
                .verify();

        verifyNoInteractions(postelClient);
        verify(addressBatchRequestRepository, times(1)).create(any(PnRequest.class));
    }

    @Test
    void deduplicates_whenPersistenceFailsAfterPostel_shouldPropagateError() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");
        AddressOut master = addressOutIt("Via Roma 1", "00100", "Roma", "RM");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setMasterOut(master);
        postelResp.setRisultatoDedu(true);

        CapModel capModel = new CapModel();
        capModel.setCap("00100");

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));
        when(capRepo.findValidCap(any())).thenReturn(Mono.just(capModel));
        when(addressBatchRequestRepository.create(any(PnRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Persistence failed after postel")));

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Persistence failed after postel"))
                .verify();

        verify(postelClient, times(1)).deduplica(any());
        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(any());
        verify(addressBatchRequestRepository, times(1)).create(any(PnRequest.class));
    }

    @Test
    void deduplicates_whenPostelFails_shouldNotPersistPnRequest_showingCurrentGapVsSpecification() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress target = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest request = makeReq(base, target, CORR_ID);

        when(postelClient.deduplica(any()))
                .thenReturn(Mono.error(new RuntimeException("Postel API failure")));

        StepVerifier.create(service.deduplicates(request, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Postel API failure"))
                .verify();

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any());
        verify(sqsSender, never()).pushDeduplicaResponseEvent(any());
        verify(addressBatchRequestRepository, never()).create(any(PnRequest.class));
    }



}