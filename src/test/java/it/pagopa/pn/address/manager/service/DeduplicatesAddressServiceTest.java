package it.pagopa.pn.address.manager.service;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressOut;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.entity.CapModel;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.middleware.client.DeduplicaClient;
import it.pagopa.pn.address.manager.repository.CapRepository;
import it.pagopa.pn.address.manager.repository.CountryRepository;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
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
        service = new DeduplicatesAddressService(addressUtils, postelClient, pnAddressManagerConfig, apiKeyUtils, capAndCountryService, addressConverter);

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
        return a;
    }

    private static AddressOut addressOutForeign(String via, String country, String city, String pr) {
        AddressOut a = new AddressOut();
        a.setsComuneSpedizione(city);
        a.setsViaCompletaSpedizione(via);
        a.setsStatoSpedizione(country);
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
        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003"); // settato da verifyRequiredFields
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorPNADDR003ForeignAddress() {
        AnalogAddress base = analogForeign("Via Roma 1", "Roma", "FRANCIA");
        AnalogAddress tgt = analogForeign("Via Roma 1", "Roma", "FRANCIA");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutForeign("Via Roma 1", "FRANCIA", "", null);

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR003");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        verify(postelClient).deduplica(any());
        verifyNoInteractions(countryRepo);
    }

    @Test
    void deduplicaErrorPNADDR001() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "", "Roma", "RM");
        slave.setfPostalizzabile("0");
        slave.setnErroreNorm(19);

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
        postelResp.setRisultatoDedu(true);

        when(postelClient.deduplica(any())).thenReturn(Mono.just(postelResp));

        StepVerifier.create(service.deduplicates(req, CXID, X_API_KEY))
                .assertNext(resp -> {
                    assertThat(resp.getCorrelationId()).isEqualTo(CORR_ID);
                    assertThat(resp.getError()).isEqualTo("PNADDR001");
                    assertThat(resp.getNormalizedAddress()).isNull();
                })
                .verifyComplete();

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorPNADDR002() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
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

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap(any());
    }

    @Test
    void deduplicaErrorPNADDR009() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "", "Roma", "RM");
        slave.setfPostalizzabile("0");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
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

        verify(postelClient).deduplica(any());
        verifyNoInteractions(capRepo);
    }

    @Test
    void deduplicaErrorErrorRisultatoDEDU() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);
        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        slave.setfPostalizzabile("1");

        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
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

        verify(postelClient).deduplica(any());
        verify(capRepo).findValidCap(any());
    }

    @Test
    void deduplicaOk() {
        AnalogAddress base = analogIt("Via Roma 1", "00100", "Roma", "RM");
        AnalogAddress tgt = analogIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicatesRequest req = makeReq(base, tgt, CORR_ID);

        AddressOut slave = addressOutIt("Via Roma 1", "00100", "Roma", "RM");
        DeduplicaResponse postelResp = new DeduplicaResponse();
        postelResp.setSlaveOut(slave);
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
    }
}