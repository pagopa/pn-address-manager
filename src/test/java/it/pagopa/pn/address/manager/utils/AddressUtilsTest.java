package it.pagopa.pn.address.manager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.ForeignValidationMode;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.service.CsvService;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.NormalizerCallbackRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddressUtilsTest {

    @Mock
    private CsvService csvService;

    @Mock
    private PnAddressManagerConfig pnAddressManagerConfig;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        when(csvService.capList()).thenReturn(getMockedCapMap());
        when(csvService.countryMap()).thenReturn(getMockedCountryMap());
        when(pnAddressManagerConfig.getEnableValidation()).thenReturn(true);
        when(pnAddressManagerConfig.getFlagCsv()).thenReturn(true);
        when(pnAddressManagerConfig.getValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig.getForeignValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig.getForeignValidationMode()).thenReturn(ForeignValidationMode.PASSTHROUGH);

    }

    private Map<String, String> getMockedCountryMap() {
        Map<String, String> mockedCountryMap = new HashMap<>();
        mockedCountryMap.put("ITALIA", "ITALIA");
        mockedCountryMap.put("AFRICA DEL SUD", "SUDAFRICA");
        mockedCountryMap.put("AMERICA", "STATI UNITI D'AMERICA");
        mockedCountryMap.put("???", "???");

        return mockedCountryMap;
    }

    private List<CapModel> getMockedCapMap() {
        List<CapModel> mockedCapList = new ArrayList<>();
        mockedCapList.add(new CapModel("00010", "Roma", "RM"));
        mockedCapList.add(new CapModel("00011", "Roma", "RM"));
        mockedCapList.add(new CapModel("00012", "Roma", "RM"));
        return mockedCapList;
    }


    @Test
    void getNormalizeRequestFromBatchRequest() throws JsonProcessingException {
        NormalizeRequest normalizeRequest = getNormalizeRequest();
        objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(List.of(normalizeRequest));

        PnRequest pnRequest = new PnRequest();
        pnRequest.setAddresses(json);
        pnRequest.setMessage("message");

        objectMapper.setTypeFactory(TypeFactory.defaultInstance());
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getNormalizeRequestFromBatchRequest(pnRequest));
        assertThrows(PnInternalException.class, () -> addressUtils.getNormalizeResultFromBatchRequest(pnRequest));
    }

    @NotNull
    private static NormalizeRequest getNormalizeRequest() {
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow("123 Main St");
        analogAddress.setAddressRow2("Apt 4B");
        analogAddress.setCap("12345");
        analogAddress.setCity("Sample City");
        analogAddress.setCity2("Suburb");
        analogAddress.setPr("CA");
        analogAddress.setCountry("USA");
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.setId("id");
        normalizeRequest.setAddress(analogAddress);
        return normalizeRequest;
    }

    @Test
    void getCorrelationIdCreatedAt() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        PnRequest batchRequest = new PnRequest();
        LocalDateTime localDateTime = LocalDateTime.now();
        batchRequest.setCorrelationId("correlationId");
        batchRequest.setCreatedAt(localDateTime);
        assertEquals(addressUtils.getCorrelationIdCreatedAt(batchRequest), "correlationId#" + localDateTime);
    }

    @Test
    void normalizeAddress10() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("AFRICA DEL SUD");
        base.setCap("42");
        assertNotNull(addressUtils.normalizeAddress(base, "42", "correlationid"));
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(new AnalogAddress(), "42",
                "42");
        assertEquals("Cap, city and Province are mandatory", actualNormalizeAddressResult.getError());
        assertTrue(actualNormalizeAddressResult.isItalian());
        assertEquals("42", actualNormalizeAddressResult.getId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress3() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("GB");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Country not found: [GB]", actualNormalizeAddressResult.getError());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress, atLeast(1)).getCountry();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress4() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("[VERIFY] verify address");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Country not found: [[VERIFY] VERIFY ADDRESS]", actualNormalizeAddressResult.getError());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress, atLeast(1)).getCountry();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress5() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getPr()).thenReturn("Pr");
        when(analogAddress.getCity()).thenReturn("Oxford");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("ITAL");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Invalid Address, Cap, City and Province: [Cap,Oxford,Pr]", actualNormalizeAddressResult.getError());
        assertTrue(actualNormalizeAddressResult.isItalian());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress, atLeast(1)).getCap();
        verify(analogAddress, atLeast(1)).getCity();
        verify(analogAddress, atLeast(1)).getCountry();
        verify(analogAddress, atLeast(1)).getPr();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress6() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getPr()).thenThrow(new PnInternalAddressManagerException("An error occurred",
                "The characteristics of someone or something", 2, "An error occurred"));
        when(analogAddress.getCity()).thenReturn("Oxford");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("ITAL");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("The characteristics of someone or something", actualNormalizeAddressResult.getError());
        assertTrue(actualNormalizeAddressResult.isItalian());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress).getCap();
        verify(analogAddress).getCity();
        verify(analogAddress, atLeast(1)).getCountry();
        verify(analogAddress).getPr();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress7() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getPr()).thenReturn("");
        when(analogAddress.getCity()).thenReturn("Oxford");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("ITAL");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Cap, city and Province are mandatory", actualNormalizeAddressResult.getError());
        assertTrue(actualNormalizeAddressResult.isItalian());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress).getCap();
        verify(analogAddress).getCity();
        verify(analogAddress, atLeast(1)).getCountry();
        verify(analogAddress).getPr();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress8() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getPr()).thenReturn("Pr");
        when(analogAddress.getCity()).thenReturn("");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCountry()).thenReturn("ITAL");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Cap, city and Province are mandatory", actualNormalizeAddressResult.getError());
        assertTrue(actualNormalizeAddressResult.isItalian());
        assertEquals("42", actualNormalizeAddressResult.getId());
        verify(analogAddress).getCap();
        verify(analogAddress).getCity();
        verify(analogAddress, atLeast(1)).getCountry();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress9() {
        PnAddressManagerConfig pnAddressManagerConfig1 = mock(PnAddressManagerConfig.class);
        when(pnAddressManagerConfig1.getEnableValidation()).thenReturn(true);
        when(pnAddressManagerConfig1.getFlagCsv()).thenReturn(true);
        when(pnAddressManagerConfig1.getValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig1.getForeignValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig1.getForeignValidationMode()).thenReturn(ForeignValidationMode.PATTERN);


        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig1, objectMapper);

        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCountry("AMERICA");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("AMERICA", actualNormalizeAddressResult.getNormalizedAddress().getCountry());
        assertEquals("42", actualNormalizeAddressResult.getId());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddress(AnalogAddress, String, String)}
     */
    @Test
    void testNormalizeAddress10() {
        PnAddressManagerConfig pnAddressManagerConfig1 = mock(PnAddressManagerConfig.class);
        when(pnAddressManagerConfig1.getEnableValidation()).thenReturn(true);
        when(pnAddressManagerConfig1.getFlagCsv()).thenReturn(true);
        when(pnAddressManagerConfig1.getValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig1.getForeignValidationPattern()).thenReturn("01234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ./ '-");
        when(pnAddressManagerConfig1.getForeignValidationMode()).thenReturn(ForeignValidationMode.PATTERN);


        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig1, objectMapper);

        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCountry("???");
        NormalizedAddressResponse actualNormalizeAddressResult = addressUtils.normalizeAddress(analogAddress, "42", "42");
        assertEquals("Address contains invalid characters", actualNormalizeAddressResult.getError());
        assertEquals("42", actualNormalizeAddressResult.getId());
    }

    @Test
    void toJson1() {
        NormalizeRequest normalizeRequest = getNormalizeRequest();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.toJson(normalizeRequest));
    }

    @Test
    void compareAddress() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertTrue(addressUtils.compareAddress(base, base, true));
    }

    @Test
    void toObject() throws JsonProcessingException {
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("correlationId");
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        normalizeRequest.setAddress(base);
        normalizeRequest.setId("id");
        objectMapper = new ObjectMapper();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        NormalizeRequest normalizeRequest1 = addressUtils.toObject(objectMapper.writeValueAsString(normalizeRequest), NormalizeRequest.class);
        assertNotNull(normalizeRequest1);
        assertThrows(PnInternalException.class, () -> addressUtils.toObject("", NormalizeRequest.class));
    }

    @Test
    void getPostelCallbackSqsDto() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getPostelCallbackSqsDto(new NormalizerCallbackRequest(), "batchId"));
    }

    @Test
    void normalizeRequestToPostelCsvRequest() throws JsonProcessingException {
        PnRequest pnRequest = new PnRequest();
        pnRequest.setCorrelationId("correlationId");
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        normalizeRequest.setAddress(base);
        normalizeRequest.setId("id");
        objectMapper = new ObjectMapper();
        pnRequest.setAddresses(objectMapper.writeValueAsString(List.of(normalizeRequest)));
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToPostelCsvRequest(pnRequest));
    }

    @Test
    void toNormalizeRequestPostelInput() {
        List<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        normalizeRequest.setAddress(base);
        normalizeRequest.setId("id");
        normalizeRequestList.add(normalizeRequest);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.toNormalizeRequestPostelInput(normalizeRequestList, "correlationId", LocalDateTime.now()));
    }


    @Test
    void computeSha256() {
        String inputString = "Hello, World!";

        byte[] content = inputString.getBytes();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        assertNotNull(addressUtils.computeSha256(content));
    }

    @Test
    void getFileCreationRequest() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getFileCreationRequest());
    }

    @Test
    void computeSha2561() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        assertThrows(PnInternalException.class, () -> addressUtils.computeSha256(null));
    }

    @Test
    void createNewStartBatchRequest() {
        PnAddressManagerConfig.Normalizer n = new PnAddressManagerConfig.Normalizer();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setTtl(0);
        n.setBatchRequest(batchRequest);
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setNormalizer(n);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.createNewStartBatchRequest());
    }

    @Test
    void normalizeRequestToResult() {
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setRequestItems(new ArrayList<>());
        normalizeItemsRequest.setCorrelationId("correlationId");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToResult(normalizeItemsRequest));
        assertNotNull(addressUtils.mapToAcceptedResponse(normalizeItemsRequest));
    }

    @Test
    void normalizeRequestToResult1() {
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCountry("AMERICA");
        analogAddress.setAddressRow("abc");
        analogAddress.setAddressRow2("");
        analogAddress.setCity("abc");
        analogAddress.setPr("abc");
        analogAddress.setCap(null);
        normalizeRequest.setAddress(analogAddress);
        normalizeItemsRequest.setRequestItems(List.of(normalizeRequest));

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToResult(normalizeItemsRequest));
        assertNotNull(addressUtils.mapToAcceptedResponse(normalizeItemsRequest));
    }

    @Test
    void normalizeRequestToResult2() {
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setCorrelationId("correlationId");
        NormalizeRequest normalizeRequest = new NormalizeRequest();
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCountry("???");
        analogAddress.setAddressRow(")))))");
        analogAddress.setAddressRow2("");
        analogAddress.setCity("abc");
        analogAddress.setPr("abc");
        analogAddress.setCap(null);
        normalizeRequest.setAddress(analogAddress);
        normalizeItemsRequest.setRequestItems(List.of(normalizeRequest));

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToResult(normalizeItemsRequest));
        assertNotNull(addressUtils.mapToAcceptedResponse(normalizeItemsRequest));
        NormalizedAddressResponse normalizedAddressResponse = addressUtils.normalizeAddress(analogAddress, "", "");
    }

    @Test
    void getCorrelationId() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getCorrelationIdCreatedAt("prova#id#id"));
        assertNotNull(addressUtils.getCorrelationIdCreatedAt(""));
    }

    @Test
    void toResultItem() {
        NormalizedAddress normalizedAddress = getNormalizedAddress(42);
        NormalizedAddress normalizedAddress1 = getNormalizedAddress(0);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        List<NormalizedAddress> normalizedAddresses = new ArrayList<>();
        normalizedAddresses.add(normalizedAddress);
        normalizedAddresses.add(normalizedAddress1);
        assertNotNull(addressUtils.toResultItem(normalizedAddresses, new PnRequest()));
    }

    @Test
    void toResultItemNotPostalizzabile() {
        NormalizedAddress normalizedAddress = getNormalizedAddressNotPostalizzabile(42);
        NormalizedAddress normalizedAddress1 = getNormalizedAddressNotPostalizzabile(0);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        List<NormalizedAddress> normalizedAddresses = new ArrayList<>();
        normalizedAddresses.add(normalizedAddress);
        normalizedAddresses.add(normalizedAddress1);
        assertNotNull(addressUtils.toResultItem(normalizedAddresses, new PnRequest()));
    }

    @Test
    void toResultItemNotPostalizzabileERROR_901() {
        NormalizedAddress normalizedAddress = getNormalizedAddressNotPostalizzabile(42);
        NormalizedAddress normalizedAddress1 = getNormalizedAddressNotPostalizzabile(0);
        normalizedAddress.setNErroreNorm(901);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        List<NormalizedAddress> normalizedAddresses = new ArrayList<>();
        normalizedAddresses.add(normalizedAddress);
        normalizedAddresses.add(normalizedAddress1);
        assertNotNull(addressUtils.toResultItem(normalizedAddresses, new PnRequest()));
    }

    @Test
    void toResultItemNotPostalizzabileERROR_999() {
        NormalizedAddress normalizedAddress = getNormalizedAddressNotPostalizzabile(42);
        NormalizedAddress normalizedAddress1 = getNormalizedAddressNotPostalizzabile(0);
        normalizedAddress.setNErroreNorm(999);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        List<NormalizedAddress> normalizedAddresses = new ArrayList<>();
        normalizedAddresses.add(normalizedAddress);
        normalizedAddresses.add(normalizedAddress1);
        assertNotNull(addressUtils.toResultItem(normalizedAddresses, new PnRequest()));
    }

    @NotNull
    private static NormalizedAddress getNormalizedAddressNotPostalizzabile(int nRisultatoNorm) {
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("1#1#1");
        normalizedAddress.setNRisultatoNorm(nRisultatoNorm);
        normalizedAddress.setNErroreNorm(0);
        normalizedAddress.setSSiglaProv("MI");
        normalizedAddress.setFPostalizzabile(0);
        normalizedAddress.setSStatoUff("Lombardia");
        normalizedAddress.setSStatoAbb("LO");
        normalizedAddress.setSStatoSpedizione("Lombardy");
        normalizedAddress.setSComuneUff("Milano");
        normalizedAddress.setSComuneAbb("Milan");
        normalizedAddress.setSComuneSpedizione("Milan City");
        normalizedAddress.setSFrazioneUff("Frazione Ufficio");
        normalizedAddress.setSFrazioneAbb("Frazione Abbreviata");
        normalizedAddress.setSFrazioneSpedizione("Frazione Spedizione");
        normalizedAddress.setSCivicoAltro("Altro Civico");
        normalizedAddress.setSCap("20100");
        normalizedAddress.setSPresso("Presso Qualcuno");
        normalizedAddress.setSViaCompletaUff("Via Ufficio Completa");
        normalizedAddress.setSViaCompletaAbb("Via Abbreviata Completa");
        normalizedAddress.setSViaCompletaSpedizione("Via Spedizione Completa");
        return normalizedAddress;
    }

    @NotNull
    private static NormalizedAddress getNormalizedAddress(int nRisultatoNorm) {
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("1#1#1");
        normalizedAddress.setNRisultatoNorm(nRisultatoNorm);
        normalizedAddress.setNErroreNorm(0);
        normalizedAddress.setSSiglaProv("MI");
        normalizedAddress.setFPostalizzabile(1);
        normalizedAddress.setSStatoUff("Lombardia");
        normalizedAddress.setSStatoAbb("LO");
        normalizedAddress.setSStatoSpedizione("Lombardy");
        normalizedAddress.setSComuneUff("Milano");
        normalizedAddress.setSComuneAbb("Milan");
        normalizedAddress.setSComuneSpedizione("Milan City");
        normalizedAddress.setSFrazioneUff("Frazione Ufficio");
        normalizedAddress.setSFrazioneAbb("Frazione Abbreviata");
        normalizedAddress.setSFrazioneSpedizione("Frazione Spedizione");
        normalizedAddress.setSCivicoAltro("Altro Civico");
        normalizedAddress.setSCap("20100");
        normalizedAddress.setSPresso("Presso Qualcuno");
        normalizedAddress.setSViaCompletaUff("Via Ufficio Completa");
        normalizedAddress.setSViaCompletaAbb("Via Abbreviata Completa");
        normalizedAddress.setSViaCompletaSpedizione("Via Spedizione Completa");
        return normalizedAddress;
    }

    @Test
    void compareAddress1() {
        AnalogAddress base = new AnalogAddress();
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCountry("42");
        base.setCap("42");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertTrue(addressUtils.compareAddress(base, base, false));
    }

    @Test
    void compareAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        AnalogAddress target = new AnalogAddress();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertFalse(addressUtils.compareAddress(base, target, true));
    }

    @Test
    void normalizeAddress() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("00010");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeAddress(base, "1", "correlationid"));
    }


    @Test
    void normalizeAddress0() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma ");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("@@@");
        base.setCap("00010");
        pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setEnableValidation(true);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeAddress(base, "1", "correlationid"));
    }


    @Test
    void normalizeAddress1() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("Roma ");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("RM  ");
        base.setCap("00010");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeAddress(base, "1", "correlationid"));
    }

    @Test
    void normalizeAddress2() {
        AnalogAddress base = new AnalogAddress();
        base.setCity("42");
        base.setCity2("42");
        base.setAddressRow("42");
        base.setAddressRow2("42");
        base.setPr("42");
        base.setCap("ARUBA");
        base.setCountry("ARUBA");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeAddress(base, "1", "correlationid"));
    }

    /**
     * Method under test: {@link AddressUtils#validateForeignAddress(AnalogAddress)}
     */
    @Test
    void testValidateForeignAddress2() {

        CsvService csvService = mock(CsvService.class);
        when(csvService.capList()).thenReturn(new ArrayList<>());
        when(csvService.countryMap()).thenReturn(new HashMap<>());
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, new ObjectMapper());
        addressUtils.validateForeignAddress(new AnalogAddress());
        verify(csvService).capList();
        verify(csvService).countryMap();
    }
    /**
     * Method under test: {@link AddressUtils#validateForeignAddress(AnalogAddress)}
     */
    @Test
    void testValidateForeignAddress3() {

        CsvService csvService = mock(CsvService.class);
        when(csvService.capList()).thenReturn(new ArrayList<>());
        when(csvService.countryMap()).thenReturn(new HashMap<>());
        PnAddressManagerConfig pnAddressManagerConfig = new PnAddressManagerConfig();
        pnAddressManagerConfig.setForeignValidationMode(ForeignValidationMode.PATTERN);
        pnAddressManagerConfig.setForeignValidationPattern("123");
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setCountry("ALBANIA");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, new ObjectMapper());
        addressUtils.validateForeignAddress(analogAddress);
        verify(csvService).capList();
        verify(csvService).countryMap();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List, String)}
     */
    @Test
    void testNormalizeAddresses3() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List, String)}
     */
    @Test
    void testNormalizeAddresses4() {

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(new AnalogAddress());
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List, String)}
     */
    @Test
    void testNormalizeAddresses6() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00010");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        when(analogAddress.getPr()).thenReturn("RM");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    @Test
    void testNormalizeAddresses7() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00010");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    @Test
    void testNormalizeAddresses8() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getCap()).thenReturn("00015");
        when(analogAddress.getCountry()).thenReturn("ITALIA");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List, String)}
     */
    @Test
    void testNormalizeAddresses9() {

        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        AnalogAddress analogAddress = mock(AnalogAddress.class);
        when(analogAddress.getAddressRow2()).thenReturn("42 Main St");
        when(analogAddress.getCap()).thenReturn("Cap");
        when(analogAddress.getCity2()).thenReturn("Oxford");
        when(analogAddress.getCountry()).thenReturn("GB");
        when(analogAddress.getPr()).thenReturn("Pr");
        when(analogAddress.getCity()).thenReturn("Oxford");
        when(analogAddress.getAddressRow()).thenReturn("42 Main St");
        NormalizeRequest normalizeRequest = mock(NormalizeRequest.class);
        when(normalizeRequest.getAddress()).thenReturn(analogAddress);
        when(normalizeRequest.address(any())).thenReturn(new NormalizeRequest());
        when(normalizeRequest.getId()).thenReturn("42");
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList, "correlationid");
        assertEquals(1, resultItems.size());
        assertEquals("42", resultItems.get(0).getId());
    }
}

