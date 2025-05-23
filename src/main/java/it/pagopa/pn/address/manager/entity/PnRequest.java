package it.pagopa.pn.address.manager.entity;

import it.pagopa.pn.address.manager.converter.LocalDateTimeToInstant;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

import static it.pagopa.pn.address.manager.constant.PnRequestConstant.*;

@Data
@ToString
@DynamoDbBean
public class PnRequest {

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(PK)
    }))
    private String correlationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ADDRESSES)
    }))
    private String addresses;

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
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_S)
    }))
    private String status;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_LAST_RESERVED),
            @DynamoDbSecondarySortKey(indexNames = {GSI_BL, GSI_SSL}),
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime lastReserved;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CREATED_AT),
            @DynamoDbSortKey,
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    @Setter(onMethod = @__({
            @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime createdAt;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_SEND_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = GSI_SSL)
    }))
    private String sendStatus;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_POSTEL_RESPONSE_CODES)
    }))
    private List<PostelResponseCodes> postelResponseCodes;

    @ToString.Exclude
    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_MESSAGE)
    }))
    private String message;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_XAPIKEY)
    }))
    private String xApiKey;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_AWS_MESSAGE_ID)
    }))
    private String awsMessageId;
}
