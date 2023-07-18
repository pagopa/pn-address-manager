package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ActivationMessage {
    @XmlElement(name = "MSG")
    private Msg msg;
}

