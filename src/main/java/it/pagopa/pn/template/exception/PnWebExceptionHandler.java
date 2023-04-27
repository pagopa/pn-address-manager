package it.pagopa.pn.template.exception;

import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

@Order(-2)
@Configuration
@Import(ExceptionHelper.class)
public class PnWebExceptionHandler extends PnErrorWebExceptionHandler{
    public PnWebExceptionHandler(ExceptionHelper exceptionHelper) { super(exceptionHelper); }

}
