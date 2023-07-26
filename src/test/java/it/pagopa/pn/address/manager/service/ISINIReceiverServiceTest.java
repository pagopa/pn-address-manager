package it.pagopa.pn.address.manager.service;

import it.pagopa.pn.address.manager.utils.SoapActivationMessageUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest
@ExtendWith (SpringExtension.class)
class ISINIReceiverServiceTest {
	@Autowired
	private ISINIReceiverService iSINIReceiverService;

	@MockBean
	private SoapActivationMessageUtils soapBuilder;

}

