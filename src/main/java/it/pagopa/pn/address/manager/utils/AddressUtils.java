package it.pagopa.pn.address.manager.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.constant.PostelNErrorNorm;
import it.pagopa.pn.address.manager.entity.PnRequest;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.microservice.msclient.generated.pn.safe.storage.v1.dto.FileCreationRequestDto;
import it.pagopa.pn.address.manager.model.*;
import it.pagopa.pn.address.manager.service.CsvService;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.NormalizerCallbackRequest;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.*;
import static it.pagopa.pn.address.manager.constant.BatchStatus.*;
import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_VERIFY_ADDRESS;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@CustomLog
public class AddressUtils {
    private static final String ERROR_DURING_VERIFY_CSV = "Error during verify csv";
    private final List<CapModel> capList;
    private final Map<String, String> countryMap;

    private final PnAddressManagerConfig pnAddressManagerConfig;
    private final ObjectMapper objectMapper;

    public AddressUtils(CsvService csvService, PnAddressManagerConfig pnAddressManagerConfig, ObjectMapper objectMapper) {
        this.capList = csvService.capList();
        this.countryMap = csvService.countryMap();
        this.pnAddressManagerConfig = pnAddressManagerConfig;
        this.objectMapper = objectMapper;
    }

    public boolean compareAddress(AnalogAddress baseAddress, AnalogAddress targetAddress, boolean isItalian) {
        return compare(baseAddress.getAddressRow(), targetAddress.getAddressRow())
                && compare(baseAddress.getAddressRow2(), targetAddress.getAddressRow2())
                && compare(baseAddress.getCap(), targetAddress.getCap())
                && compare(baseAddress.getCity(), targetAddress.getCity())
                && compare(baseAddress.getCity2(), targetAddress.getCity2())
                && compare(baseAddress.getPr(), targetAddress.getPr())
                && (isItalian || compare(baseAddress.getCountry(), targetAddress.getCountry()));
    }

    private boolean compare(String base, String target) {
        String trimmedBase = StringUtils.normalizeSpace(Optional.ofNullable(base).orElse(""));
        String trimmedTarget = StringUtils.normalizeSpace(Optional.ofNullable(target).orElse(""));
        return trimmedBase.equalsIgnoreCase(trimmedTarget);
    }

    public NormalizedAddressResponse normalizeAddress(AnalogAddress analogAddress, String id, String correlationId) {
        NormalizedAddressResponse normalizedAddressResponse = verifyAddress(analogAddress, correlationId);
        normalizedAddressResponse.setId(id);
        if (StringUtils.isBlank(normalizedAddressResponse.getError())) {
            normalizedAddressResponse.setNormalizedAddress(toUpperCase(analogAddress));
        }
        return normalizedAddressResponse;
    }

    private boolean validateAddress(AnalogAddress analogAddress) {
        return validateAddressField(analogAddress.getAddressRow())
                && validateAddressField(analogAddress.getAddressRow2())
                && validateAddressField(analogAddress.getCity())
                && validateAddressField(analogAddress.getCity2())
                && validateAddressField(analogAddress.getCountry())
                && validateAddressField(analogAddress.getPr())
                && validateAddressField(analogAddress.getCap());
    }

    private boolean validateAddressField(String fieldValue) {
        if (!StringUtils.isBlank(fieldValue)) {
            return fieldValue.matches("[" + pnAddressManagerConfig.getValidationPattern() + "]*");
        }
        return true;
    }

