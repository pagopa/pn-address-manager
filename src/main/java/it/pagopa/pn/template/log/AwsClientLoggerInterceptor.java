package it.pagopa.pn.template.log;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.interceptor.*;

@Slf4j
public class AwsClientLoggerInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<String> SERVICE_NAME = SdkExecutionAttribute.SERVICE_NAME;
    private static final ExecutionAttribute<String> OPERATION_NAME = SdkExecutionAttribute.OPERATION_NAME;
    private static final ExecutionAttribute<Long> START_TIME= new ExecutionAttribute<>("startTime");

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);
        log.info("START - {}.{} - {}", serviceName, operationName, context.request());
        executionAttributes.putAttribute(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        Long startTime = executionAttributes.getAttribute(START_TIME);
        Long elapsed = startTime != null ? (System.currentTimeMillis() - startTime) : null;

        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);
        log.info("END - {}.{} - request: {} - timelapse: {} ms",serviceName, operationName, context.request(), elapsed);

    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);
        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        log.warn("{}.{}", serviceName, operationName, context.exception());
    }

}
