package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressOut;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.middleware.queue.producer.SqsSender;
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

    private DeduplicatesAddressService service;

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
        CsvService csv = mock(CsvService.class);
        AddressUtils addressUtils = new AddressUtils(csv, pnAddressManagerConfig, new ObjectMapper());
        AddressConverter addressConverter = new AddressConverter();
        CapAndCountryService capAndCountryService = new CapAndCountryService(capRepo, countryRepo, pnAddressManagerConfig);
        service = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig, apiKeyUtils, capAndCountryService, addressConverter, sqsSender);

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

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003"); // settato da verifyRequiredFields
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR001");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR002");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR999");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getResultDetails()).isEqualTo("RD01");
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getEqualityResult()).isTrue();
                    assertThat(resp.getResultDetails()).isNull();
                    assertThat(resp.getError()).isNull();
                    assertThat(resp.getNormalizedAddress()).isNotNull();
                })
                .verifyComplete();

        // ArgumentCaptor for sendRequestEvent and sendResponseEvent
        ArgumentCaptor<DeduplicaRequest> requestCaptor = ArgumentCaptor.forClass(DeduplicaRequest.class);
        ArgumentCaptor<DeduplicaResponse> responseCaptor = ArgumentCaptor.forClass(DeduplicaResponse.class);

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(responseCaptor.capture(), eq(CORR_ID));

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
        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(requestCaptor.capture(), eq(CORR_ID));
        // Response event must NOT be sent
        verify(sqsSender, never()).pushDeduplicaResponseEvent(any(), any());
    }

    @Test
    void pushDeduplicaRequestEventThrows() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        doThrow(new RuntimeException("SQS push request failed"))
                .when(sqsSender).pushDeduplicaRequestEvent(any(), any());

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS push request failed"))
                .verify();

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any(), eq(CORR_ID));
        verify(sqsSender, times(0)).pushDeduplicaResponseEvent(any(), eq(CORR_ID));
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
                .when(sqsSender).pushDeduplicaResponseEvent(any(), any());
        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("SQS push response failed"))
                .verify();

        verify(sqsSender, times(1)).pushDeduplicaRequestEvent(any(), eq(CORR_ID));
        verify(sqsSender, times(1)).pushDeduplicaResponseEvent(any(), eq(CORR_ID));
    }
}