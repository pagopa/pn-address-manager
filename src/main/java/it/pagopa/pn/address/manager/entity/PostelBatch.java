package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.LocalDateTime;

import static it.pagopa.pn.address.manager.constant.PostelBatchConstant.*;

@Data
@ToString
@DynamoDbBean
public class PostelBatch {

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(PK)
    }))
    private String batchId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_FILE_KEY)
    }))
    private String fileKey;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RETRY)
    }))
    private Integer retry;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TTL)
    }))
    private Long ttl;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_S)
    }))
    private String status;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_RESERVATION_ID)
    }))
    private String reservationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_LAST_RESERVED)
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TIMESTAMP)
    }))
    private LocalDateTime createdAt;
}
