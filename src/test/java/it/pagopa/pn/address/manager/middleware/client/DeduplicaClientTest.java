package it.pagopa.pn.address.manager.middleware.client;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressIn;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.ConfigIn;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import io.netty.handler.timeout.ReadTimeoutException;
import it.pagopa.pn.address.manager.MockServeConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class DeduplicaClientTest extends MockServeConfig {


    @Autowired
    private DeduplicaClient deduplicaClient;



    @Test
    void shouldReturn200Ok_withValidDeduplicaResponse() {
        // --- ARRANGE ---
        AddressIn masterIn = new AddressIn()
                .localita("Napoli")
                .indirizzo("Via Napoli 12")
                .provincia("NA")
                .cap("80124");
        AddressIn slaveIn = new AddressIn().localita("Napoli")
                .indirizzo("Via Napoli 12")
                .provincia("NA")
                .cap("80124");
        DeduplicaRequest request = new DeduplicaRequest();
        request.setMasterIn(masterIn);
        request.setSlaveIn(slaveIn);
        request.setConfigIn(new ConfigIn().configurazioneDeduplica("").configurazioneNorm(""));

        // --- ACT ---
        Mono<DeduplicaResponse> responseMono = deduplicaClient.deduplica(request);

        // --- ASSERT ---
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response, "La risposta non dovrebbe essere null");
                    assertTrue(response.getRisultatoDedu(), "Il risultato deduplica dovrebbe essere true");

                    // Verifica il masterOut
                    assertNotNull(response.getMasterOut(), "masterOut non dovrebbe essere null");
                    assertEquals("", response.getMasterOut().getId());
                    assertEquals("80124", response.getMasterOut().getsCap());
                    assertEquals("NA", response.getMasterOut().getsSiglaProv());
                    assertEquals("NA", response.getMasterOut().getsSiglaProv());
                    assertEquals("VIA NAPOLI 12", response.getMasterOut().getsViaCompletaSpedizione());

                    // Verifica lo slaveOut
                    assertNotNull(response.getSlaveOut(), "slaveOut non dovrebbe essere null");
                    assertEquals("", response.getSlaveOut().getId());
                    assertEquals("80124", response.getSlaveOut().getsCap());
                    assertEquals("NA", response.getSlaveOut().getsSiglaProv());
                    assertEquals("VIA NAPOLI 12", response.getSlaveOut().getsViaCompletaSpedizione());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowReadTimeoutException_whenServerTakesTooLong() {

        // --- ARRANGE ---
        AddressIn masterIn = new AddressIn().localita("Roma").indirizzo("Via Roma 1");
        AddressIn slaveIn = new AddressIn().localita("Milano").indirizzo("Via Milano 1");
        DeduplicaRequest request = new DeduplicaRequest();
        request.setMasterIn(masterIn);
        request.setSlaveIn(slaveIn);

        // --- ACT ---
        Mono<DeduplicaResponse> responseMono = deduplicaClient.deduplica(request);

        // --- ASSERT ---
        StepVerifier.create(responseMono)
                .expectErrorMatches(throwable -> {
                    // Verifica che l'eccezione lanciata sia dovuta al timeout
                    return throwable instanceof WebClientRequestException &&
                            throwable.getCause() instanceof ReadTimeoutException;
                })
                .verify();
    }
}
