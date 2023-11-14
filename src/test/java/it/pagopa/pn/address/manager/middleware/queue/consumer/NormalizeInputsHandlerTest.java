package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(classes = {NormalizeInputsHandler.class})
@ExtendWith(SpringExtension.class)
class NormalizeInputsHandlerTest {
    @MockBean
    private NormalizeAddressService normalizeAddressService;

    @Autowired
    private NormalizeInputsHandler normalizeInputsHandler;

    /**
     * Method under test: {@link NormalizeInputsHandler#pnAddressManagerRequestConsumer()}
     */
    @Test
    void testPnAddressManagerRequestConsumer() {
       assertNotNull(normalizeInputsHandler.pnAddressManagerRequestConsumer());
    }
}

