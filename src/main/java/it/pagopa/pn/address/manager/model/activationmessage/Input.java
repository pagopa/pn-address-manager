package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
@Data

@XmlAccessorType(XmlAccessType.FIELD)
public class Input {
    @XmlElement(name = "ATTACHED_FILE")
    private AttachedFile attachedFile;

    @XmlElement
    private Esterno esterno;

    @XmlElement
    private IdLotto idLotto;

    @XmlElement(name = "OUPUT")
    private Output output;

    @XmlElement(name = "IDCONF")
    private IdConf idConf;

}
