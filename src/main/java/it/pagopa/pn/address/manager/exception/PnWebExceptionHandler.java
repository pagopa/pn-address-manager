package it.pagopa.pn.address.manager.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.common.rest.error.v1.dto.Problem;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.normalizzatore.webhook.generated.generated.openapi.server.v1.dto.OperationResultCodeResponse;
import lombok.CustomLog;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import javax.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static it.pagopa.pn.address.manager.constant.AddressManagerConstant.*;

@CustomLog
@Order(-2)
@Configuration
@Import(ExceptionHelper.class)
public class PnWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ExceptionHelper exceptionHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PnWebExceptionHandler(ExceptionHelper exceptionHelper) {
        this.exceptionHelper = exceptionHelper;
        objectMapper.findAndRegisterModules();

        objectMapper
                .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                .configOverride(OffsetDateTime.class)
                .setFormat(JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange serverWebExchange, @NonNull Throwable throwable) {
        DataBuffer dataBuffer;
        DataBufferFactory bufferFactory = serverWebExchange.getResponse().bufferFactory();
        int status = Objects.requireNonNull(serverWebExchange.getResponse().getStatusCode()).value();

        Problem problem;
        if (throwable instanceof MethodArgumentNotValidException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR, SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof MissingServletRequestParameterException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR,  SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof WebExchangeBindException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR, SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof ServerWebInputException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR, SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof ConstraintViolationException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR, SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof MethodArgumentTypeMismatchException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SYNTAX_ERROR, SYNTAX_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else if (throwable instanceof PnAddressManagerException exception)
            dataBuffer = setError(bufferFactory, serverWebExchange, exception.getMessage(), SEMANTIC_ERROR, SEMANTIC_ERROR_CODE, HttpStatus.BAD_REQUEST.value());
        else {
            problem = exceptionHelper.handleException(throwable);
            try {
                dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(problem));
            } catch (JsonProcessingException e) {
                log.error("cannot output problem", e);
                dataBuffer = bufferFactory.wrap(exceptionHelper.generateFallbackProblem().getBytes(StandardCharsets.UTF_8));
            }
            serverWebExchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        }
        serverWebExchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return serverWebExchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    private DataBuffer setError(DataBufferFactory bufferFactory, ServerWebExchange serverWebExchange, String message, String errorType, String code, int status) {
        OperationResultCodeResponse problem = convertToOperationResultCodeResponse(message, errorType, code);
        DataBuffer dataBuffer;
        try {
            dataBuffer = bufferFactory.wrap(objectMapper.writeValueAsBytes(problem));
        } catch (JsonProcessingException e) {
            log.error("cannot output problem", e);
            dataBuffer = bufferFactory.wrap(exceptionHelper.generateFallbackProblem().getBytes(StandardCharsets.UTF_8));
        }
        serverWebExchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        return dataBuffer;
    }

    private OperationResultCodeResponse convertToOperationResultCodeResponse(String message, String errorType, String code) {
        OperationResultCodeResponse operationResultCodeResponse = new OperationResultCodeResponse();
        operationResultCodeResponse.setResultCode(code);
        operationResultCodeResponse.setResultDescription(errorType);
        operationResultCodeResponse.setErrorList(List.of(Objects.requireNonNull(message)));
        operationResultCodeResponse.setClientResponseTimeStamp(Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        return operationResultCodeResponse;
    }
}
