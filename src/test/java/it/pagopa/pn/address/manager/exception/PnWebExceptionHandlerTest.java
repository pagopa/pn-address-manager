package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHeadResponseDecorator;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import reactor.test.StepVerifier;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {PnWebExceptionHandler.class})
@ExtendWith(SpringExtension.class)
class PnWebExceptionHandlerTest {
    @MockitoBean
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
        when(serverWebExchange.getResponse()).thenReturn((delegate));
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
        when(serverWebExchange.getResponse()).thenReturn((delegate));
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
        when(serverWebExchange.getResponse()).thenReturn((delegate));
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
        when(serverWebExchange.getResponse()).thenReturn((delegate));
        pnWebExceptionHandler.handle(serverWebExchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
        verify(exceptionHelper).generateFallbackProblem();
        verify(serverWebExchange, atLeast(1)).getResponse();
        verify(delegate).setStatusCode(Mockito.<HttpStatus>any());
        verify(delegate).bufferFactory();
        verify(delegate, atLeast(1)).getHeaders();
        verify(delegate).getStatusCode();
    }

    @Test
    void MethodArgumentNotValidException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
    @Test
    void MissingServletRequestParameterException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));
        MissingServletRequestParameterException exception = mock(MissingServletRequestParameterException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
    @Test
    void WebExchangeBindException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
    @Test
    void ServerWebInputException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));

        ServerWebInputException exception = mock(ServerWebInputException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
    @Test
    void ConstraintViolationException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));

        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
    @Test
    void MethodArgumentTypeMismatchException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));

        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }

    @Test
    void PnAddressManagerException() {
        HttpHeadResponseDecorator delegate = mock(HttpHeadResponseDecorator.class);
        when(delegate.setStatusCode(Mockito.<HttpStatus>any())).thenReturn(true);
        when(delegate.getHeaders()).thenReturn(new HttpHeaders());
        when(delegate.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(delegate.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        DefaultServerWebExchange serverWebExchange = mock(DefaultServerWebExchange.class);
        when(serverWebExchange.getResponse()).thenReturn((delegate));

        PnAddressManagerException exception = mock(PnAddressManagerException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(serverWebExchange, exception))
                .verifyComplete();
    }
}

