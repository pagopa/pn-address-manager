package it.pagopa.pn.address.manager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.DefaultAccessorNamingStrategy;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;

import java.text.DateFormat;

import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.ApiClient;
import it.pagopa.pn.address.manager.generated.openapi.msclient.postel.normalizzatore.v1.RFC3339DateFormat;
import org.junit.jupiter.api.Test;

class NormalizzatoreClientConfigTest {
	/**
	 * Method under test: {@link NormalizzatoreClientConfig#normalizzatoreApi(PnAddressManagerConfig)}
	 */
	@Test
	void testNormalizzatoreApi () {
		// Arrange
		NormalizzatoreClientConfig normalizzatoreClientConfig = new NormalizzatoreClientConfig();

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

		// Act and Assert
		ApiClient apiClient = normalizzatoreClientConfig.normalizzatoreApi(cfg).getApiClient();
		ObjectMapper objectMapper = apiClient.getObjectMapper();
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
		assertTrue(serializationConfig.getDefaultPrettyPrinter() instanceof DefaultPrettyPrinter);
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();
		ClassIntrospector classIntrospector = deserializationConfig.getClassIntrospector();
		assertTrue(classIntrospector instanceof BasicClassIntrospector);
		AccessorNamingStrategy.Provider accessorNaming = serializationConfig.getAccessorNaming();
		assertTrue(accessorNaming instanceof DefaultAccessorNamingStrategy.Provider);
		SerializerProvider serializerProviderInstance = objectMapper.getSerializerProviderInstance();
		assertTrue(serializerProviderInstance instanceof DefaultSerializerProvider.Impl);
		DateFormat dateFormat = apiClient.getDateFormat();
		assertTrue(dateFormat instanceof RFC3339DateFormat);
		JsonFactory factory = objectMapper.getFactory();
		assertEquals(" ", factory.getRootValueSeparator());
		assertEquals("Normalizzatore Base Path", apiClient.getBasePath());
		SerializerProvider serializerProvider = objectMapper.getSerializerProvider();
		assertEquals(16385, factory.getParserFeatures());
		assertEquals(21770556, serializationConfig.getSerializationFeatures());
		assertEquals(237020288, deserializationConfig.getDeserializationFeatures());
		assertEquals(31, factory.getGeneratorFeatures());
		assertEquals(JsonInclude.Include.ALWAYS, serializationConfig.getSerializationInclusion());
		DeserializerFactoryConfig factoryConfig = ((BeanDeserializerFactory) objectMapper.getDeserializationContext()
				.getFactory()).getFactoryConfig();
		assertFalse(factoryConfig.hasAbstractTypeResolvers());
		assertFalse(factoryConfig.hasDeserializerModifiers());
		assertFalse(deserializationConfig.hasExplicitTimeZone());
		assertFalse(serializationConfig.hasExplicitTimeZone());

	}

	@Test
	void testNormalizzatoreApi2 () {
		// Arrange
		NormalizzatoreClientConfig normalizzatoreClientConfig = new NormalizzatoreClientConfig();

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
		cfg.setAddressLengthValidation(2);
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

		// Act and Assert
		ApiClient apiClient = normalizzatoreClientConfig.normalizzatoreApi(cfg).getApiClient();
		ObjectMapper objectMapper = apiClient.getObjectMapper();
		SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
		assertTrue(serializationConfig.getDefaultPrettyPrinter() instanceof DefaultPrettyPrinter);
		DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();
		ClassIntrospector classIntrospector = deserializationConfig.getClassIntrospector();
		assertTrue(classIntrospector instanceof BasicClassIntrospector);
		AccessorNamingStrategy.Provider accessorNaming = serializationConfig.getAccessorNaming();
		assertTrue(accessorNaming instanceof DefaultAccessorNamingStrategy.Provider);
		SerializerProvider serializerProviderInstance = objectMapper.getSerializerProviderInstance();
		assertTrue(serializerProviderInstance instanceof DefaultSerializerProvider.Impl);
		DateFormat dateFormat = apiClient.getDateFormat();
		assertTrue(dateFormat instanceof RFC3339DateFormat);
		JsonFactory factory = objectMapper.getFactory();
		assertEquals(" ", factory.getRootValueSeparator());
		assertEquals("Normalizzatore Base Path", apiClient.getBasePath());
		assertNull(objectMapper.getPropertyNamingStrategy());
		assertNull(deserializationConfig.getPropertyNamingStrategy());
		SerializerProvider serializerProvider = objectMapper.getSerializerProvider();
		assertNull(serializerProvider.getConfig());
		assertNull(deserializationConfig.getHandlerInstantiator());
		assertNull(serializationConfig.getHandlerInstantiator());
		assertNull(serializationConfig.getFilterProvider());
		assertNull(deserializationConfig.getProblemHandlers());
		assertNull(deserializationConfig.getDefaultMergeable());
		assertNull(serializerProvider.getActiveView());
		assertEquals(16385, factory.getParserFeatures());
		assertEquals(21770556, serializationConfig.getSerializationFeatures());
		assertEquals(237020288, deserializationConfig.getDeserializationFeatures());
		assertEquals(31, factory.getGeneratorFeatures());
		assertEquals(JsonInclude.Include.ALWAYS, serializationConfig.getSerializationInclusion());
		DeserializerFactoryConfig factoryConfig = ((BeanDeserializerFactory) objectMapper.getDeserializationContext()
				.getFactory()).getFactoryConfig();
		assertFalse(factoryConfig.hasAbstractTypeResolvers());
		assertFalse(factoryConfig.hasDeserializerModifiers());
		assertFalse(deserializationConfig.hasExplicitTimeZone());
		assertFalse(serializationConfig.hasExplicitTimeZone());
	}
}
