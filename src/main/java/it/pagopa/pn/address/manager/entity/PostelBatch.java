package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

import static it.pagopa.pn.address.manager.constant.PostelBatchConstant.*;

@Data
@ToString
@DynamoDbBean
public class PostelBatch {

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_BATCH_ID),
            @DynamoDbPartitionKey
    }))
    private String batchId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_FILE_KEY),
    }))
    private String fileKey;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_SHA256)
    }))
    private String sha256;

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
            @DynamoDbSecondaryPartitionKey(indexNames = {GSI_S, GSI_SWT})
    }))
    private String status;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_LAST_RESERVED)
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_WORKINGTTL),
            @DynamoDbSecondarySortKey(indexNames = GSI_SWT)
    }))
    private LocalDateTime workingTtl;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TIMESTAMP)
    }))
    private LocalDateTime createdAt;
}
