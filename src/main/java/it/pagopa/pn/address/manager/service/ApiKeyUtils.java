package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.config.PnAddressManagerConfig;
import it.pagopa.pn.address.manager.entity.ApiKeyModel;
import it.pagopa.pn.address.manager.exception.PnInternalAddressManagerException;
import it.pagopa.pn.address.manager.repository.ApiKeyRepository;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_CHECKING_APIKEY;
import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.*;

@Component
@RequiredArgsConstructor
@CustomLog
public class ApiKeyUtils {

    private final ApiKeyRepository apiKeyRepository;
    private final PnAddressManagerConfig pnAddressManagerConfig;

    public Mono<ApiKeyModel> checkPostelApiKey(String cxId, String xApiKey) {
        log.logChecking(PROCESS_CHECKING_APIKEY + ": starting check ApiKey");
        return apiKeyRepository.findById(cxId)
                .filter(apiKeyModel -> apiKeyModel.getApiKey().equalsIgnoreCase(xApiKey))
                .switchIfEmpty(Mono.error(new PnInternalAddressManagerException(APIKEY_DOES_NOT_EXISTS, APIKEY_DOES_NOT_EXISTS, HttpStatus.FORBIDDEN.value(), "Api Key not found")));

    }

    public Mono<ApiKeyModel> checkApiKey(String cxId, String xApiKey) {
        if(Boolean.FALSE.equals(pnAddressManagerConfig.getFlagCsv())){
            log.logChecking(PROCESS_CHECKING_APIKEY + ": starting check ApiKey");
            return apiKeyRepository.findById(cxId)
                    .switchIfEmpty(Mono.error(new PnInternalAddressManagerException(ERROR_CLIENT_ID_MESSAGE, ERROR_CLIENT_ID_MESSAGE, HttpStatus.FORBIDDEN.value(), ERROR_CLIENT_ID)));
        }
        return Mono.just(buildApiKeyMock());
    }

    public ApiKeyModel buildApiKeyMock() {
        ApiKeyModel apiKeyModel = new ApiKeyModel();
        apiKeyModel.setApiKey("mockedApiKey");
        apiKeyModel.setCxId("mockedCxId");
        return apiKeyModel;
    }

}
