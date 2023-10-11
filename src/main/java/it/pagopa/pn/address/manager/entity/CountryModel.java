package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@DynamoDbBean
public class CountryModel {

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("country")}))
    private String country;

    @Getter(onMethod=@__({@DynamoDbAttribute("startValidity")}))
    private LocalDateTime startValidity;

    @Getter(onMethod=@__({@DynamoDbAttribute("endValidity")}))
    private LocalDateTime endValidity;

}
