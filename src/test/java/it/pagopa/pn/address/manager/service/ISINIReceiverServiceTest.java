package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.utils.SoapActivationMessageUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.xml.bind.JAXBException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration (classes = {ISINIReceiverService.class})
@ExtendWith (SpringExtension.class)
class ISINIReceiverServiceTest {
	@Autowired
	private ISINIReceiverService iSINIReceiverService;

	@MockBean
	private SoapActivationMessageUtils soapBuilder;

}

