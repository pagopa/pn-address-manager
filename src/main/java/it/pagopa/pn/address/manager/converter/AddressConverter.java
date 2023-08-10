package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.model.WsNormAccInputModel;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaRequest;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaResponse;
import it.pagopa.pn.address.manager.model.deduplica.NormOutputPagoPa;
import it.pagopa.pn.address.manager.utils.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AddressConverter {

    public DeduplicaRequest createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest deduplicatesRequest){
        AnalogAddress master = deduplicatesRequest.getBaseAddress();
        AnalogAddress slave = deduplicatesRequest.getTargetAddress();
        DeduplicaRequest deduplicaRequest = new DeduplicaRequest();

        deduplicaRequest.setProvinciaSlave(slave.getPr());
        deduplicaRequest.setCapSlave(slave.getCap());
        deduplicaRequest.setLocalitaSlave(slave.getCity());
        deduplicaRequest.setLocalitaAggiuntivaSlave(slave.getCity2());
        deduplicaRequest.setIndirizzoSlave(slave.getAddressRow());
        //dug? civico?
        deduplicaRequest.setProvinciaMaster(master.getPr());
        deduplicaRequest.setCapMaster(master.getCap());
        deduplicaRequest.setLocalitaMaster(master.getCity());
        deduplicaRequest.setLocalitaAggiuntivaMaster(master.getCity2());
        deduplicaRequest.setIndirizzoMaster(master.getAddressRow());

        //configurazione e auth key??
        return deduplicaRequest;
    }

    public DeduplicatesResponse createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse deduplicaResponse){
        DeduplicatesResponse deduplicatesResponse = new DeduplicatesResponse();

        if(deduplicaResponse.isErrore()){
            deduplicatesResponse.setError(deduplicaResponse.getErrorMessage());
        }
        else{
            NormOutputPagoPa normOutputPagoPa = JsonUtils.fromJson(deduplicaResponse.getResult(), NormOutputPagoPa.class);

            AnalogAddress target = getAnalogAddress(normOutputPagoPa);

            deduplicatesResponse.setEqualityResult(normOutputPagoPa.getFlagNormalizzatoSlave() == 1);
            deduplicatesResponse.setNormalizedAddress(target);
        }

        return deduplicatesResponse;
    }

    @NotNull
    private static AnalogAddress getAnalogAddress (NormOutputPagoPa normOutputPagoPa) {
        AnalogAddress target = new AnalogAddress();
        target.setPr(normOutputPagoPa.getSSiglaProvincia());
        target.setCap(normOutputPagoPa.getSCAP());
        target.setCity(normOutputPagoPa.getSComuneUfficiale());
        target.setCity2(normOutputPagoPa.getSComuneAbbreviato());
        target.setCountry(normOutputPagoPa.getSStatoUfficiale());
        target.setAddressRow(normOutputPagoPa.getSViaCompletaUfficiale());
        target.setAddressRow2(normOutputPagoPa.getSViaCompletaAbbreviata());
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

    public List<WsNormAccInputModel> normalizeRequestToWsNormAccInputModel(List<NormalizeRequest> normalizeRequestList){
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
}
