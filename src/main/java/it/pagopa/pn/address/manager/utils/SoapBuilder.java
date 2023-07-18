package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.model.activationmessage.*;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL;

@Component
public class SoapBuilder {

    private static final String SOAP = "soap";
    private static final String ENV = "env";

    private static final String WS_ADDRESSING_NAMESPACE_URI = "http://www.w3.org/2005/08/addressing";

    public String soapActivationMessageBuilder() throws JAXBException, SOAPException, TransformerException {

        // Command
        Command command = new Command();
        command.setId(""); // TODO: Riporta l’identificativo del comando
        command.setAction("ExecuteNorm");

        // Msg
        Msg msg = new Msg();
        msg.setSender("PDI");
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String formattedDateTime = now.format(formatter);
        msg.setTimestamp(formattedDateTime);
        msg.setCommand(command);

        // Esterno
        Esterno esterno = new Esterno();
        esterno.setValue("SINI");

        // IdLotto
        IdLotto idLotto = new IdLotto();
        idLotto.setValue("SINI");

        // IdConf
        IdConf idConf = new IdConf();
        idConf.setValue("SINI");

        // AttachedFile
        AttachedFile attachedFile = new AttachedFile();
        attachedFile.setPathName("");

        // Output
        Output output = new Output();
        output.setPath("");

        // Input
        Input input = new Input();
        input.setOutput(output);
        input.setAttachedFile(attachedFile);
        input.setEsterno(esterno);
        input.setIdLotto(idLotto);
        input.setIdConf(idConf);

        // Item
        Item item = new Item();
        item.setInput(input);
        command.setItem(item);

        // ActivationMessage
        ActivationMessage activationMessage = new ActivationMessage();
        activationMessage.setMsg(msg);

        // ActivateSINIComponent
        ActivateSINIComponent activateSINIComponent = new ActivateSINIComponent();
        activateSINIComponent.setCommandId("test"); // TODO: Riporta l’identificativo del comando
        activateSINIComponent.setActivationMessage(activationMessage);
        // SOAPMessage
        SOAPMessage soapMessage = MessageFactory.newInstance(SOAP_1_2_PROTOCOL).createMessage();

        // SOAPEnvelope
        SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();

        // Change namespace declaration
        SOAPBody soapBody = soapMessage.getSOAPBody();
        SOAPHeader soapHeader = soapMessage.getSOAPHeader();

        soapEnvelope.removeNamespaceDeclaration(ENV);
        soapEnvelope.addNamespaceDeclaration("sep", "http://www.postel.it/pdi/SEPDICommonServices");

        // Set new prefix
        soapEnvelope.setPrefix(SOAP);
        soapHeader.setPrefix(SOAP);
        soapBody.setPrefix(SOAP);

        addWSAddressingHeaders(soapHeader);

        // Marshalling
        StringWriter sw = new StringWriter();
        JAXB.marshal(activationMessage, sw);
        JAXBContext jaxbContext = JAXBContext.newInstance(ActivateSINIComponent.class);
        Marshaller marshaller = jaxbContext.createMarshaller();


        marshaller.marshal(activateSINIComponent, soapBody);

        // Print
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        Source source = soapMessage.getSOAPPart().getContent();
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        transformer.transform(source, result);

        return stringWriter.toString();
    }

    private static void addWSAddressingHeaders(SOAPHeader soapHeader) throws SOAPException {

        // MessageID header
        SOAPHeaderElement messageIDElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, "MessageID"));
        messageIDElement.setTextContent("commandId");

        // ReplyTo header
        SOAPHeaderElement replyToElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, "ReplyTo"));
        SOAPElement addressElement = replyToElement.addChildElement("Address");
        addressElement.setTextContent("test");

        // From header
        SOAPHeaderElement fromElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, "From"));
        SOAPElement fromAddressElement = fromElement.addChildElement("Address");
        fromAddressElement.setTextContent("test");
    }
}
