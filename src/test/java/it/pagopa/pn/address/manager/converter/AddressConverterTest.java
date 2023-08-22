package it.pagopa.pn.address.manager.converter;

import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.address.manager.model.NormalizedAddressResponse;
import it.pagopa.pn.address.manager.model.WsNormAccInputModel;
import it.pagopa.pn.address.manager.model.deduplica.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration (classes = {AddressConverter.class})
@ExtendWith (SpringExtension.class)
@TestPropertySource (properties = {"pn.address.manager.postel.authKey=test", "pn.address.manager.postel.ttl=12345"})
class AddressConverterTest {
	@Autowired
	private AddressConverter addressConverter;
	/**
	 * Method under test: {@link AddressConverter#createDeduplicaRequestFromDeduplicatesRequest(DeduplicatesRequest)}
	 */
	@Test
	void testCreateDeduplicaRequestFromDeduplicatesRequest3 () {
		//   Diffblue Cover was unable to write a Spring test,
		//   so wrote a non-Spring test instead.
		//   Reason: R027 Missing beans when creating Spring context.
		//   Failed to create Spring context due to missing beans
		//   in the current Spring profile:
		//       it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi
		//   See https://diff.blue/R027 to resolve this issue.

		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		AnalogAddress targetAddress = new AnalogAddress();
		targetAddress.cap("001SLAVE");

		DeduplicatesRequest deduplicatesRequest = new DeduplicatesRequest();
		deduplicatesRequest.baseAddress(new AnalogAddress());
		deduplicatesRequest.targetAddress(targetAddress);

		// Act
		InputDeduplica actualCreateDeduplicaRequestFromDeduplicatesRequestResult = addressConverter
				.createDeduplicaRequestFromDeduplicatesRequest(deduplicatesRequest);

		// Assert
		SlaveIn slaveIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getSlaveIn();
		assertNull(slaveIn.getStato());
		MasterIn masterIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getMasterIn();
		assertNull(masterIn.getStato());
		assertNull(masterIn.getProvincia());
		assertNull(masterIn.getLocalitaAggiuntiva());
		assertNull(masterIn.getLocalita());
		assertEquals("null null", masterIn.getIndirizzo());
		assertEquals("001MASTER", masterIn.getId());
		assertNull(masterIn.getCap());
		assertNull(slaveIn.getProvincia());
		assertNull(slaveIn.getLocalitaAggiuntiva());
		assertNull(slaveIn.getLocalita());
		assertEquals("null null", slaveIn.getIndirizzo());
		assertEquals("001SLAVE", slaveIn.getId());
		assertEquals("001SLAVE", slaveIn.getCap());
		ConfigIn configIn = actualCreateDeduplicaRequestFromDeduplicatesRequestResult.getConfigIn();
		assertEquals("", configIn.getConfigurazioneDeduplica());
		assertEquals("Auth Key", configIn.getAuthKey());
		assertEquals("", configIn.getConfigurazioneNorm());
	}

