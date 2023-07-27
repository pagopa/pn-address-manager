package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaRequest;
import it.pagopa.pn.address.manager.model.deduplica.DeduplicaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration (classes = {AddressConverter.class})
@ExtendWith (SpringExtension.class)
class AddressConverterTest {
	@Autowired
	private AddressConverter addressConverter;

	/**
	 * Method under test: {@link AddressConverter#createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest)}
	 */
	@Test
	void testCreateDeduplicaRequestFromDeduplicatesRequest3 () {
		// Arrange
		AnalogAddress targetAddress = new AnalogAddress();
		targetAddress.addressRow("42 Main St");

		DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
		deduplicatesRequest.baseAddress(new AnalogAddress());
		deduplicatesRequest.targetAddress(targetAddress);

		// Act
		DeduplicaRequest actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
				.createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);

		// Assert
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getProvinciaSlave());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getProvinciaMaster());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getLocalitaSlave());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getLocalitaMaster());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getLocalitaAggiuntivaSlave());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getLocalitaAggiuntivaMaster());
		assertEquals("42 Main St", actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getIndirizzoSlave());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getIndirizzoMaster());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getCapSlave());
		assertNull(actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getCapMaster());
	}
	/**
	 * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(DeduplicaResponse)}
	 */
	@Test
	void testCreateDeduplicatesResponseFromDeduplicaResponse () {
		// Arrange
		DeduplicaResponse deduplicaResponse = new DeduplicaResponse();
		deduplicaResponse.setErrorCode(-1);
		deduplicaResponse.setErrorMessage("An error occurred");
		deduplicaResponse.setErrore(true);
		deduplicaResponse.setNextValue(42L);
		deduplicaResponse.setNumeroRecords(10);
		deduplicaResponse.setResult("Result");
		deduplicaResponse.setRowFetched(2);

		// Act and Assert
		assertEquals("An error occurred",
				addressConverter.createDeduplicatesResponseFromDeduplicaResponse(deduplicaResponse).getError());
	}
	@Test
	void testCreateDeduplicatesResponseFromDeduplicaResponse2 () {
		// Arrange
		DeduplicaResponse deduplicaResponse = new DeduplicaResponse();
		deduplicaResponse.setErrorCode(-1);
		deduplicaResponse.setErrorMessage("An error occurred");
		deduplicaResponse.setErrore(false);
		deduplicaResponse.setNextValue(42L);
		deduplicaResponse.setNumeroRecords(10);
		deduplicaResponse.setResult("{}");
		deduplicaResponse.setRowFetched(2);

		// Act and Assert
		assertNull(addressConverter.createDeduplicatesResponseFromDeduplicaResponse(deduplicaResponse).getCorrelationId());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizeItemsRequestToAcceptedResponse(NormalizeItemsRequest)}
	 */
	@Test
	void testNormalizeItemsRequestToAcceptedResponse () {
		// Arrange, Act and Assert
		assertNull(
				addressConverter.normalizeItemsRequestToAcceptedResponse(new NormalizeItemsRequest()).getCorrelationId());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizedAddressResponsetoNormalizeResult(NormalizedAddressResponse)}
	 */
	@Test
	void testNormalizedAddressResponsetoNormalizeResult () {
		// Arrange and Act
		NormalizeResult actualNormalizedAddressResponsetoNormalizeResultResult = addressConverter
				.normalizedAddressResponsetoNormalizeResult(new NormalizedAddressResponse());

		// Assert
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getError());
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getNormalizedAddress());
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getId());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizeRequestToWsNormAccInputModel(List)}
	 */
	@Test
	void testNormalizeRequestToWsNormAccInputModel () {
		// Arrange, Act and Assert
		assertTrue(addressConverter.normalizeRequestToWsNormAccInputModel(new ArrayList<>()).isEmpty());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizeRequestToWsNormAccInputModel(List)}
	 */
	@Test
	void testNormalizeRequestToWsNormAccInputModel3 () {
		// Arrange
		NormalizeRequest normalizeRequest = new NormalizeRequest();
		normalizeRequest.address(new AnalogAddress());

		ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
		normalizeRequestList.add(normalizeRequest);

		// Act and Assert
		assertEquals(1, addressConverter.normalizeRequestToWsNormAccInputModel(normalizeRequestList).size());
	}
}

