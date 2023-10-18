package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.deduplica.v1.dto.*;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
public class AddressConverter {

    public DeduplicaRequest createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest deduplicatesRequest) {
        DeduplicaRequest inputDeduplica = new DeduplicaRequest();

        ConfigIn configIn = new ConfigIn();
        inputDeduplica.setConfigIn(configIn);

        AddressIn slaveIn = getAddressIn(deduplicatesRequest.getTargetAddress(), deduplicatesRequest.getCorrelationId());
        inputDeduplica.setSlaveIn(slaveIn);

        AddressIn masterIn = getAddressIn(deduplicatesRequest.getBaseAddress(), deduplicatesRequest.getCorrelationId());
        inputDeduplica.setMasterIn(masterIn);

        return inputDeduplica;
    }

    @NotNull
    private static AddressIn getAddressIn(AnalogAddress analogAddress, String correlationId) {
        AddressIn addressIn = new AddressIn();
        addressIn.setId(correlationId);
        addressIn.setProvincia(analogAddress.getPr());
        addressIn.setLocalita(analogAddress.getCity());
        addressIn.setIndirizzo(analogAddress.getAddressRow());
        addressIn.setIndirizzoAggiuntivo(analogAddress.getAddressRow2());
        addressIn.setCap(analogAddress.getCap());
        addressIn.setLocalitaAggiuntiva(analogAddress.getCity2());
        addressIn.setStato(analogAddress.getCountry());
        return addressIn;
    }

    public DeduplicatesResponse createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse risultatoDeduplica, String correlationId) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(correlationId);

        if (risultatoDeduplica.getErrore() != null) {
            deduplicatesResponse.setError(decodeErrorDedu(risultatoDeduplica.getErrore()));
        } else {
            getAnalogAddress(risultatoDeduplica, deduplicatesResponse);
        }

        return deduplicatesResponse;
    }

    private static String decodeErroreNorm(Integer error) {
        return String.valueOf(error);
        //return PostelErrorNormEnum.getValueFromName(error);
    }

    private static String decodeErrorDedu(String erroreDedu) {
        return erroreDedu;
        //return PostelErrorEnum.valueOf(erroreDedu).getValue();
    }

    private static void getAnalogAddress(DeduplicaResponse risultatoDeduplica, DeduplicatesResponse deduplicatesResponse) {

        if (risultatoDeduplica.getSlaveOut() != null) {
            if (StringUtils.hasText(risultatoDeduplica.getSlaveOut().getfPostalizzabile())
                    && risultatoDeduplica.getSlaveOut().getfPostalizzabile().equalsIgnoreCase("0")) {
                deduplicatesResponse.setError(decodeErroreNorm(risultatoDeduplica.getSlaveOut().getnErroreNorm()));
                return;
            }
            AnalogAddress analogAddress = getAddress(risultatoDeduplica.getSlaveOut());
            deduplicatesResponse.setNormalizedAddress(analogAddress);
            deduplicatesResponse.setEqualityResult(risultatoDeduplica.getRisultatoDedu());
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
        batchPolling.setBatchId(batchId);
        batchPolling.setFileKey(fileKey);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setSha256(sha256);
        batchPolling.setLastReserved(now);
        batchPolling.setCreatedAt(now);
        return batchPolling;
    }
}
