package it.pagopa.pn.address.manager.exception;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.pn.address.manager.log.RequestLoggingDecorator;
import it.pagopa.pn.address.manager.log.ResponseLoggingDecorator;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.WebSessionManager;

@ContextConfiguration(classes = {PnWebExceptionHandler.class})
@ExtendWith(SpringExtension.class)
class PnWebExceptionHandlerTest {
    @MockBean
    private ExceptionHelper exceptionHelper;

    @Autowired
    private PnWebExceptionHandler pnWebExceptionHandler;


    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle4() {
        when(exceptionHelper.handleException(Mockito.<Throwable>any())).thenReturn(new Problem());
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn(new ResponseLoggingDecorator(new ResponseLoggingDecorator(
                new ResponseLoggingDecorator(new ResponseLoggingDecorator(new ResponseLoggingDecorator(delegate))))));
        pnWebExceptionHandler.handle(serverWebExchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
        verify(serverWebExchange, atLeast(1)).getResponse();
        verify(delegate).setStatusCode(Mockito.<HttpStatus>any());
        verify(delegate).bufferFactory();
        verify(delegate, atLeast(1)).getHeaders();
        verify(delegate).getStatusCode();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle6() {
        OffsetDateTime timestamp = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        when(exceptionHelper.handleException(Mockito.<Throwable>any()))
                .thenReturn(new Problem("Type", 1, "Dr", "Detail", "42", timestamp, new ArrayList<>()));
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn(new ResponseLoggingDecorator(new ResponseLoggingDecorator(
                new ResponseLoggingDecorator(new ResponseLoggingDecorator(new ResponseLoggingDecorator(delegate))))));
        pnWebExceptionHandler.handle(serverWebExchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
        verify(serverWebExchange, atLeast(1)).getResponse();
        verify(delegate).setStatusCode(Mockito.<HttpStatus>any());
        verify(delegate).bufferFactory();
        verify(delegate, atLeast(1)).getHeaders();
        verify(delegate).getStatusCode();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle7() {
        when(exceptionHelper.handleException(Mockito.<Throwable>any())).thenReturn(null);
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn(new ResponseLoggingDecorator(new ResponseLoggingDecorator(
                new ResponseLoggingDecorator(new ResponseLoggingDecorator(new ResponseLoggingDecorator(delegate))))));
        pnWebExceptionHandler.handle(serverWebExchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
        verify(serverWebExchange, atLeast(1)).getResponse();
        verify(delegate).setStatusCode(Mockito.<HttpStatus>any());
        verify(delegate).bufferFactory();
        verify(delegate, atLeast(1)).getHeaders();
        verify(delegate).getStatusCode();
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle8() {
        when(exceptionHelper.generateFallbackProblem()).thenReturn("Generate Fallback Problem");
        when(exceptionHelper.handleException(Mockito.<Throwable>any())).thenReturn(mock(Problem.class));
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.CONTINUE);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn(new ResponseLoggingDecorator(new ResponseLoggingDecorator(
                new ResponseLoggingDecorator(new ResponseLoggingDecorator(new ResponseLoggingDecorator(delegate))))));
        pnWebExceptionHandler.handle(serverWebExchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(serverWebExchange, atLeast(1)).getResponse();
        verify(delegate).setStatusCode(Mockito.<HttpStatus>any());
        verify(delegate).bufferFactory();
        verify(delegate, atLeast(1)).getHeaders();
        verify(delegate).getStatusCode();
    }

}