    private AnalogAddress toUpperCase(AnalogAddress analogAddress) {
        analogAddress.setAddressRow(Optional.ofNullable(analogAddress.getAddressRow()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCity(Optional.ofNullable(analogAddress.getCity()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCap(Optional.ofNullable(analogAddress.getCap()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setPr(Optional.ofNullable(analogAddress.getPr()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setAddressRow2(Optional.ofNullable(analogAddress.getAddressRow2()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCity2(Optional.ofNullable(analogAddress.getCity2()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setCountry(Optional.ofNullable(analogAddress.getCountry()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        analogAddress.setNameRow2(Optional.ofNullable(analogAddress.getNameRow2()).map(s -> StringUtils.normalizeSpace(s).toUpperCase()).orElse(null));
        return analogAddress;
    }


    private NormalizedAddressResponse verifyAddress(AnalogAddress analogAddress, String correlationId) {
        NormalizedAddressResponse normalizedAddressResponse = new NormalizedAddressResponse();
        log.logChecking(PROCESS_VERIFY_ADDRESS);
        if (Boolean.TRUE.equals(pnAddressManagerConfig.getEnableValidation())
                && !validateAddress(analogAddress)) {
            log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, false, "Address contains invalid characters");
            log.error("Error during verifyAddressInCsv for {}: Address contains invalid characters", correlationId);
            normalizedAddressResponse.setError("Address contains invalid characters");
            return normalizedAddressResponse;
        }

        try {
            verifyAddressInCsv(analogAddress, normalizedAddressResponse);
        } catch (PnInternalAddressManagerException e) {
            log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, false, e.getDescription());
            log.error("Error during verifyAddressInCsv for {}: {}", correlationId, e.getDescription(), e);
            normalizedAddressResponse.setError(e.getDescription());
        }

        log.logCheckingOutcome(PROCESS_VERIFY_ADDRESS, true);
        return normalizedAddressResponse;
    }

    private void verifyAddressInCsv(AnalogAddress analogAddress, NormalizedAddressResponse normalizedAddressResponse) {
        if (StringUtils.isBlank(analogAddress.getCountry())
                || analogAddress.getCountry().toUpperCase().trim().startsWith("ITAL")) {
            normalizedAddressResponse.setItalian(true);
            verifyCapAndCity(analogAddress);
        } else {
            searchCountry(analogAddress.getCountry(), countryMap);
        }
    }

    private void verifyCapAndCity(AnalogAddress analogAddress) {
        if (StringUtils.isBlank(analogAddress.getCap())
                || StringUtils.isBlank(analogAddress.getCity())
                || StringUtils.isBlank(analogAddress.getPr())) {
            throw new PnInternalAddressManagerException(ERROR_DURING_VERIFY_CSV, "Cap, city and Province are mandatory", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        } else if (!compareWithCapModelObject(analogAddress)) {
            throw new PnInternalAddressManagerException(ERROR_DURING_VERIFY_CSV, "Invalid Address, Cap, City and Province: [" + analogAddress.getCap() + "," + analogAddress.getCity() + "," + analogAddress.getPr() + "]", HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_CAPNOTFOUND);
        }
    }

    private boolean compareWithCapModelObject(AnalogAddress analogAddress) {
        return capList.stream()
                .anyMatch(capModel -> capModel.getCap().equalsIgnoreCase(analogAddress.getCap().trim())
                        && capModel.getProvince().equalsIgnoreCase(analogAddress.getPr().trim())
                        && capModel.getCity().equalsIgnoreCase(analogAddress.getCity().trim()));
    }

    private void searchCountry(String country, Map<String, String> countryMap) {
        String normalizedCountry = StringUtils.normalizeSpace(country.toUpperCase());
        if (!countryMap.containsKey(normalizedCountry)) {
            throw new PnInternalAddressManagerException(ERROR_DURING_VERIFY_CSV, String.format("Country not found: [%s]", normalizedCountry), HttpStatus.BAD_REQUEST.value(), ERROR_CODE_ADDRESS_MANAGER_COUNTRYNOTFOUND);
        }
    }

    public List<NormalizeResult> normalizeAddresses(List<NormalizeRequest> requestItems, String correlationId) {
        return requestItems.stream()
                .map(normalizeRequest -> normalizeAddress(normalizeRequest.getAddress(), normalizeRequest.getId(), correlationId))
                .map(this::toNormalizeResult)
                .toList();
    }

    private NormalizeResult toNormalizeResult(NormalizedAddressResponse response) {
        NormalizeResult normalizeResult = new NormalizeResult();
        normalizeResult.setId(response.getId());
        normalizeResult.setError(response.getError());
        normalizeResult.setNormalizedAddress(response.getNormalizedAddress());
        return normalizeResult;
    }

    public List<NormalizeRequestPostelInput> toNormalizeRequestPostelInput(List<NormalizeRequest> normalizeRequestList, String correlationId, LocalDateTime createdAt) {
        return normalizeRequestList.stream()
                .map(normalizeRequest -> {
                    NormalizeRequestPostelInput normalizeRequestPostelInput = new NormalizeRequestPostelInput();
                    normalizeRequestPostelInput.setIdCodiceCliente(correlationId + "#" + createdAt + "#" + normalizeRequest.getId());
                    normalizeRequestPostelInput.setCap(normalizeRequest.getAddress().getCap());
                    normalizeRequestPostelInput.setLocalita(normalizeRequest.getAddress().getCity());
                    normalizeRequestPostelInput.setProvincia(normalizeRequest.getAddress().getPr());
                    normalizeRequestPostelInput.setIndirizzo(normalizeRequest.getAddress().getAddressRow());
                    normalizeRequestPostelInput.setIndirizzoAggiuntivo(normalizeRequest.getAddress().getAddressRow2());
                    normalizeRequestPostelInput.setStato(normalizeRequest.getAddress().getCountry());
                    normalizeRequestPostelInput.setLocalitaAggiuntiva(normalizeRequest.getAddress().getCity2());

                    return normalizeRequestPostelInput;
                }).toList();
    }

    public List<NormalizeRequest> getNormalizeRequestFromBatchRequest(PnRequest pnRequest) {
        return toObject(pnRequest.getAddresses(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, NormalizeRequest.class));
    }

    public List<NormalizeResult> getNormalizeResultFromBatchRequest(PnRequest pnRequest) {
        return toObject(pnRequest.getMessage(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, NormalizeResult.class));
    }

    public List<NormalizeRequestPostelInput> normalizeRequestToPostelCsvRequest(PnRequest pnRequest) {
        return toNormalizeRequestPostelInput(getNormalizeRequestFromBatchRequest(pnRequest), pnRequest.getCorrelationId(), pnRequest.getCreatedAt());
    }

    public String computeSha256(byte[] content) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(content);
            return bytesToBase64(encodedHash);
        } catch (Exception e) {
            throw new PnInternalException("Error during compute sha245", "ERROR_GENERATE_SHA256");
        }
    }

    private static String bytesToBase64(byte[] hash) {
        return Base64Utils.encodeToString(hash);
    }

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_HANDLEEVENTFAILED, ERROR_CODE_ADDRESS_MANAGER_HANDLEEVENTFAILED, e);
        }
    }

    public <T> T toObject(String json, Class<T> newClass) {
        try {
            return objectMapper.readValue(json, newClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_HANDLEEVENTFAILED, ERROR_CODE_ADDRESS_MANAGER_HANDLEEVENTFAILED, e);
        }
    }

    public <T> T toObject(String json, CollectionType newClass) {
        try {
            return objectMapper.readValue(json, newClass);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_HANDLEEVENTFAILED, ERROR_CODE_ADDRESS_MANAGER_HANDLEEVENTFAILED, e);
        }
    }

    public FileCreationRequestDto getFileCreationRequest() {
        FileCreationRequestDto fileCreationRequestDto = new FileCreationRequestDto();
        fileCreationRequestDto.setContentType(CONTENT_TYPE);
        fileCreationRequestDto.setStatus(SAFE_STORAGE_STATUS);
        fileCreationRequestDto.setDocumentType(DOCUMENT_TYPE);
        return fileCreationRequestDto;
    }

    public PnRequest createNewStartBatchRequest() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PnRequest pnRequest = new PnRequest();
        pnRequest.setBatchId(BatchStatus.NO_BATCH_ID.getValue());
        pnRequest.setStatus(BatchStatus.NOT_WORKED.getValue());
        pnRequest.setRetry(0);
        pnRequest.setLastReserved(now);
        pnRequest.setCreatedAt(now);
        log.trace("New Batch Request: {}", pnRequest);
        return pnRequest;
    }

    public NormalizeItemsResult normalizeRequestToResult(NormalizeItemsRequest normalizeItemsRequest) {
        NormalizeItemsResult normalizeItemsResult = new NormalizeItemsResult();
        normalizeItemsResult.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        normalizeItemsResult.setResultItems(normalizeAddresses(normalizeItemsRequest.getRequestItems(), normalizeItemsRequest.getCorrelationId()));
        return normalizeItemsResult;
    }

    public AcceptedResponse mapToAcceptedResponse(NormalizeItemsRequest normalizeItemsRequest) {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        return acceptedResponse;
    }

    public List<NormalizeResult> toResultItem(List<NormalizedAddress> normalizedAddresses, PnRequest pnRequest) {
        return normalizedAddresses.stream().map(normalizedAddress -> {
            NormalizeResult result = new NormalizeResult();
            String[] index = normalizedAddress.getId().split("#");
            if (index.length == 3) {
                result.setId(index[2]);
                log.info("Address with correlationId: [{}], createdAt: [{}] and index: [{}] has FPostalizzabile = {}, NRisultatoNorm = {}, NErroreNorm = {}", index[0], index[1], index[2],
                        normalizedAddress.getFPostalizzabile(), normalizedAddress.getNRisultatoNorm(), normalizedAddress.getNErroreNorm());
                if (normalizedAddress.getFPostalizzabile() != null && normalizedAddress.getFPostalizzabile() == 0) {
                    String errorMessage = evaluatedErrorToUpdateBatchRequest(normalizedAddress, index, pnRequest);
                    result.setError(errorMessage);
                } else {
                    result.setNormalizedAddress(toAnalogAddress(normalizedAddress));
                }
            }
            return result;
        }).toList();
    }

    private String evaluatedErrorToUpdateBatchRequest(NormalizedAddress normalizedAddress, String[] index, PnRequest pnRequest) {
        if(normalizedAddress.getNErroreNorm() != null){
            PostelNErrorNorm error = PostelNErrorNorm.fromCode(normalizedAddress.getNErroreNorm());
            log.warn("Error during normalize address: correlationId: [{}] and index: [{}] - error: {}", index[0], index[1], error.getDescription());
            switch (error){
                case ERROR_901, ERROR_997, ERROR_998:
                    pnRequest.setStatus(TAKEN_CHARGE.getValue());
                    break;
                case ERROR_999:
                    pnRequest.setStatus(ERROR.getValue());
                    break;
                default:
                    pnRequest.setStatus(WORKED.getValue());
            }
        }else{
            log.warn("Error during normalize address: correlationId: [{}] and index: [{}] - error: {}", index[0], index[1], "Errore non presente");
        }
        return PNADDR001_MESSAGE;
    }

    private AnalogAddress toAnalogAddress(NormalizedAddress normalizedAddress) {
        AnalogAddress analogAddress = new AnalogAddress();
        analogAddress.setAddressRow(normalizedAddress.getSViaCompletaSpedizione());
        analogAddress.setAddressRow2(normalizedAddress.getSCivicoAltro());
        analogAddress.setCap(normalizedAddress.getSCap());
        analogAddress.setCity(normalizedAddress.getSComuneSpedizione());
        analogAddress.setCity2(normalizedAddress.getSFrazioneSpedizione());
        analogAddress.setPr(normalizedAddress.getSSiglaProv());
        analogAddress.setCountry(normalizedAddress.getSStatoSpedizione());
        return analogAddress;
    }


    public String getCorrelationIdCreatedAt(String id) {
        if (org.springframework.util.StringUtils.hasText(id)) {
            String[] splittedId = id.split("#");
            if(splittedId.length == 3) {
                return splittedId[0] + "#" + splittedId[1];
            }
        }
        return "noCorrelationId";
    }

    public String getCorrelationIdCreatedAt(PnRequest pnRequest) {
        return pnRequest.getCorrelationId() + "#" + pnRequest.getCreatedAt();
    }

    public PostelCallbackSqsDto getPostelCallbackSqsDto(NormalizerCallbackRequest callbackRequest, String batchId) {
        return PostelCallbackSqsDto.builder()
                .requestId(batchId)
                .outputFileKey(callbackRequest.getUri())
                .error(callbackRequest.getError())
                .build();
    }

    public static Duration getTimeSpent(Instant start) {
        Instant end = Instant.now();
        return Duration.between(start, end);
    }
}
