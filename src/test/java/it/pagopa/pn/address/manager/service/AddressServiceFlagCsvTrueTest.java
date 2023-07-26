package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ExtendWith (MockitoExtension.class)
@TestPropertySource (properties = {"${pn.address.manager.flag.csv}=true"})
class AddressServiceFlagCsvTrueTest {
	@Autowired
	private AddressService addressService;
	@Mock
	private PagoPaClient pagoPaClient;
	@Mock
	private EventService eventService;
	@Mock
	private CsvService csvService;
	@Mock
	private ISINIReceiverService isiniReceiverService;
	@Mock
	private AddressConverter addressConverter;
	@Mock
	private AddressUtils addressUtils;
	@Mock
	private Scheduler scheduler;
	@Test
	void testNormalizeAddressAsync () {
		NormalizeItemsRequest normalizedAddressRequest = new NormalizeItemsRequest();
		normalizedAddressRequest.setCorrelationId("correlationId");
		AcceptedResponse acceptedResponse = new AcceptedResponse();
		acceptedResponse.setCorrelationId("correlationId");
		NormalizeResult normalizeResult = mock(NormalizeResult.class);
		StepVerifier.create(addressService.normalizeAddressAsync(normalizedAddressRequest, "cxId"))
				.expectNext(acceptedResponse)
				.verifyComplete();
	}
}
