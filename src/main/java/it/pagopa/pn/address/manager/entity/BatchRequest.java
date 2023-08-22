package it.pagopa.pn.address.manager.entity;

import it.pagopa.pn.address.manager.constant.BatchRequestConstant;
import it.pagopa.pn.address.manager.generated.openapi.server.v1.dto.AnalogAddress;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.pn.address.manager.constant.BatchRequestConstant.*;

@Data
@ToString
@DynamoDbBean
public class BatchRequest {

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(PK)
    }))
    private String correlationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ADDRESSES)
    }))
    private List<AnalogAddress> addresses;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_BATCH_ID),
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_BL)
    }))
    private String batchId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RETRY)
    }))
    private Integer retry;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TTL)
    }))
    private Long ttl;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CLIENT_ID)
    }))
    private String clientId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = BatchRequestConstant.GSI_S)
    }))
    private String status;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_LAST_RESERVED),
            @DynamoDbSecondarySortKey(indexNames = {GSI_BL, GSI_SSL})
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RESERVATION_ID)
    }))
    private String reservationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TIMESTAMP)
    }))
    private LocalDateTime createdAt;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_SEND_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_SSL)
    }))
    private String sendStatus;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_MESSAGE)
    }))
    private String message;
}
