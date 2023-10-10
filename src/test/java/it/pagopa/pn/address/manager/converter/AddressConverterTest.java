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

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.*;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
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
     * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse, String)}
     */
    @Test
    void testCreateDeduplicatesResponseFromDeduplicaResponse() {
        assertThrows(PnInternalException.class,
                () -> addressConverter.createDeduplicatesResponseFromDeduplicaResponse(new DeduplicaResponse(), "42"));
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
     * Method under test: {@link AddressConverter#createPostelBatchByBatchIdAndFileKey(String, String, String)}
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
        postel.setRequestPrefix("");

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");

        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(normalizer);
        PostelBatch actualCreatePostelBatchByBatchIdAndFileKeyResult = (new AddressConverter(pnAddressManagerConfig))
                .createPostelBatchByBatchIdAndFileKey("42", "File Key", "sha256");
        assertEquals("42", actualCreatePostelBatchByBatchIdAndFileKeyResult.getBatchId());
        assertEquals("NOT_WORKED", actualCreatePostelBatchByBatchIdAndFileKeyResult.getStatus());
        assertEquals(0, actualCreatePostelBatchByBatchIdAndFileKeyResult.getRetry().intValue());
        assertEquals("File Key", actualCreatePostelBatchByBatchIdAndFileKeyResult.getFileKey());
    }
}

