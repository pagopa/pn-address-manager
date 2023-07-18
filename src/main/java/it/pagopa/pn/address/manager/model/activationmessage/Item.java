package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
@Data

@XmlAccessorType(XmlAccessType.FIELD)
public class Item {
    @XmlElement(name = "INPUT")
    private Input input;
}
