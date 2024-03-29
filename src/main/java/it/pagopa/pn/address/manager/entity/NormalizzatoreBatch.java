package it.pagopa.pn.address.manager.entity;

import it.pagopa.pn.address.manager.converter.LocalDateTimeToInstant;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

import static it.pagopa.pn.address.manager.constant.NormalizzatoreBatchConstant.*;

@Data
@ToString
@DynamoDbBean
public class NormalizzatoreBatch {

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
            @DynamoDbAttribute(COL_OUTPUT_FILE_KEY),
    }))
    private String outputFileKey;

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
            @DynamoDbAttribute(COL_LAST_RESERVED),
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_WORKINGTTL),
            @DynamoDbSecondarySortKey(indexNames = GSI_SWT),
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime workingTtl;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_TIMESTAMP),
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime createdAt;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CALLBACK_TIMESTAMP),
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime callbackTimeStamp;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ERROR)
    }))
    private String error;
}
