package it.pagopa.pn.address.manager.exception;

import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
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
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.CONTINUE);
        pnWebExceptionHandler.handle(exchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());
    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle6() {
        OffsetDateTime timestamp = OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        when(exceptionHelper.handleException(Mockito.<Throwable>any()))
                .thenReturn(new Problem("Type", 1, "Dr", "Detail", "42", timestamp, new ArrayList<>()));
        when(exceptionHelper.handleException(Mockito.<Throwable>any())).thenReturn(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.CONTINUE);
        pnWebExceptionHandler.handle(exchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());

    }

    /**
     * Method under test: {@link PnWebExceptionHandler#handle(ServerWebExchange, Throwable)}
     */
    @Test
    void testHandle7() {
        when(exceptionHelper.handleException(Mockito.<Throwable>any())).thenReturn(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.CONTINUE);
        pnWebExceptionHandler.handle(exchange, new Throwable());
        verify(exceptionHelper).handleException(Mockito.<Throwable>any());

    }


    @Test
    void MethodArgumentNotValidException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }
    @Test
    void MissingServletRequestParameterException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        MissingServletRequestParameterException exception = mock(MissingServletRequestParameterException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }
    @Test
    void WebExchangeBindException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getMessage()).thenReturn("error");

        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }
    @Test
    void ServerWebInputException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        ServerWebInputException exception = new ServerWebInputException("error");

        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();

    }
    @Test
    void ConstraintViolationException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }
    @Test
    void MethodArgumentTypeMismatchException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }

    @Test
    void PnAddressManagerException() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build()
        );
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);

        PnAddressManagerException exception = mock(PnAddressManagerException.class);
        when(exception.getMessage()).thenReturn("error");
        StepVerifier.create(pnWebExceptionHandler.handle(exchange, exception))
                .verifyComplete();
    }
}

