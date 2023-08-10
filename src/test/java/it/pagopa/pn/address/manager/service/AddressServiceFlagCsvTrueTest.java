package it.pagopa.pn.address.manager.service;


import it.pagopa.pn.address.manager.client.PagoPaClient;
import it.pagopa.pn.address.manager.client.PnSafeStorageClient;
import it.pagopa.pn.address.manager.converter.AddressConverter;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AcceptedResponse;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeItemsRequest;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.NormalizeResult;
import it.pagopa.pn.address.manager.utils.AddressUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.scheduler.Scheduler;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith (SpringExtension.class)
@TestPropertySource (properties = {"${pn.address.manager.flag.csv}=true"})
class AddressServiceFlagCsvTrueTest {
	@Autowired
	private AddressService addressService;
	@MockBean
	private PagoPaClient pagoPaClient;
	@MockBean
	private EventService eventService;
	@MockBean
	private CsvService csvService;
	@MockBean
	private ISINIReceiverService isiniReceiverService;
	@MockBean
	private AddressConverter addressConverter;
	@MockBean
	private AddressUtils addressUtils;
	@MockBean
	@Qualifier ("addressManagerScheduler")
	private Scheduler scheduler;
	@MockBean
	private PnSafeStorageClient pnSafeStorageClient;
	@Test
	void testNormalizeAddressAsync () {
		//AddressService addressService = new AddressService(pagoPaClient, eventService, csvService, isiniReceiverService, addressConverter, addressUtils, scheduler, true, pnSafeStorageClient);
		NormalizeItemsRequest normalizedAddressRequest = new NormalizeItemsRequest();
		normalizedAddressRequest.setCorrelationId("correlationId");
		AcceptedResponse acceptedResponse = new AcceptedResponse();
		acceptedResponse.setCorrelationId("correlationId");
		NormalizeResult normalizeResult = mock(NormalizeResult.class);
		when(addressConverter.normalizeItemsRequestToAcceptedResponse(any())).thenReturn(acceptedResponse);
		StepVerifier.create(addressService.normalizeAddressAsync(normalizedAddressRequest, "cxId"))
				.expectNext(acceptedResponse)
				.verifyComplete();
	}
}
