package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.AddressOut;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeSyncRequest;
import it.pagopa.pn.address.manager.middleware.client.NormalizeSyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormalizeSyncServiceTest {

    private static final String CX_ID = "cx-123";
    private static final String API_KEY = "api-key-123";

    @Mock
    private NormalizeSyncClient normalizeSyncClient;

    @Mock
    private ApiKeyUtils apiKeyUtils;

    private NormalizeSyncService service;

    @BeforeEach
    void setUp() {
        service = new NormalizeSyncService(normalizeSyncClient, new AddressConverter(), apiKeyUtils);
    }

    @Test
    void normalizeSyncShouldProxyRequestItemAndMapResponse() {
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("Via Roma 1");
        analogAddress.setAddressRow2("Scala A");
        analogAddress.setCap("00100");
        analogAddress.setCity("Roma");
        analogAddress.setCity2("Centro");
        analogAddress.setPr("RM");
        analogAddress.setCountry("ITALIA");

        NormalizeSyncRequest request = new NormalizeSyncRequest();
        request.setCorrelationId("corr-1");
        request.setAddress(analogAddress);

        NormalizzazioneSyncResponse postelResponse =
                new NormalizzazioneSyncResponse();
        AddressOut addressOut = new AddressOut();
        addressOut.setId("addr-1");
        addressOut.setsViaCompletaSpedizione("VIA ROMA 1");
        addressOut.setsCivicoAltro("SCALA A");
        addressOut.setsCap("00100");
        addressOut.setsComuneSpedizione("ROMA");
        addressOut.setsFrazioneSpedizione("CENTRO");
        addressOut.setsSiglaProv("RM");
        addressOut.setsStatoSpedizione("ITALIA");
        postelResponse.setAddressOut(addressOut);

        when(apiKeyUtils.checkApiKey(eq(CX_ID), eq(API_KEY))).thenReturn(Mono.just(new ApiKeyModel()));
        when(normalizeSyncClient.normalizzazioneSync(eq(CX_ID), eq(API_KEY), any()))
                .thenReturn(Mono.just(postelResponse));

        StepVerifier.create(service.normalizeSync(CX_ID, API_KEY, request))
                .assertNext(response -> {
                    assertThat(response.getCorrelationId()).isEqualTo("corr-1");
                    assertThat(response.getError()).isNull();
                    assertThat(response.getNormalizedAddress()).isNotNull();
                    assertThat(response.getNormalizedAddress().getAddressRow()).isEqualTo("VIA ROMA 1");
                    assertThat(response.getNormalizedAddress().getAddressRow2()).isEqualTo("SCALA A");
                    assertThat(response.getNormalizedAddress().getCap()).isEqualTo("00100");
                    assertThat(response.getNormalizedAddress().getCity()).isEqualTo("ROMA");
                    assertThat(response.getNormalizedAddress().getCity2()).isEqualTo("CENTRO");
                    assertThat(response.getNormalizedAddress().getPr()).isEqualTo("RM");
                    assertThat(response.getNormalizedAddress().getCountry()).isEqualTo("ITALIA");
                })
                .verifyComplete();

        ArgumentCaptor<NormalizzazioneSyncRequest> captor =
                ArgumentCaptor.forClass(NormalizzazioneSyncRequest.class);
        verify(normalizeSyncClient).normalizzazioneSync(eq(CX_ID), eq(API_KEY), captor.capture());

        assertThat(captor.getValue().getAddressIn()).isNotNull();
        assertThat(captor.getValue().getAddressIn().getId()).isEqualTo("corr-1");
        assertThat(captor.getValue().getAddressIn().getIndirizzo()).isEqualTo("Via Roma 1");
        assertThat(captor.getValue().getAddressIn().getIndirizzoAggiuntivo()).isEqualTo("Scala A");
        assertThat(captor.getValue().getAddressIn().getCap()).isEqualTo("00100");
        assertThat(captor.getValue().getAddressIn().getLocalita()).isEqualTo("Roma");
        assertThat(captor.getValue().getAddressIn().getLocalitaAggiuntiva()).isEqualTo("Centro");
        assertThat(captor.getValue().getAddressIn().getProvincia()).isEqualTo("RM");
        assertThat(captor.getValue().getAddressIn().getStato()).isEqualTo("ITALIA");
        verify(apiKeyUtils).checkApiKey(CX_ID, API_KEY);
    }

    @Test
    void normalizeSyncShouldStopWhenApiKeyCheckFails() {
        NormalizeSyncRequest request = new NormalizeSyncRequest();
        request.setCorrelationId("corr-1");
        request.setAddress(new AnalogAddress());

        when(apiKeyUtils.checkApiKey(eq(CX_ID), eq(API_KEY)))
                .thenReturn(Mono.error(new RuntimeException("api-key-error")));

        StepVerifier.create(service.normalizeSync(CX_ID, API_KEY, request))
                .expectErrorMessage("api-key-error")
                .verify();

        verifyNoInteractions(normalizeSyncClient);
    }
}
