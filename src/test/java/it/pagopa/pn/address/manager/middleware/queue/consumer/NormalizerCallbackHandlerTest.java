package it.pagopa.pn.address.manager.middleware.queue.consumer;

import it.pagopa.pn.address.manager.service.NormalizeAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(classes = {NormalizerCallbackHandler.class})
@ExtendWith(SpringExtension.class)
class NormalizerCallbackHandlerTest {
    @MockBean
    private NormalizeAddressService normalizeAddressService;

    @Autowired
    private NormalizerCallbackHandler normalizerCallbackHandler;

    /**
     * Method under test: {@link NormalizerCallbackHandler#pnAddressManagerPostelCallbackConsumer()}
     */
    @Test
    void testPnAddressManagerPostelCallbackConsumer() {
        assertNotNull(normalizerCallbackHandler.pnAddressManagerPostelCallbackConsumer());
    }
}

