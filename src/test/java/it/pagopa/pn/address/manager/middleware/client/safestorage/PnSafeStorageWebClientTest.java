package it.pagopa.pn.address.manager.middleware.client.safestorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.introspect.AccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.BasicClassIntrospector;
import com.fasterxml.jackson.databind.introspect.ClassIntrospector;
import com.fasterxml.jackson.databind.introspect.DefaultAccessorNamingStrategy;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.StdSubtypeResolver;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.impl.FailingSerializer;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ArrayIterator;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.log.ResponseExchangeFilter;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.ApiClient;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.RFC3339DateFormat;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.auth.ApiKeyAuth;
import it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.auth.Authentication;

import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

@ContextConfiguration(classes = {PnSafeStorageWebClient.class, PnAddressManagerConfig.class})
@ExtendWith(SpringExtension.class)
class PnSafeStorageWebClientTest {
    @Autowired
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Autowired
    private PnSafeStorageWebClient pnSafeStorageWebClient;

    @MockBean
    private ResponseExchangeFilter responseExchangeFilter;

    /**
     * Method under test: {@link PnSafeStorageWebClient#init()}
     */
    @Test
    void testInit() {
        ExchangeFilterFunction exchangeFilterFunction = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction.apply(Mockito.<ExchangeFunction>any())).thenReturn(mock(ExchangeFunction.class));
        ExchangeFilterFunction exchangeFilterFunction2 = mock(ExchangeFilterFunction.class);
        when(exchangeFilterFunction2.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction);
        when(responseExchangeFilter.andThen(Mockito.<ExchangeFilterFunction>any())).thenReturn(exchangeFilterFunction2);
        ApiClient actualInitResult = pnSafeStorageWebClient.init();
        Map<String, Authentication> authentications = actualInitResult.getAuthentications();
        assertEquals(1, authentications.size());
        assertEquals("http://localhost:8080", actualInitResult.getBasePath());
        DateFormat dateFormat = actualInitResult.getDateFormat();
        assertTrue(dateFormat instanceof RFC3339DateFormat);
        ObjectMapper objectMapper = actualInitResult.getObjectMapper();
        PolymorphicTypeValidator polymorphicTypeValidator = objectMapper.getPolymorphicTypeValidator();
        assertTrue(polymorphicTypeValidator instanceof LaissezFaireSubTypeValidator);
        assertNull(objectMapper.getPropertyNamingStrategy());
        assertEquals(1, objectMapper.getRegisteredModuleIds().size());
        SerializerFactory serializerFactory = objectMapper.getSerializerFactory();
        assertTrue(serializerFactory instanceof BeanSerializerFactory);
        SerializerProvider serializerProvider = objectMapper.getSerializerProvider();
        assertTrue(serializerProvider instanceof DefaultSerializerProvider.Impl);
        SerializerProvider serializerProviderInstance = objectMapper.getSerializerProviderInstance();
        assertTrue(serializerProviderInstance instanceof DefaultSerializerProvider.Impl);
        DeserializationContext deserializationContext = objectMapper.getDeserializationContext();
        assertTrue(deserializationContext instanceof DefaultDeserializationContext.Impl);
        SubtypeResolver subtypeResolver = objectMapper.getSubtypeResolver();
        assertTrue(subtypeResolver instanceof StdSubtypeResolver);
        JsonFactory factory = objectMapper.getFactory();
        assertSame(factory, objectMapper.getJsonFactory());
        assertNull(deserializationContext.getActiveView());
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
        assertSame(typeFactory, serializationConfig.getTypeFactory());
        assertFalse(serializationConfig.hasExplicitTimeZone());
        DeserializationConfig deserializationConfig = objectMapper.getDeserializationConfig();
        AccessorNamingStrategy.Provider accessorNaming = deserializationConfig.getAccessorNaming();
        assertTrue(accessorNaming instanceof DefaultAccessorNamingStrategy.Provider);
        assertNull(deserializationConfig.getActiveView());
        AnnotationIntrospector annotationIntrospector = deserializationConfig.getAnnotationIntrospector();
        assertTrue(annotationIntrospector instanceof JacksonAnnotationIntrospector);
        ContextAttributes attributes = deserializationConfig.getAttributes();
        assertTrue(attributes instanceof ContextAttributes.Impl);
        ClassIntrospector classIntrospector = deserializationConfig.getClassIntrospector();
        assertTrue(classIntrospector instanceof BasicClassIntrospector);
        assertEquals("x-api-key", ((ApiKeyAuth) authentications.get("ApiKeyAuth")).getParamName());
        assertEquals(31, factory.getGeneratorFeatures());
        assertSame(dateFormat, deserializationConfig.getDateFormat());
        assertNull(deserializationConfig.getDefaultMergeable());
        assertEquals("header", ((ApiKeyAuth) authentications.get("ApiKeyAuth")).getLocation());
        assertSame(objectMapper.getVisibilityChecker(), deserializationConfig.getDefaultVisibilityChecker());
        assertEquals(237020288, deserializationConfig.getDeserializationFeatures());
        assertNull(deserializationConfig.getFullRootName());
        assertEquals(16385, factory.getParserFeatures());
        assertNull(deserializationConfig.getHandlerInstantiator());
        Locale locale = actualInitResult.getOffsetDateTimeFormatter().getLocale();
        assertSame(locale, deserializationConfig.getLocale());
        assertSame(accessorNaming, serializationConfig.getAccessorNaming());
        assertSame(serializerProviderInstance.getDefaultNullValueSerializer(),
                serializerProvider.getDefaultNullValueSerializer());
        assertSame(serializerProviderInstance.getDefaultNullKeySerializer(),
                serializerProvider.getDefaultNullKeySerializer());
        assertNull(serializerProvider.getConfig());
        assertNull(serializerProvider.getActiveView());
        assertFalse(deserializationConfig.hasExplicitTimeZone());
        assertSame(typeFactory, deserializationConfig.getTypeFactory());
        assertSame(subtypeResolver, deserializationConfig.getSubtypeResolver());
        assertNull(deserializationConfig.getPropertyNamingStrategy());
        JsonNodeFactory expectedNodeFactory = objectMapper.getNodeFactory();
        assertSame(expectedNodeFactory, deserializationConfig.getNodeFactory());
        assertSame(polymorphicTypeValidator, deserializationConfig.getPolymorphicTypeValidator());
        assertEquals(" ", factory.getRootValueSeparator());
        assertNull(serializationConfig.getActiveView());
        assertNull(deserializationConfig.getProblemHandlers());
        assertSame(annotationIntrospector, serializationConfig.getAnnotationIntrospector());
        Base64Variant expectedBase64Variant = deserializationConfig.getBase64Variant();
        assertSame(expectedBase64Variant, serializationConfig.getBase64Variant());
        assertTrue(deserializationConfig.isAnnotationProcessingEnabled());
        assertSame(attributes, serializationConfig.getAttributes());
        assertTrue(serializationConfig.isAnnotationProcessingEnabled());
        assertSame(classIntrospector, serializationConfig.getClassIntrospector());
        assertSame(dateFormat, serializationConfig.getDateFormat());
        assertNull(deserializationContext.getConfig());
        assertSame(subtypeResolver, serializationConfig.getSubtypeResolver());
        assertNull(serializationConfig.getDefaultMergeable());
        assertTrue(serializationConfig.getDefaultPrettyPrinter() instanceof DefaultPrettyPrinter);
        assertEquals(JsonInclude.Include.ALWAYS, serializationConfig.getSerializationInclusion());
        assertEquals(21770556, serializationConfig.getSerializationFeatures());
        assertNull(serializationConfig.getRootName());
        assertNull(serializationConfig.getPropertyNamingStrategy());
        assertSame(polymorphicTypeValidator, serializationConfig.getPolymorphicTypeValidator());
        assertSame(locale, serializationConfig.getLocale());
        assertNull(serializationConfig.getHandlerInstantiator());
        assertNull(serializationConfig.getFilterProvider());
        JsonSetter.Value expectedDefaultSetterInfo = deserializationConfig.getDefaultSetterInfo();
        assertSame(expectedDefaultSetterInfo, serializationConfig.getDefaultSetterInfo());
        assertNull(deserializationContext.getParser());
        assertEquals(0, deserializationContext.getDeserializationFeatures());
        assertSame(objectMapper, factory.getCodec());
        DeserializerFactory factory2 = deserializationContext.getFactory();
        assertTrue(factory2 instanceof BeanDeserializerFactory);
        SerializerFactoryConfig factoryConfig = ((BeanSerializerFactory) serializerFactory).getFactoryConfig();
        assertTrue(factoryConfig.hasKeySerializers());
        assertTrue(factoryConfig.hasSerializers());
        assertFalse(factoryConfig.hasSerializerModifiers());
        DeserializerFactoryConfig factoryConfig2 = ((BeanDeserializerFactory) factory2).getFactoryConfig();
        assertFalse(factoryConfig2.hasAbstractTypeResolvers());
        assertTrue(((ArrayIterator<Serializers>) factoryConfig.serializers()).hasNext());
        assertFalse(factoryConfig2.hasDeserializerModifiers());
        assertTrue(factoryConfig2.hasKeyDeserializers());
        assertTrue(factoryConfig2.hasValueInstantiators());
        assertTrue(factoryConfig2.hasDeserializers());
        assertTrue(((ArrayIterator<Deserializers>) factoryConfig2.deserializers()).hasNext());
        verify(responseExchangeFilter).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction2).andThen(Mockito.<ExchangeFilterFunction>any());
        verify(exchangeFilterFunction).apply(Mockito.<ExchangeFunction>any());
    }
}

