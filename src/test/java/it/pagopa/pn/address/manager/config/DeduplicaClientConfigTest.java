package it.pagopa.pn.address.manager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.postel.deduplica.v1.RFC3339DateFormat;

import java.text.DateFormat;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

class DeduplicaClientConfigTest {
    /**
     * Method under test: {@link DeduplicaClientConfig#deduplicaApi(PnAddressManagerConfig)}
     */
    @Test
    void testDeduplicaApi() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Diffblue AI was unable to find a test

        DeduplicaClientConfig deduplicaClientConfig = new DeduplicaClientConfig(new ResponseExchangeFilter());

        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Path Cap");
        csv.setPathCountry("GB");

        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setApiKeyTableName("Api Key Table Name");
        dao.setBatchRequestTableName("Batch Request Table Name");
        dao.setCapTableName("Cap Table Name");
        dao.setCountryTableName("GB");
        dao.setPostelBatchTableName("Postel Batch Table Name");
        dao.setShedlockTableName("Shedlock Table Name");

        PnAddressManagerConfig.EventBus eventBus = new PnAddressManagerConfig.EventBus();
        eventBus.setDetailType("Detail Type");
        eventBus.setName("Name");
        eventBus.setSource("Source");

        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setLockAtLeast(2);
        batchRequest.setLockAtMost(2);
        batchRequest.setMaxRetry(3);
        batchRequest.setQueryMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setRequestPrefix("Request Prefix");
        postel.setTtl(2);
        postel.setWorkingTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setMaxCsvSize(3);
        normalizer.setMaxFileNumber(3);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");

        PnAddressManagerConfig.Sqs sqs = new PnAddressManagerConfig.Sqs();
        sqs.setCallbackDlqQueueName("Callback Dlq Queue Name");
        sqs.setCallbackQueueName("Callback Queue Name");
        sqs.setInputDlqQueueName("Input Dlq Queue Name");
        sqs.setInputQueueName("Input Queue Name");

