package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressIn;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.AddressOut;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaRequest;
import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.DeduplicaResponse;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
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
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Autowired
    AddressConverter addressConverter;


    /**
     * Method under test: {@link AddressConverter#createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest)}
     */
    @Test
    void testCreateDeduplicaRequestFromDeduplicatesRequest() {
        DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
        deduplicatesRequest.baseAddress(new AnalogAddress());
        deduplicatesRequest.targetAddress(new AnalogAddress());
        DeduplicaRequest actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
                .createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);
        AddressIn slaveIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getSlaveIn();
        AddressIn masterIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getMasterIn();
        assertEquals(slaveIn, masterIn);
        assertNull(slaveIn.getStato());
        assertNull(masterIn.getStato());
        assertNull(masterIn.getProvincia());
        assertNull(masterIn.getLocalitaAggiuntiva());
        assertNull(masterIn.getIndirizzoAggiuntivo());
        assertNull(masterIn.getId());
        assertNull(masterIn.getCap());
        assertNull(slaveIn.getProvincia());
        assertNull(slaveIn.getLocalitaAggiuntiva());
        assertNull(slaveIn.getIndirizzoAggiuntivo());
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
        when(deduplicatesRequest.getTargetAddress()).thenReturn(new AnalogAddress());
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
        assertNull(masterIn.getIndirizzoAggiuntivo());
        assertEquals("42", masterIn.getId());
        assertNull(masterIn.getCap());
        assertNull(slaveIn.getProvincia());
        assertNull(slaveIn.getLocalitaAggiuntiva());
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
        DeduplicaResponse risultatoDeduplica = new DeduplicaResponse();
        assertThrows(PnInternalException.class, () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));

    }

    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponseError() {
        createDeduplicatesResponseFromDeduplicaResponseError("DED001");
        createDeduplicatesResponseFromDeduplicaResponseError("DED002");
        createDeduplicatesResponseFromDeduplicaResponseError("DED003");
        createDeduplicatesResponseFromDeduplicaResponseError("DED400");
    }

    void createDeduplicatesResponseFromDeduplicaResponseError(String errore) {
        AddressOut addressOut = mock(AddressOut.class);
        DeduplicaResponse risultatoDeduplica = new DeduplicaResponse();
        risultatoDeduplica.setSlaveOut(addressOut);
        risultatoDeduplica.setMasterOut(addressOut);
        risultatoDeduplica.setErrore(errore);
        assertDoesNotThrow(() -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
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
    }

    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponseGetAnalogAddressError() {
        DeduplicaResponse risultatoDeduplica = new DeduplicaResponse();
        AddressOut addressOut = new AddressOut();
        addressOut.setfPostalizzabile(String.valueOf(0));
        risultatoDeduplica.setSlaveOut(addressOut);
        assertDoesNotThrow(() -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
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
    void testCreateDeduplicatesResponseFromDeduplicaResponse8() {
        AddressOut addressOut = mock(AddressOut.class);
        when(addressOut.getnErroreNorm()).thenThrow(new PnInternalAddressManagerException("An error occurred",
                "The characteristics of someone or something", 2, "An error occurred"));
        when(addressOut.getfPostalizzabile()).thenReturn("0");
        DeduplicaResponse risultatoDeduplica = mock(DeduplicaResponse.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(addressOut);
        when(risultatoDeduplica.getErrore()).thenReturn(null);
        assertThrows(PnInternalAddressManagerException.class,
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
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = getNormalizer(batchRequest);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        NormalizzatoreBatch actualCreatePostelBatchByRequestIdAndFileKeyResult = (new AddressConverter())
                .createPostelBatchByBatchIdAndFileKey("42", "File Key", "sha256");
        assertEquals("42", actualCreatePostelBatchByRequestIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByRequestIdAndFileKeyResult.getStatus());
        assertEquals(0, actualCreatePostelBatchByRequestIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByRequestIdAndFileKeyResult.getFileKey());
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer(PnAddressManagerConfig.BatchRequest batchRequest) {
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
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
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = getNormalizer1(batchRequest);

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        NormalizzatoreBatch actualCreatePostelBatchByRequestIdAndFileKeyResult = (new AddressConverter())
                .createPostelBatchByBatchIdAndFileKey("Request Prefix42", "File Key", "Sha256");
        assertEquals("Request Prefix42", actualCreatePostelBatchByRequestIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByRequestIdAndFileKeyResult.getStatus());
        assertEquals("Sha256", actualCreatePostelBatchByRequestIdAndFileKeyResult.getSha256());
        assertEquals(0, actualCreatePostelBatchByRequestIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByRequestIdAndFileKeyResult.getFileKey());
    }

    @NotNull
    private static PnAddressManagerConfig.Normalizer getNormalizer1(PnAddressManagerConfig.BatchRequest batchRequest) {
        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
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

