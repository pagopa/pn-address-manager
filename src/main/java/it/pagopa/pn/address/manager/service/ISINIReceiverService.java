package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.utils.SoapBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import javax.xml.soap.*;
import javax.xml.transform.*;

@Service
public class ISINIReceiverService {

    private final SoapBuilder soapBuilder;

    public ISINIReceiverService(SoapBuilder soapBuilder) {
        this.soapBuilder = soapBuilder;
    }

    public Mono<String> activateSINIComponent() throws JAXBException, SOAPException, TransformerException {
        return Mono.just(soapBuilder.soapActivationMessageBuilder());
    }
}
