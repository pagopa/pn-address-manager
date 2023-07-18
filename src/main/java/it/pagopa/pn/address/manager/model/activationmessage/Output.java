package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Output {
    @XmlAttribute(name = "PATH")
    private String path;

    // getters and setters
}
