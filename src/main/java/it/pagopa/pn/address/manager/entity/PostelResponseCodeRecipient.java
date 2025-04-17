package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@DynamoDbBean
public class PostelResponseCodeRecipient {

    private Integer nRisultatoNorm;
    private Integer fPostalizzabile;
    private Integer nErroreNorm;
}
