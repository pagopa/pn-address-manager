package it.pagopa.pn.address.manager.log;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ResponseExchangeFilter implements ExchangeFilterFunction {

    private static final int MAX_LOGGED_BODY_SIZE = 400;

    @Override
    public @NotNull Mono<ClientResponse> filter(@NotNull ClientRequest request, ExchangeFunction next) {
        long start = System.currentTimeMillis();
        return next.exchange(interceptBody(request))
                .doOnError(WebClientResponseException.class, e -> logResponseBody(start, e, request))
                .map(response -> interceptBody(start, response, request));
    }

    private ClientResponse interceptBody(long startTime, ClientResponse response, ClientRequest request) {
        StringBuilder body = new StringBuilder();
        return response.mutate()
                .body(data -> data
                        .doOnNext(dataBuffer -> body.append(dataBuffer.toString(StandardCharsets.UTF_8)))
                        .doOnComplete(() -> logResponseBody(startTime, body.toString(), response, request)))
                .build();
    }

    public void logResponseBody(long startTime, String body, ClientResponse response, ClientRequest request) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Response HTTP from {} {} {} - body: {} - timelapse: {}ms",
                request.url(),
                response.statusCode().value(),
                response.statusCode().name(),
                String.format("%1."+MAX_LOGGED_BODY_SIZE+"s", body),
                duration);
    }

    public void logResponseBody(long startTime, WebClientResponseException exception, ClientRequest request) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Response HTTP from {} {} {} - body: {} - timelapse: {}ms",
                request.url(),
                exception.getStatusCode().value(),
                exception.getStatusCode().name(),
                exception.getResponseBodyAsString(),
                duration);
    }

    private ClientRequest interceptBody(ClientRequest request) {
        return ClientRequest.from(request)
                .body((outputMessage, context) -> request.body().insert(new ClientHttpRequestDecorator(outputMessage) {
                    @Override public @NotNull Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
                        return super.writeWith(Mono.from(body)
                                .doOnNext(dataBuffer -> logRequestBody(dataBuffer, request)));
                    }
                }, context))
                .build();
    }

    public void logRequestBody(DataBuffer dataBuffer, ClientRequest request) {
        log.info("Request HTTP {} to: {} - body: {}",
                request.method().name(),
                request.url(),
                dataBuffer.toString(StandardCharsets.UTF_8));
    }

}
