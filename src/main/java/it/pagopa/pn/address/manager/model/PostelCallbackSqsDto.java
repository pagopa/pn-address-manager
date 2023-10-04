package it.pagopa.pn.address.manager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PostelCallbackSqsDto {
    private String fileKeyInput;
    private String fileKeyOutput;
    private String fileOutputUrl;
}
