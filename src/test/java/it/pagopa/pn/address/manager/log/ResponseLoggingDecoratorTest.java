package it.pagopa.pn.address.manager.log;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResponseLoggingDecoratorTest {

    /**
     * Method under test: {@link ResponseLoggingDecorator#ResponseLoggingDecorator(ServerHttpResponse)}
     */
    @Test
    void testConstructor() {
        ResponseLoggingDecorator responseLoggingDecorator = new ResponseLoggingDecorator(new MockServerHttpResponse());
        assertEquals("", responseLoggingDecorator.getCapturedBody());
        assertNull(responseLoggingDecorator.getStatusCode());
    }

    /**
     * Method under test: {@link ResponseLoggingDecorator#writeWith(Publisher)}
     */
    @Test
    void testWriteWith() {
        DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap("test".getBytes());
        MockServerHttpResponse mockServerHttpResponse = new MockServerHttpResponse(DefaultDataBufferFactory.sharedInstance);
        ResponseLoggingDecorator responseLoggingDecorator = new ResponseLoggingDecorator(mockServerHttpResponse);
        responseLoggingDecorator.writeWith(Mono.just(dataBuffer)).block(Duration.ofMillis(3000));
        assertEquals("test", responseLoggingDecorator.getCapturedBody());
    }
}
