package it.pagopa.pn.address.manager.converter;

import _it.pagopa.pn.address.manager.microservice.msclient.generated.generated.postel.v1.dto.*;
import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.constant.BatchStatus;
import it.pagopa.pn.address.manager.entity.PostelBatch;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.jetbrains.annotations.NotNull;
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

    public InputDeduplica createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest deduplicatesRequest) {
        InputDeduplica inputDeduplica = new InputDeduplica();

        ConfigIn configIn = new ConfigIn();
        configIn.setAuthKey(pnAddressManagerConfig.getPostel().getAuthKey());
        configIn.setConfigurazioneDeduplica(""); // configurazione deduplica?
        configIn.setConfigurazioneNorm(""); // configurazione normalizzazione?
        inputDeduplica.setConfigIn(configIn);

        SlaveIn slaveIn = getSlaveIn(deduplicatesRequest);
        inputDeduplica.setSlaveIn(slaveIn);

        MasterIn masterIn = getMasterIn(deduplicatesRequest);
        inputDeduplica.setMasterIn(masterIn);

        return inputDeduplica;
    }

    @NotNull
    private static MasterIn getMasterIn(DeduplicatesRequest deduplicatesRequest) {
        MasterIn masterIn = new MasterIn();
        masterIn.setId(deduplicatesRequest.getCorrelationId()); // id??
        masterIn.setProvincia(deduplicatesRequest.getBaseAddress().getPr());
        masterIn.setLocalita(deduplicatesRequest.getBaseAddress().getCity());
        masterIn.setIndirizzo(deduplicatesRequest.getBaseAddress().getAddressRow() + " " + deduplicatesRequest.getBaseAddress().getAddressRow2());
        masterIn.setCap(deduplicatesRequest.getBaseAddress().getCap());
        masterIn.setLocalitaAggiuntiva(deduplicatesRequest.getBaseAddress().getCity2());
        masterIn.setStato(deduplicatesRequest.getBaseAddress().getCountry());
        return masterIn;
    }

    @NotNull
    private static SlaveIn getSlaveIn(DeduplicatesRequest deduplicatesRequest) {
        SlaveIn slaveIn = new SlaveIn();
        slaveIn.setId(deduplicatesRequest.getCorrelationId()); // id??
        slaveIn.setProvincia(deduplicatesRequest.getTargetAddress().getPr());
        slaveIn.setLocalita(deduplicatesRequest.getTargetAddress().getCity());
        slaveIn.setIndirizzo(deduplicatesRequest.getTargetAddress().getAddressRow() + " " + deduplicatesRequest.getTargetAddress().getAddressRow2());
        slaveIn.setCap(deduplicatesRequest.getTargetAddress().getCap());
        slaveIn.setLocalitaAggiuntiva(deduplicatesRequest.getTargetAddress().getCity2());
        slaveIn.setStato(deduplicatesRequest.getTargetAddress().getCountry());
        return slaveIn;
    }

    public DeduplicatesResponse createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica risultatoDeduplica, String correlationId) {
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();
        deduplicatesResponse.setCorrelationId(correlationId);

        //TODO: VERIFICARE CHE LA RISPOSTA SIA 0 O 1
        deduplicatesResponse.setEqualityResult(risultatoDeduplica.getRisultatoDedu() != null
                && !risultatoDeduplica.getRisultatoDedu().equalsIgnoreCase("0"));

        deduplicatesResponse.setError(decodeErrorDedu(risultatoDeduplica.getErroreDedu()));

        getAnalogAddress(risultatoDeduplica, deduplicatesResponse);

        return deduplicatesResponse;
    }

    private String decodeErrorDedu(Integer erroreDedu) {
        if(erroreDedu != null) {
            //TODO: CHIEDERE INFORMAZIONI SULLA DECODIFICA DEL CODICE ERRORE DEDU
            return "TODO";
        }else{
            return null;
        }
    }

    @NotNull
    private static DeduplicatesResponse getAnalogAddress(RisultatoDeduplica risultatoDeduplica, DeduplicatesResponse deduplicatesResponse) {

        if (risultatoDeduplica.getSlaveOut() != null) {
            if(StringUtils.hasText(risultatoDeduplica.getSlaveOut().getfPostalizzabile())
            && risultatoDeduplica.getSlaveOut().getfPostalizzabile().equalsIgnoreCase("0")){
                //TODO: GESTIONE ERRORE NORMALIZZAZIONE
            }
            AnalogAddress analogAddress = getAddress(risultatoDeduplica.getSlaveOut());
            deduplicatesResponse.setNormalizedAddress(analogAddress);
            return deduplicatesResponse;
        } else {
            throw new PnInternalException(ERROR_MESSAGE_ADDRESS_MANAGER_DEDUPLICA_POSTEL, ERROR_CODE_ADDRESS_MANAGER_DEDUPLICA_POSTEL);
        }
    }

    @NotNull
    private static AnalogAddress getAddress(SlaveOut slaveOut) {
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

    public PostelBatch createPostelBatchByBatchIdAndFileKey(String batchId, String fileKey) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PostelBatch batchPolling = new PostelBatch();
        batchPolling.setBatchId(batchId);
        batchPolling.setFileKey(fileKey);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(pnAddressManagerConfig.getPostel().getBatchTtl()).toEpochSecond(ZoneOffset.UTC));
        return batchPolling;
    }
}
