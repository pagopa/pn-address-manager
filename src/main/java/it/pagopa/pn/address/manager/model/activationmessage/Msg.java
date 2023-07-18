package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.*;
@Data

@XmlAccessorType(XmlAccessType.FIELD)
public class Msg {
    @XmlAttribute
    private String sender;

    @XmlAttribute
    private String timestamp;

    @XmlElement(name = "COMMAND")
    private Command command;
}
