package it.pagopa.pn.address.manager.entity;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Data
@DynamoDbBean
public class PostelResponseCodes {

    private String id;
    private PostelResponseCodeRecipient postelResponseCodeRecipient;
}
