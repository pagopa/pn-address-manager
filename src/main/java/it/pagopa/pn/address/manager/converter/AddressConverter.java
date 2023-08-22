package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.model.WsNormAccInputModel;
import it.pagopa.pn.address.manager.model.deduplica.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressConverter {

    private final long postelTtl;

    private final String authKey;

    public AddressConverter(@Value("${pn.address.manager.postel.ttl}") long postelTtl,
                            @Value("${pn.address.manager.postel.authKey}") String authKey) {
        this.postelTtl = postelTtl;
        this.authKey = authKey;
    }
    public InputDeduplica createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest deduplicatesRequest){
        InputDeduplica inputDeduplica = new InputDeduplica();
        inputDeduplica.setConfigIn(new ConfigIn());
        inputDeduplica.setMasterIn(new MasterIn());
        inputDeduplica.setSlaveIn(new SlaveIn());

        inputDeduplica.getConfigIn().setAuthKey(authKey);
        inputDeduplica.getConfigIn().setConfigurazioneDeduplica(""); // configurazione deduplica?
        inputDeduplica.getConfigIn().setConfigurazioneNorm(""); // configurazione normalizzazione?

        inputDeduplica.getSlaveIn().setId("001SLAVE"); // id??
        inputDeduplica.getSlaveIn().setProvincia(deduplicatesRequest.getTargetAddress().getPr());
        inputDeduplica.getSlaveIn().setLocalita(deduplicatesRequest.getTargetAddress().getCity());
        inputDeduplica.getSlaveIn().setIndirizzo(deduplicatesRequest.getTargetAddress().getAddressRow() + " " + deduplicatesRequest.getTargetAddress().getAddressRow2());
        inputDeduplica.getSlaveIn().setCap(deduplicatesRequest.getTargetAddress().getCap());
        inputDeduplica.getSlaveIn().setLocalitaAggiuntiva(deduplicatesRequest.getTargetAddress().getCity2());
        inputDeduplica.getSlaveIn().setStato(deduplicatesRequest.getTargetAddress().getCountry());

        inputDeduplica.getMasterIn().setId("001MASTER"); // id??
        inputDeduplica.getMasterIn().setProvincia(deduplicatesRequest.getBaseAddress().getPr());
        inputDeduplica.getMasterIn().setLocalita(deduplicatesRequest.getBaseAddress().getCity());
        inputDeduplica.getMasterIn().setIndirizzo(deduplicatesRequest.getBaseAddress().getAddressRow() + " " + deduplicatesRequest.getBaseAddress().getAddressRow2());
        inputDeduplica.getMasterIn().setCap(deduplicatesRequest.getBaseAddress().getCap());
        inputDeduplica.getMasterIn().setLocalitaAggiuntiva(deduplicatesRequest.getBaseAddress().getCity2());
        inputDeduplica.getMasterIn().setStato(deduplicatesRequest.getBaseAddress().getCountry());

        return inputDeduplica;
    }

    public DeduplicatesResponse createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica risultatoDeduplica){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();

        deduplicatesResponse.setEqualityResult(true);   // ci arriva una stringa che descrive il risultato del confronto,
                                                        // come dobbiamo valutarla?
        deduplicatesResponse.setError(risultatoDeduplica.getRisultatoDedu());
        deduplicatesResponse.setCorrelationId("correlationId"); // come lo valorizziamo?

        AnalogAddress target = getAnalogAddress(risultatoDeduplica);

        deduplicatesResponse.setNormalizedAddress(target);

        return deduplicatesResponse;
    }

    @NotNull
    private static AnalogAddress getAnalogAddress (RisultatoDeduplica risultatoDeduplica) {
        AnalogAddress target = new AnalogAddress();

        target.setAddressRow(risultatoDeduplica.getMasterOut().getsViaCompletaSpedizione());
        target.setAddressRow2(""); // addresRow2?
        target.setCity(risultatoDeduplica.getMasterOut().getsComuneSpedizione());
        target.setCity2(""); // city2?
        target.setCap(risultatoDeduplica.getMasterOut().getsCap());
        target.setPr(risultatoDeduplica.getMasterOut().getsSiglaProv());
        target.setCountry(risultatoDeduplica.getMasterOut().getsStatoSpedizione());
        return target;
    }
    public AcceptedResponse normalizeItemsRequestToAcceptedResponse(NormalizeItemsRequest normalizeItemsRequest) {
        AcceptedResponse acceptedResponse = new AcceptedResponse();
        acceptedResponse.setCorrelationId(normalizeItemsRequest.getCorrelationId());
        return acceptedResponse;
    }


    public NormalizeResult normalizedAddressResponsetoNormalizeResult(NormalizedAddressResponse response) {
        NormalizeResult normalizeResult = new NormalizeResult();
        normalizeResult.setId(response.getId());
        normalizeResult.setError(response.getError());
        normalizeResult.setNormalizedAddress(response.getNormalizedAddress());
        return normalizeResult;
    }

    public WsNormAccInputModel normalizeRequestToWsNormAccInputModel(NormalizeRequest normalizeRequest){
        AnalogAddress address = normalizeRequest.getAddress();
        WsNormAccInputModel wsNormAccInputModel = new WsNormAccInputModel();
        wsNormAccInputModel.setIdCodiceCliente(normalizeRequest.getId());
        wsNormAccInputModel.setProvincia(address.getPr());
        wsNormAccInputModel.setCap(address.getCap());
        wsNormAccInputModel.setLocalita(address.getCity());
        wsNormAccInputModel.setLocalitaAggiuntiva(address.getCity2());
        wsNormAccInputModel.setDug(address.getCountry());
        wsNormAccInputModel.setIndirizzo(address.getAddressRow());
        wsNormAccInputModel.setCivico(address.getAddressRow()); //???
        return wsNormAccInputModel;
    }

    public List<WsNormAccInputModel> normalizeRequestListToWsNormAccInputModel(List<NormalizeRequest> normalizeRequestList){
        return normalizeRequestList.stream()
                .map(normalizeRequest -> {
                    AnalogAddress address = normalizeRequest.getAddress();
                    WsNormAccInputModel wsNormAccInputModel = new WsNormAccInputModel();
                    wsNormAccInputModel.setIdCodiceCliente(normalizeRequest.getId());
                    wsNormAccInputModel.setProvincia(address.getPr());
                    wsNormAccInputModel.setCap(address.getCap());
                    wsNormAccInputModel.setLocalita(address.getCity());
                    wsNormAccInputModel.setLocalitaAggiuntiva(address.getCity2());
                    wsNormAccInputModel.setDug(address.getCountry());
                    wsNormAccInputModel.setIndirizzo(address.getAddressRow());
                    wsNormAccInputModel.setCivico(address.getAddressRow()); //???
                    return wsNormAccInputModel;
                }).toList();
    }
    /*public PostelBatch createPostelBatchByBatchIdAndFileKey(String batchId, String fileKey) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PostelBatch batchPolling = new PostelBatch();
        batchPolling.setBatchId(batchId);
        batchPolling.setFileKey(fileKey);
        batchPolling.setStatus(BatchStatus.NOT_WORKED.getValue());
        batchPolling.setRetry(0);
        batchPolling.setCreatedAt(now);
        batchPolling.setTtl(now.plusSeconds(postelTtl).toEpochSecond(ZoneOffset.UTC));
        return batchPolling;
    }*/
}
