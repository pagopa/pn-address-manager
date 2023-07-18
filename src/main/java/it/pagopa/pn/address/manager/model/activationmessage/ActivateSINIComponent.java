package it.pagopa.pn.address.manager.model.activationmessage;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "ActivateSINIComponent", namespace = "http://www.postel.it/pdi/SEPDICommonServices")
@XmlAccessorType(XmlAccessType.FIELD)
public class ActivateSINIComponent {
    @XmlElement(name = "commandId", namespace = "http://www.postel.it/pdi/SEPDICommonServices")
    private String commandId;

    @XmlElement(name = "activationMessage", namespace = "http://www.postel.it/pdi/SEPDICommonServices")
    private ActivationMessage activationMessage;

}
