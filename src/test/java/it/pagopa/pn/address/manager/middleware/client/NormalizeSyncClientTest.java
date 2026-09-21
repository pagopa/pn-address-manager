package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.MockServeConfig;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.AddressIn;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.sync.v1.dto.NormalizzazioneSyncResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class NormalizeSyncClientTest extends MockServeConfig {

    @Autowired
    private NormalizeSyncClient normalizeSyncClient;

    @Test
    void shouldReturn200OkWithValidNormalizzazioneSyncResponse() {
        AddressIn addressIn = new AddressIn()
                .id("addr-1")
                .localita("Napoli")
                .indirizzo("Via Napoli 12")
                .provincia("NA")
                .cap("80124");
        NormalizzazioneSyncRequest request = new NormalizzazioneSyncRequest();
        request.setAddressIn(addressIn);

        Mono<NormalizzazioneSyncResponse> responseMono = normalizeSyncClient.normalizzazioneSync("POSTEL", "test", request);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("", response.getErrore());
                    assertNotNull(response.getAddressOut());
                    assertEquals("addr-1", response.getAddressOut().getId());
                    assertEquals("80124", response.getAddressOut().getsCap());
                    assertEquals("NA", response.getAddressOut().getsSiglaProv());
                    assertEquals("NAPOLI", response.getAddressOut().getsComuneSpedizione());
                    assertEquals("VIA NAPOLI 12", response.getAddressOut().getsViaCompletaSpedizione());
                })
                .verifyComplete();
    }

}


