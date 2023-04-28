package it.pagopa.pn.address.manager.log;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestResponseLoggingFilter implements WebFilter {

    static final String LOG_REQUEST = "Request HTTP {} to {}";
    static final String LOG_REQUEST_BODY = "Request HTTP {} to {} - body: {}";

    static final String LOG_RESPONSE_OK = "Response from {} {} - body: {} - timelapse: {}ms";

    private final String healthCheckPath;

    public RequestResponseLoggingFilter(@Value("${pn.address.manager.health-check-path}") String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        final ServerHttpRequest httpRequest = exchange.getRequest();

        if (healthCheckPath.equalsIgnoreCase(httpRequest.getURI().getPath())) {
            log.trace("request to health-check actuator");
            return chain.filter(exchange);
        }

        final HttpMethod httpMethod = httpRequest.getMethod();
        final String httpUrl = httpRequest.getURI().toString();

        long startTime = System.currentTimeMillis();

        HttpHeaders headers = httpRequest.getHeaders();
        if (headers.getContentLength() <= 0) {
            // if the request does not include a body, then I run the log here,
            // but if the body is present the request is logged by the RequestDecorator
            log.info(LOG_REQUEST, httpMethod, httpUrl);
        }

        RequestLoggingDecorator requestDecorator = new RequestLoggingDecorator(httpRequest);
        ResponseLoggingDecorator responseDecorator = new ResponseLoggingDecorator(exchange.getResponse());
        return chain.filter(exchange.mutate().request(requestDecorator).response(responseDecorator).build())
                .doOnTerminate(() -> {
                    var body = responseDecorator.getCapturedBody();
                    var elapsed = System.currentTimeMillis() - startTime;
                    log.info(LOG_RESPONSE_OK, httpMethod, httpUrl, body, elapsed);
                });
    }
}

