package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.AddressIn;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.AddressOut;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {AddressConverter.class, PnAddressManagerConfig.class})
@ExtendWith(SpringExtension.class)
class AddressConverterTest {
    @Autowired
    private AddressConverter addressConverter;

    @Autowired
    private PnAddressManagerConfig pnAddressManagerConfig;


    /**
     * Method under test: {@link AddressConverter#createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest)}
     */
    @Test
    void testCreateDeduplicaRequestFromDeduplicatesRequest() {
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        deduplicatesRequest.baseAddress(new AnalogAddress());
        DeduplicaRequest actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
                .createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);
        AddressIn slaveIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getSlaveIn();
        AddressIn masterIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getMasterIn();
        assertEquals(slaveIn, masterIn);
        assertNull(slaveIn.getStato());
        assertNull(masterIn.getStato());
        assertNull(masterIn.getProvincia());
        assertNull(masterIn.getLocalitaAggiuntiva());
        assertEquals("null null", masterIn.getIndirizzo());
        assertNull(masterIn.getId());
        assertNull(masterIn.getCap());
        assertNull(slaveIn.getProvincia());
        assertNull(slaveIn.getLocalitaAggiuntiva());
        assertNull(slaveIn.getLocalita());
        assertEquals("null null", slaveIn.getIndirizzo());
        assertNull(slaveIn.getId());
        assertNull(slaveIn.getCap());
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest)}
     */
    @Test
    void testCreateDeduplicaRequestFromDeduplicatesRequest2() {
        DeduplicatesRequest deduplicatesRequest = mock(DeduplicatesRequest.class);
        when(deduplicatesRequest.getBaseAddress()).thenReturn(new AnalogAddress());
        when(deduplicatesRequest.getCorrelationId()).thenReturn("42");
        DeduplicaRequest actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
                .createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);
        AddressIn slaveIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getSlaveIn();
        AddressIn masterIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getMasterIn();
        assertEquals(slaveIn, masterIn);
        assertNull(slaveIn.getStato());
        assertNull(masterIn.getStato());
        assertNull(masterIn.getProvincia());
        assertNull(masterIn.getLocalitaAggiuntiva());
        assertNull(masterIn.getLocalita());
        assertEquals("null null", masterIn.getIndirizzo());
        assertEquals("42", masterIn.getId());
        assertNull(masterIn.getCap());
        assertNull(slaveIn.getProvincia());
        assertNull(slaveIn.getLocalitaAggiuntiva());
        assertNull(slaveIn.getLocalita());
        assertEquals("null null", slaveIn.getIndirizzo());
        assertEquals("42", slaveIn.getId());
        assertNull(slaveIn.getCap());
        verify(deduplicatesRequest, atLeast(1)).getBaseAddress();
        verify(deduplicatesRequest, atLeast(1)).getCorrelationId();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse() {
        assertThrows(PnInternalException.class, () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(new DeduplicaResponse(), "42"));

    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse2() {
        DeduplicaResponse risultatoDeduplica = new DeduplicaResponse();
        risultatoDeduplica.errore("-1");
        assertThrows(PnAddressManagerException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse3() {
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getErrore()).thenReturn("An error occurred");
        assertThrows(PnAddressManagerException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
        verify(risultatoDeduplica, atLeast(1)).getErrore();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse4() {
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn(true);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(new AddressOut());
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertTrue(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getEqualityResult());
        AnalogAddress normalizedAddress = actualCreateDeduplicatesResponseFromDeduplicaResponseResult
                .getNormalizedAddress();
        assertNull(normalizedAddress.getCountry());
        assertNull(normalizedAddress.getCity2());
        assertNull(normalizedAddress.getCity());
        assertNull(normalizedAddress.getCap());
        assertNull(normalizedAddress.getAddressRow2());
        assertNull(normalizedAddress.getAddressRow());
        assertNull(normalizedAddress.getPr());
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getRisultatoDedu();
        verify(risultatoDeduplica).getErrore();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse5() {
        AddressOut addressOut = mock(AddressOut.class);
        when(addressOut.getfPostalizzabile()).thenReturn("Getf Postalizzabile");
        when(addressOut.getsCap()).thenReturn("Gets Cap");
        when(addressOut.getsCivicoAltro()).thenReturn("Gets Civico Altro");
        when(addressOut.getsComuneSpedizione()).thenReturn("Gets Comune Spedizione");
        when(addressOut.getsFrazioneSpedizione()).thenReturn("Gets Frazione Spedizione");
        when(addressOut.getsSiglaProv()).thenReturn("Gets Sigla Prov");
        when(addressOut.getsStatoSpedizione()).thenReturn("Gets Stato Spedizione");
        when(addressOut.getsViaCompletaSpedizione()).thenReturn("Gets Via Completa Spedizione");
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn(true);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(addressOut);
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertTrue(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getEqualityResult());
        AnalogAddress normalizedAddress = actualCreateDeduplicatesResponseFromDeduplicaResponseResult
                .getNormalizedAddress();
        assertEquals("Gets Stato Spedizione", normalizedAddress.getCountry());
        assertEquals("Gets Frazione Spedizione", normalizedAddress.getCity2());
        assertEquals("Gets Comune Spedizione", normalizedAddress.getCity());
        assertEquals("Gets Cap", normalizedAddress.getCap());
        assertEquals("Gets Civico Altro", normalizedAddress.getAddressRow2());
        assertEquals("Gets Via Completa Spedizione", normalizedAddress.getAddressRow());
        assertEquals("Gets Sigla Prov", normalizedAddress.getPr());
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getRisultatoDedu();
        verify(risultatoDeduplica).getErrore();
        verify(addressOut, atLeast(1)).getfPostalizzabile();
        verify(addressOut).getsCap();
        verify(addressOut).getsCivicoAltro();
        verify(addressOut).getsComuneSpedizione();
        verify(addressOut).getsFrazioneSpedizione();
        verify(addressOut).getsSiglaProv();
        verify(addressOut).getsStatoSpedizione();
        verify(addressOut).getsViaCompletaSpedizione();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse6() {
        AddressOut addressOut = mock(AddressOut.class);
        when(addressOut.getnErroreNorm()).thenReturn(-1);
        when(addressOut.getfPostalizzabile()).thenReturn("0");
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(addressOut);
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertEquals("TODO", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getErrore();
        verify(addressOut).getnErroreNorm();
        verify(addressOut, atLeast(1)).getfPostalizzabile();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse7() {
        AddressOut addressOut = mock(AddressOut.class);
        when(addressOut.getnErroreNorm()).thenReturn(null);
        when(addressOut.getfPostalizzabile()).thenReturn("0");
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(addressOut);
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertNull(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getErrore();
        verify(addressOut).getnErroreNorm();
        verify(addressOut, atLeast(1)).getfPostalizzabile();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse8() {
        AddressOut addressOut = mock(AddressOut.class);
        when(addressOut.getnErroreNorm()).thenThrow(new PnAddressManagerException("An error occurred",
                "The characteristics of someone or something", 2, "An error occurred"));
        when(addressOut.getfPostalizzabile()).thenReturn("0");
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(addressOut);
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        assertThrows(PnAddressManagerException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getErrore();
        verify(addressOut).getnErroreNorm();
        verify(addressOut, atLeast(1)).getfPostalizzabile();
    }

    /**
     * Method under test: {@link AddressConverter#createPostelBatchByBatchIdAndFileKey(String, String, String)}
     */
    @Test
    void testCreatePostelBatchByBatchIdAndFileKey() {
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setMaxRetry(3);
        batchRequest.setMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = getNormalizer(batchRequest);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        PostelBatch actualCreatePostelBatchByBatchIdAndFileKeyResult = (new AddressConverter(pnAddressManagerConfig))
                .createPostelBatchByBatchIdAndFileKey("42", "File Key", "sha256");
        assertEquals("42", actualCreatePostelBatchByBatchIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByBatchIdAndFileKeyResult.getStatus());
        assertEquals(0, actualCreatePostelBatchByBatchIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByBatchIdAndFileKeyResult.getFileKey());
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer(PnAddressManagerConfig.BatchRequest batchRequest) {
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setDelay(2);
        postel.setMaxRetry(3);
        postel.setMaxSize(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setTtl(2);
        postel.setRequestPrefix("");

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");
        return normalizer;
    }

    /**
     * Method under test: {@link AddressConverter#createPostelBatchByBatchIdAndFileKey(String, String, String)}
     */
    @Test
    void testCreatePostelBatchByBatchIdAndFileKey2() {
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setMaxRetry(3);
        batchRequest.setMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = getNormalizer1(batchRequest);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        PostelBatch actualCreatePostelBatchByBatchIdAndFileKeyResult = (new AddressConverter(pnAddressManagerConfig))
                .createPostelBatchByBatchIdAndFileKey("42", "File Key", "Sha256");
        assertEquals("Request Prefix42", actualCreatePostelBatchByBatchIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByBatchIdAndFileKeyResult.getStatus());
        assertEquals("Sha256", actualCreatePostelBatchByBatchIdAndFileKeyResult.getSha256());
        assertEquals(0, actualCreatePostelBatchByBatchIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByBatchIdAndFileKeyResult.getFileKey());
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer1(PnAddressManagerConfig.BatchRequest batchRequest) {
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setDelay(2);
        postel.setMaxRetry(3);
        postel.setMaxSize(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setRequestPrefix("Request Prefix");
        postel.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");
        return normalizer;
    }
}