	/**
	 * Method under test: {@link AddressConverter#createDeduplicatesResponseFromDeduplicaResponse(RisultatoDeduplica)}
	 */
	@Test
	void testCreateDeduplicatesResponseFromDeduplicaResponse () {
		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		MasterOut masterOut = new MasterOut();
		masterOut.setId("42");
		masterOut.setfPostalizzabile("F Postalizzabile");
		masterOut.setnErroreNorm(-1);
		masterOut.setsCap("S Cap");
		masterOut.setsCivicoAltro("S Civico Altro");
		masterOut.setsComuneAbb("S Comune Abb");
		masterOut.setsComuneSpedizione("S Comune Spedizione");
		masterOut.setsComuneUff("S Comune Uff");
		masterOut.setsFrazioneAbb("S Frazione Abb");
		masterOut.setsFrazioneSpedizione("S Frazione Spedizione");
		masterOut.setsFrazioneUff("S Frazione Uff");
		masterOut.setsPresso("S Presso");
		masterOut.setsSiglaProv("S Sigla Prov");
		masterOut.setsViaCompletaAbb("S Via Completa Abb");
		masterOut.setsViaCompletaSpedizione("S Via Completa Spedizione");
		masterOut.setsViaCompletaUff("S Via Completa Uff");

		SlaveOut slaveOut = new SlaveOut();
		slaveOut.setId("42");
		slaveOut.setfPostalizzabile("F Postalizzabile");
		slaveOut.setnErroreNorm(-1);
		slaveOut.setsCap("S Cap");
		slaveOut.setsCivicoAltro("S Civico Altro");
		slaveOut.setsComuneAbb("S Comune Abb");
		slaveOut.setsComuneSpedizione("S Comune Spedizione");
		slaveOut.setsComuneUff("S Comune Uff");
		slaveOut.setsFrazioneAbb("S Frazione Abb");
		slaveOut.setsFrazioneSpedizione("S Frazione Spedizione");
		slaveOut.setsFrazioneUff("S Frazione Uff");
		slaveOut.setsPresso("S Presso");
		slaveOut.setsSiglaProv("S Sigla Prov");
		slaveOut.setsViaCompletaAbb("S Via Completa Abb");
		slaveOut.setsViaCompletaSpedizione("S Via Completa Spedizione");
		slaveOut.setsViaCompletaUff("S Via Completa Uff");

		RisultatoDeduplica risultatoDeduplica = new RisultatoDeduplica();
		risultatoDeduplica.setErroreDedu(-1);
		risultatoDeduplica.setErroreGenerico(-1);
		risultatoDeduplica.setMasterOut(masterOut);
		risultatoDeduplica.setSlaveOut(slaveOut);

		// Act
		DeduplicatesResponse actualCreateDeduplicatesResponseFromDeduplicaResponseResult = addressConverter
				.createDeduplicatesResponseFromDeduplicaResponse(risultatoDeduplica);

		// Assert
		assertEquals("correlationId", actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getCorrelationId());
		assertNull(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getError());
		assertTrue(actualCreateDeduplicatesResponseFromDeduplicaResponseResult.getEqualityResult());
		AnalogAddress normalizedAddress = actualCreateDeduplicatesResponseFromDeduplicaResponseResult
				.getNormalizedAddress();
		assertNull(normalizedAddress.getCountry());
		assertEquals("", normalizedAddress.getCity2());
		assertEquals("S Comune Spedizione", normalizedAddress.getCity());
		assertEquals("S Cap", normalizedAddress.getCap());
		assertEquals("", normalizedAddress.getAddressRow2());
		assertEquals("S Via Completa Spedizione", normalizedAddress.getAddressRow());
		assertEquals("S Sigla Prov", normalizedAddress.getPr());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizeItemsRequestToAcceptedResponse(NormalizeItemsRequest)}
	 */
	@Test
	void testNormalizeItemsRequestToAcceptedResponse () {
		//   Diffblue Cover was unable to write a Spring test,
		//   so wrote a non-Spring test instead.
		//   Reason: R027 Missing beans when creating Spring context.
		//   Failed to create Spring context due to missing beans
		//   in the current Spring profile:
		//       it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi
		//   See https://diff.blue/R027 to resolve this issue.

		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		// Act and Assert
		assertNull(
				addressConverter.normalizeItemsRequestToAcceptedResponse(new NormalizeItemsRequest()).getCorrelationId());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizedAddressResponsetoNormalizeResult(NormalizedAddressResponse)}
	 */
	@Test
	void testNormalizedAddressResponsetoNormalizeResult () {
		//   Diffblue Cover was unable to write a Spring test,
		//   so wrote a non-Spring test instead.
		//   Reason: R027 Missing beans when creating Spring context.
		//   Failed to create Spring context due to missing beans
		//   in the current Spring profile:
		//       it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi
		//   See https://diff.blue/R027 to resolve this issue.

		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		// Act
		NormalizeResult actualNormalizedAddressResponsetoNormalizeResultResult = addressConverter
				.normalizedAddressResponsetoNormalizeResult(new NormalizedAddressResponse());

		// Assert
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getError());
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getNormalizedAddress());
		assertNull(actualNormalizedAddressResponsetoNormalizeResultResult.getId());
	}

	/**
	 * Method under test: {@link AddressConverter#normalizeRequestToWsNormAccInputModel(NormalizeRequest)}
	 */
	@Test
	void testNormalizeRequestToWsNormAccInputModel2 () {
		//   Diffblue Cover was unable to write a Spring test,
		//   so wrote a non-Spring test instead.
		//   Reason: R027 Missing beans when creating Spring context.
		//   Failed to create Spring context due to missing beans
		//   in the current Spring profile:
		//       it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi
		//   See https://diff.blue/R027 to resolve this issue.

		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		NormalizeRequest normalizeRequest = new NormalizeRequest();
		normalizeRequest.address(new AnalogAddress());

		// Act
		WsNormAccInputModel actualNormalizeRequestToWsNormAccInputModelResult = addressConverter
				.normalizeRequestToWsNormAccInputModel(normalizeRequest);

		// Assert
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getCap());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getProvincia());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getLocalitaAggiuntiva());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getLocalita());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getIndirizzo());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getIdCodiceCliente());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getDug());
		assertNull(actualNormalizeRequestToWsNormAccInputModelResult.getCivico());
	}
	/**
	 * Method under test: {@link AddressConverter#normalizeRequestListToWsNormAccInputModel(List)}
	 */
	@Test
	void testNormalizeRequestListToWsNormAccInputModel () {
		//   Diffblue Cover was unable to write a Spring test,
		//   so wrote a non-Spring test instead.
		//   Reason: R027 Missing beans when creating Spring context.
		//   Failed to create Spring context due to missing beans
		//   in the current Spring profile:
		//       it.pagopa.pn.address.manager.msclient.generated.pn.safe.storage.v1.api.FileUploadApi
		//   See https://diff.blue/R027 to resolve this issue.

		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		// Act and Assert
		assertTrue(addressConverter.normalizeRequestListToWsNormAccInputModel(new ArrayList<>()).isEmpty());
	}
	@Test
	void testNormalizeRequestListToWsNormAccInputModel4 () {
		// Arrange
		AddressConverter addressConverter = new AddressConverter(1L, "Auth Key");

		NormalizeRequest normalizeRequest = new NormalizeRequest();
		normalizeRequest.address(new AnalogAddress());

		ArrayList<NormalizeRequest> normalizeRequestList = new ArrayList<>();
		normalizeRequestList.add(normalizeRequest);

		// Act and Assert
		assertEquals(1, addressConverter.normalizeRequestListToWsNormAccInputModel(normalizeRequestList).size());
	}
}