package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@Data

@XmlAccessorType(XmlAccessType.FIELD)
public class Command {
    @XmlAttribute
    private String action;

    @XmlAttribute
    private String id;

    @XmlElement(name = "ITEM")
    private Item item;
}