        PnAddressManagerConfig cfg = new PnAddressManagerConfig();
        cfg.setAddressLengthValidation(1);
        cfg.setApiKey("Api Key");
        cfg.setCsv(csv);
        cfg.setDao(dao);
        cfg.setDeduplicaBasePath("Deduplica Base Path");
        cfg.setEnableValidation(true);
        cfg.setEnableWhitelisting(true);
        cfg.setEventBus(eventBus);
        cfg.setFlagCsv(true);
        cfg.setHealthCheckPath("Health Check Path");
        cfg.setNormalizer(normalizer);
        cfg.setNormalizzatoreBasePath("Normalizzatore Base Path");
        cfg.setPagoPaCxId("42");
        cfg.setPostelCxId("42");
        cfg.setSafeStorageBasePath("Safe Storage Base Path");
        cfg.setSqs(sqs);
        cfg.setValidationPattern("Validation Pattern");
        ApiClient apiClient = deduplicaClientConfig.deduplicaApi(cfg).getApiClient();
        assertTrue(apiClient.getAuthentications().isEmpty());
        assertEquals("Deduplica Base Path", apiClient.getBasePath());
        DateFormat dateFormat = apiClient.getDateFormat();
        assertTrue(dateFormat instanceof RFC3339DateFormat);
        ObjectMapper objectMapper = apiClient.getObjectMapper();
        assertTrue(objectMapper.getPolymorphicTypeValidator() instanceof LaissezFaireSubTypeValidator);
        assertNull(objectMapper.getPropertyNamingStrategy());
        assertSame(dateFormat, objectMapper.getDateFormat());
        assertTrue(objectMapper.getSerializerFactory() instanceof BeanSerializerFactory);
        assertTrue(objectMapper.getSubtypeResolver() instanceof StdSubtypeResolver);
        assertTrue(objectMapper.getSerializerProvider() instanceof DefaultSerializerProvider.Impl);
        assertTrue(objectMapper.getSerializerProviderInstance() instanceof DefaultSerializerProvider.Impl);
        assertTrue(objectMapper.getVisibilityChecker() instanceof VisibilityChecker.Std);
        assertTrue(objectMapper.getDeserializationContext() instanceof DefaultDeserializationContext.Impl);
        JsonFactory factory = objectMapper.getFactory();
        assertSame(factory, objectMapper.getJsonFactory());
        assertEquals(31, factory.getGeneratorFeatures());
        assertEquals(" ", factory.getRootValueSeparator());
        assertSame(objectMapper, factory.getCodec());
        assertEquals(16385, factory.getParserFeatures());
    }

    /**
     * Method under test: {@link DeduplicaClientConfig#deduplicaApi(PnAddressManagerConfig)}
     */
    @Test
    void testDeduplicaApi3() {
        //   Diffblue Cover was unable to write a Spring test,
        //   so wrote a non-Spring test instead.
        //   Diffblue AI was unable to find a test

        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        ResponseExchangeFilter responseExchangeFilter = mock(ResponseExchangeFilter.class);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        DeduplicaClientConfig deduplicaClientConfig = new DeduplicaClientConfig(responseExchangeFilter);

        PnAddressManagerConfig.Csv csv = new PnAddressManagerConfig.Csv();
        csv.setPathCap("Path Cap");
        csv.setPathCountry("GB");

        PnAddressManagerConfig.Dao dao = new PnAddressManagerConfig.Dao();
        dao.setApiKeyTableName("Api Key Table Name");
        dao.setBatchRequestTableName("Batch Request Table Name");
        dao.setCapTableName("Cap Table Name");
        dao.setCountryTableName("GB");
        dao.setPostelBatchTableName("Postel Batch Table Name");
        dao.setShedlockTableName("Shedlock Table Name");

        PnAddressManagerConfig.EventBus eventBus = new PnAddressManagerConfig.EventBus();
        eventBus.setDetailType("Detail Type");
        eventBus.setName("Name");
        eventBus.setSource("Source");

        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setDelay(2);
        batchRequest.setEventBridgeRecoveryDelay(1);
        batchRequest.setLockAtLeast(2);
        batchRequest.setLockAtMost(2);
        batchRequest.setMaxRetry(3);
        batchRequest.setQueryMaxSize(3);
        batchRequest.setRecoveryAfter(2);
        batchRequest.setRecoveryDelay(2);
        batchRequest.setTtl(2);

        PnAddressManagerConfig.Postel postel = new PnAddressManagerConfig.Postel();
        postel.setMaxRetry(3);
        postel.setRecoveryAfter(2);
        postel.setRecoveryDelay(2);
        postel.setRequestPrefix("Request Prefix");
        postel.setTtl(2);
        postel.setWorkingTtl(2);

        PnAddressManagerConfig.Normalizer normalizer = new PnAddressManagerConfig.Normalizer();
        normalizer.setBatchRequest(batchRequest);
        normalizer.setMaxCsvSize(3);
        normalizer.setMaxFileNumber(3);
        normalizer.setPostel(postel);
        normalizer.setPostelAuthKey("Postel Auth Key");

        PnAddressManagerConfig.Sqs sqs = new PnAddressManagerConfig.Sqs();
        sqs.setCallbackDlqQueueName("Callback Dlq Queue Name");
        sqs.setCallbackQueueName("Callback Queue Name");
        sqs.setInputDlqQueueName("Input Dlq Queue Name");
        sqs.setInputQueueName("Input Queue Name");

        PnAddressManagerConfig cfg = new PnAddressManagerConfig();
        cfg.setAddressLengthValidation(1);
        cfg.setApiKey("Api Key");
        cfg.setCsv(csv);
        cfg.setDao(dao);
        cfg.setDeduplicaBasePath("Deduplica Base Path");
        cfg.setEnableValidation(true);
        cfg.setEnableWhitelisting(true);
        cfg.setEventBus(eventBus);
        cfg.setFlagCsv(true);
        cfg.setHealthCheckPath("Health Check Path");
        cfg.setNormalizer(normalizer);
        cfg.setNormalizzatoreBasePath("Normalizzatore Base Path");
        cfg.setPagoPaCxId("42");
        cfg.setPostelCxId("42");
        cfg.setSafeStorageBasePath("Safe Storage Base Path");
        cfg.setSqs(sqs);
        cfg.setValidationPattern("Validation Pattern");
        ApiClient apiClient = deduplicaClientConfig.deduplicaApi(cfg).getApiClient();
        assertTrue(apiClient.getAuthentications().isEmpty());
        assertEquals("Deduplica Base Path", apiClient.getBasePath());
        DateFormat dateFormat = apiClient.getDateFormat();
        assertTrue(dateFormat instanceof RFC3339DateFormat);
        ObjectMapper objectMapper = apiClient.getObjectMapper();
        assertTrue(objectMapper.getPolymorphicTypeValidator() instanceof LaissezFaireSubTypeValidator);
        assertNull(objectMapper.getPropertyNamingStrategy());
        assertSame(dateFormat, objectMapper.getDateFormat());
        assertTrue(objectMapper.getSerializerFactory() instanceof BeanSerializerFactory);
        assertTrue(objectMapper.getSubtypeResolver() instanceof StdSubtypeResolver);
        assertTrue(objectMapper.getSerializerProvider() instanceof DefaultSerializerProvider.Impl);
        assertTrue(objectMapper.getSerializerProviderInstance() instanceof DefaultSerializerProvider.Impl);
        assertTrue(objectMapper.getVisibilityChecker() instanceof VisibilityChecker.Std);
        assertTrue(objectMapper.getDeserializationContext() instanceof DefaultDeserializationContext.Impl);
        JsonFactory factory = objectMapper.getFactory();
        assertSame(factory, objectMapper.getJsonFactory());
        assertEquals(31, factory.getGeneratorFeatures());
        assertEquals(" ", factory.getRootValueSeparator());
        assertSame(objectMapper, factory.getCodec());
        assertEquals(16385, factory.getParserFeatures());
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
}

