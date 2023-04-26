package it.pagopa.pn.template.exception;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ChannelSendOperator;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

@ExtendWith(MockitoExtension.class)
class PnWebExceptionHandlerTest {
    @Mock
    private ExceptionHelper exceptionHelper;

    @InjectMocks
    private PnWebExceptionHandler pnWebExceptionHandler;

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle4() {
        when(exceptionHelper.handleException(any()))
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        Throwable throwable = new Throwable();
        assertThrows(PnAddressManagerException.class,
                () -> pnWebExceptionHandler.handle(defaultServerWebExchange, throwable));
        verify(exceptionHelper).handleException(any());
        verify(defaultServerWebExchange).getResponse();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle5() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(1);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(problem).status(any());
        verify(problem, atLeast(1)).getStatus();
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle7() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(500);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(new MockServerHttpResponse());
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(problem).status(any());
        verify(problem, atLeast(1)).getStatus();
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle9() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(1);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse())
                .thenReturn(new HttpHeadResponseDecorator(new MockServerHttpResponse()));
        pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable());
        verify(exceptionHelper).handleException(any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(problem).status(any());
        verify(problem, atLeast(1)).getStatus();
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle10() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(1);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        ChannelSendOperator<Object> channelSendOperator = mock(ChannelSendOperator.class);

        when(serverHttpResponse.writeWith(any())).thenReturn(channelSendOperator);
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(serverHttpResponse.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(serverHttpResponse);
        assertSame(channelSendOperator, pnWebExceptionHandler.handle(defaultServerWebExchange, new Throwable()));
        verify(exceptionHelper).handleException(any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(problem).status(any());
        verify(problem, atLeast(1)).getStatus();
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
        verify(serverHttpResponse).bufferFactory();
        verify(serverHttpResponse).getHeaders();
        verify(serverHttpResponse).writeWith(any());
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle11() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(1);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        when(serverHttpResponse.writeWith(any()))
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(serverHttpResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(serverHttpResponse.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(serverHttpResponse);
        Throwable throwable = new Throwable();
        assertThrows(PnAddressManagerException.class,
                () -> pnWebExceptionHandler.handle(defaultServerWebExchange, throwable));
        verify(exceptionHelper).handleException(any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(problem).status(any());
        verify(problem, atLeast(1)).getStatus();
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
        verify(serverHttpResponse).bufferFactory();
        verify(serverHttpResponse).getHeaders();
        verify(serverHttpResponse).writeWith(any());
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle13() {
        Problem problem = mock(Problem.class);
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        when(serverHttpResponse.getHeaders())
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(serverHttpResponse.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(serverHttpResponse);
        PnAddressManagerException pnAddressManagerException = new PnAddressManagerException("An error occurred",
                HttpStatus.CONTINUE);
        assertThrows(PnAddressManagerException.class,
                () -> pnWebExceptionHandler.handle(defaultServerWebExchange, pnAddressManagerException));
        verify(problem).status(any());
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle12() {
        Problem problem = mock(Problem.class);
        when(problem.getStatus()).thenReturn(1);
        doNothing().when(problem).setTimestamp(any());
        doNothing().when(problem).setTraceId(any());
        when(problem.status(any())).thenReturn(new Problem());
        problem.status(1);
        when(exceptionHelper.generateFallbackProblem()).thenReturn("trace_id");
        when(exceptionHelper.handleException(any())).thenReturn(problem);
        ServerHttpResponse serverHttpResponse = mock(ServerHttpResponse.class);
        when(serverHttpResponse.getHeaders())
                .thenThrow(new PnAddressManagerException("An error occurred", HttpStatus.CONTINUE));
        when(serverHttpResponse.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        ServerHttpRequestDecorator serverHttpRequestDecorator = mock(ServerHttpRequestDecorator.class);
        when(serverHttpRequestDecorator.getURI())
                .thenReturn(Paths.get(System.getProperty("java.io.tmpdir"), "test.txt").toUri());
        DefaultServerWebExchange defaultServerWebExchange = mock(DefaultServerWebExchange.class);
        when(defaultServerWebExchange.getRequest())
                .thenReturn(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(
                        new ServerHttpRequestDecorator(new ServerHttpRequestDecorator(serverHttpRequestDecorator))))));
        when(defaultServerWebExchange.getResponse()).thenReturn(serverHttpResponse);
        RuntimeException runtimeException = new RuntimeException();
        assertThrows(PnAddressManagerException.class,
                () -> pnWebExceptionHandler.handle(defaultServerWebExchange, runtimeException));
        verify(problem).status(any());
        verify(problem).setTimestamp(any());
        verify(problem).setTraceId(any());
        verify(defaultServerWebExchange).getRequest();
        verify(defaultServerWebExchange, atLeast(1)).getResponse();
        verify(serverHttpRequestDecorator).getURI();
        verify(serverHttpResponse).bufferFactory();
        verify(serverHttpResponse).getHeaders();
    }
}

