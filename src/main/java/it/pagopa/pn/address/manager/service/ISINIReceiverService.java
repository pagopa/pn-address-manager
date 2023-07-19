package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.converter.ActivateSINIConverter;
import it.pagopa.pn.address.manager.exception.RuntimeJAXBException;
import it.pagopa.pn.address.manager.utils.SoapActivationMessageUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.*;
import javax.xml.transform.*;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pn.address.manager.constant.ProcessStatus.PROCESS_CHECKING_POSTEL_ATTIVAZIONE_SINI;

@CustomLog
@Service
public class ISINIReceiverService {

    private final SoapActivationMessageUtils soapBuilder;
    private final PagoPaClient pagoPaClient;

    private final ActivateSINIConverter activateSINIConverter;

    public ISINIReceiverService(SoapActivationMessageUtils soapBuilder, PagoPaClient pagoPaClient, ActivateSINIConverter activateSINIConverter) {
        this.soapBuilder = soapBuilder;
        this.pagoPaClient = pagoPaClient;
        this.activateSINIConverter = activateSINIConverter;
    }

    public Mono<Object> activateSINIComponent() {
        String soapMessage = soapBuilder.soapActivationMessageBuilder();
        return pagoPaClient.activateSINIComponent(soapMessage)
                .map(response -> {
                    try {
                        Object resp = unmarshaller(response);
                        log.logCheckingOutcome(PROCESS_CHECKING_POSTEL_ATTIVAZIONE_SINI, true);
                        return activateSINIConverter.mapResponse(resp);
                    } catch (JAXBException e) {
                        log.logCheckingOutcome(PROCESS_CHECKING_POSTEL_ATTIVAZIONE_SINI, false, e.getMessage());
                        throw new RuntimeJAXBException(e.getMessage());
                    }
                });
    }

    public Object unmarshaller(String response) throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(Object.class);
        String responseBody = extractResponseBody(response);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return jaxbUnmarshaller.unmarshal(new StringReader(responseBody));
    }

    private String extractResponseBody(String xmlResponse) {
        Pattern responsePattern = Pattern.compile(".*Body\\>(.*)<\\/</soap:Body>");
        String checkValidityElement = "";
        Matcher matcher = responsePattern.matcher(xmlResponse);
        while (matcher.find()) {
            checkValidityElement = matcher.group(1);
        }
        return checkValidityElement;
    }
}
