package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.*;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
public class AddressConverter {

    private final PnAddressManagerConfig pnAddressManagerConfig;

    public AddressConverter(PnAddressManagerConfig pnAddressManagerConfig) {
        this.pnAddressManagerConfig = pnAddressManagerConfig;
    }

    public DeduplicaRequest createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest deduplicatesRequest) {
        DeduplicaRequest inputDeduplica = new DeduplicaRequest();

        ConfigIn configIn = new ConfigIn();
        inputDeduplica.setConfigIn(configIn);

        AddressIn slaveIn = getAddressIn(deduplicatesRequest);
        inputDeduplica.setSlaveIn(slaveIn);

        AddressIn masterIn = getAddressIn(deduplicatesRequest);
        inputDeduplica.setMasterIn(masterIn);

        return inputDeduplica;
    }

    @NotNull
    private static AddressIn getAddressIn(DeduplicatesRequest deduplicatesRequest) {
        AddressIn addressIn = new AddressIn();
        addressIn.setId(deduplicatesRequest.getCorrelationId());
        addressIn.setProvincia(deduplicatesRequest.getBaseAddress().getPr());
        addressIn.setLocalita(deduplicatesRequest.getBaseAddress().getCity());
        addressIn.setIndirizzo(deduplicatesRequest.getBaseAddress().getAddressRow() + " " + deduplicatesRequest.getBaseAddress().getAddressRow2());
        addressIn.setCap(deduplicatesRequest.getBaseAddress().getCap());
        addressIn.setLocalitaAggiuntiva(deduplicatesRequest.getBaseAddress().getCity2());
        addressIn.setStato(deduplicatesRequest.getBaseAddress().getCountry());
        return addressIn;
    }

    public DeduplicatesResponse createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse risultatoDeduplica, String correlationId) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(correlationId);

        if (risultatoDeduplica.getErrore() != null) {
            throw new PnInternalAddressManagerException(ERROR_CODE_ADDRESSMANAGER_DEDUPLICAERROR,
                    decodeErrorDedu(risultatoDeduplica.getErrore()),
                    HttpStatus.BAD_REQUEST.value(),
                    ERROR_CODE_ADDRESSMANAGER_DEDUPLICAERROR);
        }

        getAnalogAddress(risultatoDeduplica, deduplicatesResponse);

        return deduplicatesResponse;
    }

    private static String decodeErroreNorm(Integer error) {
        if (error != null) {
            //TODO: CHIEDERE INFORMAZIONI SULLA DECODIFICA DEL CODICE ERRORE DEDU
            return "TODO";
        } else {
            return null;
        }
    }

    private static String decodeErrorDedu(String erroreDedu) {
        if (erroreDedu != null) {
            //TODO: CHIEDERE INFORMAZIONI SULLA DECODIFICA DEL CODICE ERRORE DEDU
            return "TODO";
        } else {
            return null;
        }
    }

    @NotNull
    private static DeduplicatesResponse getAnalogAddress(DeduplicaResponse risultatoDeduplica, DeduplicatesResponse deduplicatesResponse) {

        if (risultatoDeduplica.getSlaveOut() != null) {
            if (StringUtils.hasText(risultatoDeduplica.getSlaveOut().getfPostalizzabile())
                    && risultatoDeduplica.getSlaveOut().getfPostalizzabile().equalsIgnoreCase("0")) {
                deduplicatesResponse.setError(decodeErroreNorm(risultatoDeduplica.getSlaveOut().getnErroreNorm()));
                return deduplicatesResponse;
            }
            AnalogAddress analogAddress = getAddress(risultatoDeduplica.getSlaveOut());
            deduplicatesResponse.setNormalizedAddress(analogAddress);
            deduplicatesResponse.setEqualityResult(risultatoDeduplica.getRisultatoDedu());
            return deduplicatesResponse;
        } else {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL, ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL);
        }
    }

    @NotNull
    private static AnalogAddress getAddress(AddressOut slaveOut) {
        AnalogAddress target = new AnalogAddress();
        target.setAddressRow(slaveOut.getsViaCompletaSpedizione());
        target.setAddressRow2(slaveOut.getsCivicoAltro());
        target.setCity(slaveOut.getsComuneSpedizione());
        target.setCity2(slaveOut.getsFrazioneSpedizione());
        target.setCap(slaveOut.getsCap());
        target.setPr(slaveOut.getsSiglaProv());
        target.setCountry(slaveOut.getsStatoSpedizione());
        return target;
    }

    public PostelBatch createPostelBatchByBatchIdAndFileKey(String batchId, String fileKey, String sha256) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PostelBatch batchPolling = new PostelBatch();
        batchPolling.setBatchId(pnAddressManagerConfig.getNormalizer().getPostel().getRequestPrefix() + batchId);
        batchPolling.setFileKey(fileKey);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setSha256(sha256);
        batchPolling.setLastReserved(now);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(pnAddressManagerConfig.getNormalizer().getBatchRequest().getTtl()).toEpochSecond(ZoneOffset.UTC));
        return batchPolling;
    }
}
