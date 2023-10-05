package it.pagopa.pn.address.manager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.BatchRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.model.CapModel;
import it.pagopa.pn.address.manager.model.NormalizedAddress;
import it.pagopa.pn.address.manager.service.CsvService;
import it.pagopa.pn.commons.exceptions.PnInternalException;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    }

    private Map<String, String> getMockedCountryMap() {
        Map<String, String> mockedCountryMap = new HashMap<>();
        mockedCountryMap.put("ITALIA","ITALIA");
        mockedCountryMap.put("AFRICA DEL SUD","SUDAFRICA");
        mockedCountryMap.put("AMERICA","STATI UNITI D'AMERICA");
        return mockedCountryMap;
    }

    private List<CapModel> getMockedCapMap() {
        List<CapModel> mockedCapList = new ArrayList<>();
        mockedCapList.add(new CapModel("00010","Roma","RM"));
        mockedCapList.add( new CapModel("00011","Roma","RM"));
        mockedCapList.add(new CapModel("00012","Roma","RM"));
        return mockedCapList;
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
    void toNormalizeRequestPostelInput(){
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
        assertNotNull(addressUtils.toNormalizeRequestPostelInput(normalizeRequestList,"correlationId"));
    }

    @Test
    void normalizeRequestToPostelCsvRequest(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("12345"); // Imposta i valori appropriati per ciascun campo
        batchRequest.setBatchId("batch-1");
        batchRequest.setRetry(2);
        batchRequest.setTtl(1633449200L); // Esempio di valore per il tempo di vita (TTL) come timestamp Unix
        batchRequest.setClientId("client-123");
        batchRequest.setStatus("In progress");
        batchRequest.setLastReserved(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setReservationId("reservation-1");
        batchRequest.setCreatedAt(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setSendStatus("Pending");
        batchRequest.setMessage("Messaggio di esempio");
        batchRequest.setXApiKey("api-key-123");
        batchRequest.setCxId("cx-123");
        batchRequest.setAwsMessageId("aws-msg-123");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToPostelCsvRequest(batchRequest));
    }

    @Test
    void computeSha256(){
        String inputString = "Hello, World!";

        byte[] content = inputString.getBytes();
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        assertNotNull(addressUtils.computeSha256(content));
    }


    @Test
    void toJson1() throws JsonProcessingException {
        BatchRequest batchRequest = new BatchRequest();
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        assertThrows(PnInternalException.class, () -> addressUtils.toJson(batchRequest));
    }


    @Test
    void getFileCreationRequest(){
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getFileCreationRequest());
    }

    @Test
    void createNewStartBatchRequest(){
        PnAddressManagerConfig.Normalizer n = new PnAddressManagerConfig.Normalizer();
        PnAddressManagerConfig.BatchRequest batchRequest = new PnAddressManagerConfig.BatchRequest();
        batchRequest.setTtl(0);
        n.setBatchRequest(batchRequest);
        when(pnAddressManagerConfig.getNormalizer()).thenReturn(n);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.createNewStartBatchRequest());
    }

    @Test
    void normalizeRequestToResult(){
        NormalizeItemsRequest normalizeItemsRequest = new NormalizeItemsRequest();
        normalizeItemsRequest.setRequestItems(new ArrayList<>());
        normalizeItemsRequest.setCorrelationId("correlationId");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.normalizeRequestToResult(normalizeItemsRequest));
        assertNotNull(addressUtils.mapToAcceptedResponse(normalizeItemsRequest));
    }

    @Test
    void getCorrelationId(){
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getCorrelationId("prova"));
        assertNotNull(addressUtils.getCorrelationId(""));
    }

    @Test
    void toResultItem(){
        NormalizedAddress normalizedAddress = getNormalizedAddress(42);
        NormalizedAddress normalizedAddress1 = getNormalizedAddress(0);
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        List<NormalizedAddress> normalizedAddresses = new ArrayList<>();
        normalizedAddresses.add(normalizedAddress);
        normalizedAddresses.add(normalizedAddress1);
        assertNotNull(addressUtils.toResultItem(normalizedAddresses));

    }

    @NotNull
    private static NormalizedAddress getNormalizedAddress(int nRisultatoNorm) {
        NormalizedAddress normalizedAddress = new NormalizedAddress();
        normalizedAddress.setId("1#1");
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
    void getNormalizeRequestFromBatchRequest(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("12345"); // Imposta i valori appropriati per ciascun campo
        batchRequest.setBatchId("batch-1");
        batchRequest.setRetry(2);
        batchRequest.setTtl(1633449200L); // Esempio di valore per il tempo di vita (TTL) come timestamp Unix
        batchRequest.setClientId("client-123");
        batchRequest.setStatus("In progress");
        batchRequest.setLastReserved(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setReservationId("reservation-1");
        batchRequest.setCreatedAt(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setSendStatus("Pending");
        batchRequest.setMessage("Messaggio di esempio");
        batchRequest.setXApiKey("api-key-123");
        batchRequest.setCxId("cx-123");
        batchRequest.setAwsMessageId("aws-msg-123");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getNormalizeRequestFromBatchRequest(batchRequest));
    }

    @Test
    void getNormalizeRequestFromBatchRequest1(){
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("12345"); // Imposta i valori appropriati per ciascun campo
        batchRequest.setBatchId("batch-1");
        batchRequest.setRetry(2);
        batchRequest.setTtl(1633449200L); // Esempio di valore per il tempo di vita (TTL) come timestamp Unix
        batchRequest.setClientId("client-123");
        batchRequest.setStatus("In progress");
        batchRequest.setLastReserved(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setReservationId("reservation-1");
        batchRequest.setCreatedAt(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setSendStatus("Pending");
        batchRequest.setMessage("Messaggio di esempio");
        batchRequest.setXApiKey("api-key-123");
        batchRequest.setCxId("cx-123");
        batchRequest.setAwsMessageId("aws-msg-123");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        assertNotNull(addressUtils.getNormalizeRequestFromBatchRequest(batchRequest));
    }


    @Test
    void getNormalizeRequestFromBatchRequestNull() throws JsonProcessingException {
        BatchRequest batchRequest = new BatchRequest();
        batchRequest.setCorrelationId("12345"); // Imposta i valori appropriati per ciascun campo
        batchRequest.setAddresses("Indirizzo di esempio");
        batchRequest.setBatchId("batch-1");
        batchRequest.setRetry(2);
        batchRequest.setTtl(1633449200L); // Esempio di valore per il tempo di vita (TTL) come timestamp Unix
        batchRequest.setClientId("client-123");
        batchRequest.setStatus("In progress");
        batchRequest.setLastReserved(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setReservationId("reservation-1");
        batchRequest.setCreatedAt(LocalDateTime.now()); // Esempio di data e ora corrente
        batchRequest.setSendStatus("Pending");
        batchRequest.setMessage("Messaggio di esempio");
        batchRequest.setXApiKey("api-key-123");
        batchRequest.setCxId("cx-123");
        batchRequest.setAwsMessageId("aws-msg-123");
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);
        when(objectMapper.readValue(anyString(), (Class<BatchRequest>) any())).thenThrow(JsonProcessingException.class);
        assertThrows(PnInternalException.class, () -> addressUtils.getNormalizeRequestFromBatchRequest(batchRequest));
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
        assertNotNull(addressUtils.normalizeAddress(base,"1"));
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
        assertNotNull(addressUtils.normalizeAddress(base,"1"));
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
        assertNotNull(addressUtils.normalizeAddress(base, "1"));
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
     */
    @Test
    void testNormalizeAddresses3() {
        AddressUtils addressUtils = new AddressUtils(csvService, pnAddressManagerConfig, objectMapper);

        NormalizeRequest normalizeRequest = new NormalizeRequest();
        normalizeRequest.address(new AnalogAddress());

        ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
        normalizeRequestList.add(normalizeRequest);
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
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
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
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
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
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
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
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
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        NormalizeResult getResult = resultItems.get(0);
        assertEquals("42", getResult.getId());
        verify(normalizeRequest).getAddress();
        verify(normalizeRequest).address(any());
        verify(normalizeRequest).getId();
    }

    /**
     * Method under test: {@link AddressUtils#normalizeAddresses(List)}
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
        List<NormalizeResult> resultItems = addressUtils.normalizeAddresses(normalizeRequestList);
        assertEquals(1, resultItems.size());
        assertEquals("42", resultItems.get(0).getId());
    }
}

