package it.pagopa.pn.address.manager.entity;

import it.pagopa.pn.address.manager.constant.BatchAddressConstant;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.LocalDateTime;

import static it.pagopa.pn.address.manager.constant.BatchAddressConstant.*;

@Data
@ToString
@DynamoDbBean
public class BatchAddress {

    @Getter(onMethod = @__({
            @DynamoDbPartitionKey,
            @DynamoDbAttribute(PK)
    }))
    private String id;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ADDRESS_ID)
    }))
    private String addressId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CORRELATION_ID)
    }))
    private String correlationId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CX_ID)
    }))
    private String cxId;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ADDRESS_ROW)
    }))
    private String addressRow;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_ADDRESS_ROW_2)
    }))
    private String addressRow2;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CAP)
    }))
    private String cap;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CITY)
    }))
    private String city;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_CITY_2)
    }))
    private String city2;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_PR)
    }))
    private String pr;

    @Getter(onMethod = @__({
            @DynamoDbAttribute(COL_COUNTRY)
    }))
    private String country;

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
            @DynamoDbAttribute(COL_STATUS),
            @DynamoDbSecondaryPartitionKey(indexNames = BatchAddressConstant.GSI_S)
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
