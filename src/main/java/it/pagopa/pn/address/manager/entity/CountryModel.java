package it.pagopa.pn.address.manager.entity;

import it.pagopa.pn.address.manager.converter.LocalDateTimeToInstant;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@DynamoDbBean
public class CountryModel {

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute("country")}))
    private String country;

    @Getter(onMethod=@__({@DynamoDbAttribute("startValidity"), @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime startValidity;

    @Getter(onMethod=@__({@DynamoDbAttribute("endValidity"), @DynamoDbConvertedBy(LocalDateTimeToInstant.class)
    }))
    private LocalDateTime endValidity;
}
