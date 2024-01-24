package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.generated.openapi.msclient.postel.deduplica.v1.dto.*;
import it.pagopa.pn.address.manager.constant.*;
import it.pagopa.pn.address.manager.entity.NormalizzatoreBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.DeduplicatesResponse;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL;

@Component
@CustomLog
@RequiredArgsConstructor(access = AccessLevel.NONE)
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
    private AddressIn getAddressIn(AnalogAddress analogAddress, String correlationId) {
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
            log.warn("Error during deduplicate and normalize addreses: correlationId: [{}] - error: {}", deduplicatesResponse.getCorrelationId(), PostelDeduError.valueOf(risultatoDeduplica.getErrore()).getDescrizione());

            if (!risultatoDeduplica.getErrore().startsWith("DED00")) {

                deduplicatesResponse.setError(DeduplicatesError.PNADDR999.name());
                return deduplicatesResponse;

            } else {
                PostelDeduError error = PostelDeduError.valueOf(risultatoDeduplica.getErrore());
                switch (error) {
                    case DED001 -> deduplicatesResponse.setResultDetails(DeduplicatesResultDetails.RD01.name());
                    case DED002 -> deduplicatesResponse.setResultDetails(DeduplicatesResultDetails.RD02.name());
                    case DED003 -> deduplicatesResponse.setResultDetails(DeduplicatesResultDetails.RD03.name());
                    default -> deduplicatesResponse.setResultDetails(null);
                }
            }
        }

        getAnalogAddress(risultatoDeduplica, deduplicatesResponse, correlationId);
        return deduplicatesResponse;
    }

    private void getAnalogAddress(DeduplicaResponse risultatoDeduplica, DeduplicatesResponse deduplicatesResponse, String correlationId) {
        if (risultatoDeduplica.getSlaveOut() != null) {
            if (StringUtils.hasText(risultatoDeduplica.getSlaveOut().getfPostalizzabile())
                    && risultatoDeduplica.getSlaveOut().getfPostalizzabile().equalsIgnoreCase("0")) {
                log.warn("Error during deduplicate and normalize addreses: correlationId: [{}] - error: {}",
                        correlationId,
                        PostelNErrorNorm.fromCode(risultatoDeduplica.getSlaveOut().getnErroreNorm()).getDescription());
                deduplicatesResponse.setError(DeduplicatesError.PNADDR001.name());
                return;
            }
            AnalogAddress analogAddress = getAddress(risultatoDeduplica.getSlaveOut());
            deduplicatesResponse.setNormalizedAddress(analogAddress);
            deduplicatesResponse.setEqualityResult(risultatoDeduplica.getRisultatoDedu());
        } else {
            log.fatal(ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL + ": " + ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL);
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL, ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL);
        }
    }

    @NotNull
    private AnalogAddress getAddress(AddressOut slaveOut) {
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

    public NormalizzatoreBatch createPostelBatchByBatchIdAndFileKey(String batchId, String fileKey, String sha256) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        NormalizzatoreBatch batchPolling = new NormalizzatoreBatch();
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
