package it.pagopa.pn.address.manager.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.ConfigIn;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.InputDeduplica;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.MasterIn;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.RisultatoDeduplica;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.SlaveIn;
import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.SlaveOut;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException: Cannot invoke "it.pagopa.pn.address.manager.config.PnAddressManagerConfig$Normalizer.getPostelAuthKey()" because the return value of "it.pagopa.pn.address.manager.config.PnAddressManagerConfig.getNormalizer()" is null
        //       at it.pagopa.pn.address.manager.converter.AddressConverter.createDeduplicaRequestFromDeduplicatesRequest(AddressConverter.java:31)
        //   See https://diff.blue/R013 to resolve this issue.

        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setMaxRetry(3);
        batchRequest.setMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setDelay(2);
        postel.setMaxRetry(3);
        postel.setMaxSize(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setTtl(2);

        PnAddressManagerConfig.BatchRequest batchRequest2 = new PnAddressManagerConfig.BatchRequest();
        batchRequest2.setDelay(2);
        batchRequest2.setEventBridgeRecoveryDelay(1);
        batchRequest2.setMaxRetry(3);
        batchRequest2.setMaxSize(3);
        batchRequest2.setRecoveryAfter(2);
        batchRequest2.setRecoveryDelay(2);
        batchRequest2.setTtl(2);

        PnAddressManagerConfig.Postel postel2 = new PnAddressManagerConfig.Postel();
        postel2.setDelay(2);
        postel2.setMaxRetry(3);
        postel2.setMaxSize(3);
        postel2.setRecoveryAfter(2);
        postel2.setRecoveryDelay(2);
        postel2.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest2);
        normalizer.setPostel(postel2);
        normalizer.setPostelAuthKey("Postel Auth Key");
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        AddressConverter addressConverter = new AddressConverter(pnAddressManagerConfig);
        DeduplicatesRequest deduplicatesRequest = mock(DeduplicatesRequest.class);
        when(deduplicatesRequest.getBaseAddress()).thenReturn(new AnalogAddress());
        when(deduplicatesRequest.getTargetAddress()).thenReturn(new AnalogAddress());
        when(deduplicatesRequest.getCorrelationId()).thenReturn("42");
        InputDeduplica actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
                .createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);
        SlaveIn slaveIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getSlaveIn();
        assertNull(slaveIn.getStato());
        MasterIn masterIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getMasterIn();
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
        ConfigIn configIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getConfigIn();
        assertEquals("", configIn.getConfigurazioneDeduplica());
        assertEquals("Postel Auth Key", configIn.getAuthKey());
        assertEquals("", configIn.getConfigurazioneNorm());
        verify(deduplicatesRequest, atLeast(1)).getBaseAddress();
        verify(deduplicatesRequest, atLeast(1)).getTargetAddress();
        verify(deduplicatesRequest, atLeast(1)).getCorrelationId();
    }


    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse() {
        assertThrows(PnInternalException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(new RisultatoDeduplica(), "42"));
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse2() {
        RisultatoDeduplica risultatoDeduplica = new RisultatoDeduplica();
        risultatoDeduplica.erroreDedu(-1);
        assertThrows(PnInternalException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse3() {
        RisultatoDeduplica risultatoDeduplica = mock(RisultatoDeduplica.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(new SlaveOut());
        when(risultatoDeduplica.getErroreDedu()).thenReturn(-1);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn("Risultato Dedu");
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertEquals("TODO", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
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
        verify(risultatoDeduplica).getErroreDedu();
        verify(risultatoDeduplica, atLeast(1)).getRisultatoDedu();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse4() {
        SlaveOut slaveOut = mock(SlaveOut.class);
        when(slaveOut.getfPostalizzabile()).thenReturn("Getf Postalizzabile");
        when(slaveOut.getsCap()).thenReturn("Gets Cap");
        when(slaveOut.getsCivicoAltro()).thenReturn("Gets Civico Altro");
        when(slaveOut.getsComuneSpedizione()).thenReturn("Gets Comune Spedizione");
        when(slaveOut.getsFrazioneSpedizione()).thenReturn("Gets Frazione Spedizione");
        when(slaveOut.getsSiglaProv()).thenReturn("Gets Sigla Prov");
        when(slaveOut.getsStatoSpedizione()).thenReturn("Gets Stato Spedizione");
        when(slaveOut.getsViaCompletaSpedizione()).thenReturn("Gets Via Completa Spedizione");
        RisultatoDeduplica risultatoDeduplica = mock(RisultatoDeduplica.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(slaveOut);
        when(risultatoDeduplica.getErroreDedu()).thenReturn(-1);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn("Risultato Dedu");
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertEquals("TODO", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
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
        verify(risultatoDeduplica).getErroreDedu();
        verify(risultatoDeduplica, atLeast(1)).getRisultatoDedu();
        verify(slaveOut, atLeast(1)).getfPostalizzabile();
        verify(slaveOut).getsCap();
        verify(slaveOut).getsCivicoAltro();
        verify(slaveOut).getsComuneSpedizione();
        verify(slaveOut).getsFrazioneSpedizione();
        verify(slaveOut).getsSiglaProv();
        verify(slaveOut).getsStatoSpedizione();
        verify(slaveOut).getsViaCompletaSpedizione();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse5() {
        SlaveOut slaveOut = mock(SlaveOut.class);
        when(slaveOut.getfPostalizzabile()).thenReturn("Getf Postalizzabile");
        when(slaveOut.getsCap()).thenReturn("Gets Cap");
        when(slaveOut.getsCivicoAltro()).thenReturn("Gets Civico Altro");
        when(slaveOut.getsComuneSpedizione()).thenReturn("Gets Comune Spedizione");
        when(slaveOut.getsFrazioneSpedizione()).thenReturn("Gets Frazione Spedizione");
        when(slaveOut.getsSiglaProv()).thenReturn("Gets Sigla Prov");
        when(slaveOut.getsStatoSpedizione()).thenReturn("Gets Stato Spedizione");
        when(slaveOut.getsViaCompletaSpedizione()).thenReturn("Gets Via Completa Spedizione");
        RisultatoDeduplica risultatoDeduplica = mock(RisultatoDeduplica.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(slaveOut);
        when(risultatoDeduplica.getErroreDedu()).thenReturn(-1);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn("0");
        DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
                .createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42");
        assertEquals("42", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
        assertEquals("TODO", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
        assertFalse(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getEqualityResult());
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
        verify(risultatoDeduplica).getErroreDedu();
        verify(risultatoDeduplica, atLeast(1)).getRisultatoDedu();
        verify(slaveOut, atLeast(1)).getfPostalizzabile();
        verify(slaveOut).getsCap();
        verify(slaveOut).getsCivicoAltro();
        verify(slaveOut).getsComuneSpedizione();
        verify(slaveOut).getsFrazioneSpedizione();
        verify(slaveOut).getsSiglaProv();
        verify(slaveOut).getsStatoSpedizione();
        verify(slaveOut).getsViaCompletaSpedizione();
    }

    /**
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse6() {
        SlaveOut slaveOut = mock(SlaveOut.class);
        when(slaveOut.getfPostalizzabile()).thenThrow(new PnInternalException("An error occurred"));
        RisultatoDeduplica risultatoDeduplica = mock(RisultatoDeduplica.class);
        when(risultatoDeduplica.getSlaveOut()).thenReturn(slaveOut);
        when(risultatoDeduplica.getErroreDedu()).thenReturn(-1);
        when(risultatoDeduplica.getRisultatoDedu()).thenReturn("Risultato Dedu");
        assertThrows(PnInternalException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica, "42"));
        verify(risultatoDeduplica, atLeast(1)).getSlaveOut();
        verify(risultatoDeduplica).getErroreDedu();
        verify(risultatoDeduplica, atLeast(1)).getRisultatoDedu();
        verify(slaveOut).getfPostalizzabile();
    }

    /**
     * Method under test: {@link AddressConverter#createPostelBatchByBatchIdAndFileKey(String, String)}
     */
    @Test
    void testCreatePostelBatchByBatchIdAndFileKey() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Reason: R013 No inputs found that don't throw a trivial exception.
        //   Diffblue Cover tried to run the arrange/act section, but the method under
        //   test threw
        //   java.lang.NullPointerException: Cannot invoke "it.pagopa.pn.address.manager.config.PnAddressManagerConfig$Normalizer.getBatchRequest()" because the return value of "it.pagopa.pn.address.manager.config.PnAddressManagerConfig.getNormalizer()" is null
        //       at it.pagopa.pn.address.manager.converter.AddressConverter.createPostelBatchByBatchIdAndFileKey(AddressConverter.java:132)
        //   See https://diff.blue/R013 to resolve this issue.

        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setMaxRetry(3);
        batchRequest.setMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setDelay(2);
        postel.setMaxRetry(3);
        postel.setMaxSize(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        PostelBatch actualCreatePostelBatchByBatchIdAndFileKeyResult = (new AddressConverter(pnAddressManagerConfig))
                .createPostelBatchByBatchIdAndFileKey("42", "File Key");
        assertEquals("42", actualCreatePostelBatchByBatchIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByBatchIdAndFileKeyResult.getStatus());
        assertEquals(0, actualCreatePostelBatchByBatchIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByBatchIdAndFileKeyResult.getFileKey());
    }
}

