package it.pagopa.pn.address.manager.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingDecoratorTest {

    /**
     * Method under test: {@link RequestLoggingDecorator#RequestLoggingDecorator(ServerHttpRequest)}
     */
    @Test
    void testConstructor() {
        ServerHttpRequest delegate = mock(ServerHttpRequest.class);
        RequestLoggingDecorator requestLoggingDecorator = new RequestLoggingDecorator(delegate);
        assertTrue(requestLoggingDecorator.getCapturedBody().isEmpty());
    }

    /**
     * Method under test: {@link RequestLoggingDecorator#getBody()}
     */
    @Test
    void testGetBody() {
        ServerHttpRequest delegate = mock(ServerHttpRequest.class);
        DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap("test".getBytes());
        when(delegate.getBody()).thenReturn(Flux.fromIterable(List.of(dataBuffer)));
        RequestLoggingDecorator requestLoggingDecorator = new RequestLoggingDecorator(delegate);
        StepVerifier.create(requestLoggingDecorator.getBody())
                .expectNextCount(1)
                .verifyComplete();
    }

    /**
     * Method under test: {@link RequestLoggingDecorator#getCapturedBody()}
     */
    @Test
    void testGetCapturedBody() {
        ServerHttpRequest delegate = mock(ServerHttpRequest.class);
        DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap("test".getBytes());
        when(delegate.getBody()).thenReturn(Flux.fromIterable(List.of(dataBuffer)));
        RequestLoggingDecorator requestLoggingDecorator = new RequestLoggingDecorator(delegate);
        requestLoggingDecorator.getBody().blockLast(Duration.ofMillis(3000));
        String actualCapturedBody = requestLoggingDecorator.getCapturedBody();
        assertEquals("test", actualCapturedBody);
    }
}
