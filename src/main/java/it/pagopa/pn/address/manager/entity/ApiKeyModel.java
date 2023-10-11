package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@NoArgsConstructor
@DynamoDbBean
public class ApiKeyModel {

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("cxId")}))
    private String cxId;

    @Getter(onMethod=@__({@DynamoDbAttribute("apikey")}))
    private String apiKey;

}
