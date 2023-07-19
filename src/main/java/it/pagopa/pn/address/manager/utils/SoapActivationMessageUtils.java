package it.pagopa.pn.address.manager.utils;

import it.pagopa.pn.address.manager.exception.PnAddressManagerException;
import it.pagopa.pn.address.manager.model.activationmessage.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
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
import java.util.UUID;

import static it.pagopa.pn.address.manager.exception.PnAddressManagerExceptionCodes.ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI;
import static javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL;

@Component
public class SoapActivationMessageUtils {

    private static final String SOAP = "soap";
    private static final String ENV = "env";
    private static final String WS_ADDRESSING_NAMESPACE_URI = "http://www.w3.org/2005/08/addressing";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String SENDER = "PDI";
    private static final String ACTION = "ExecuteNorm";
    private static final String ESTERNO_VALUE = "TRUE";
    private static final String ID_CONF = ""; // TODO: Identificativo di configurazione da utilizzare.
    private static final String PATH_NAME = "normalizzatore/in/"; // TODO: Una stringa contenente il nome completo del file da normalizzare (comprensivo di estensione).
    private static final String OUTPUT_PATH = "normalizzatore/out"; // TODO: Stringa contenente il path del file normalizzato.
    private static final String SEP_PREFIX = "sep";
    private static final String SEP_URL = "http://www.postel.it/pdi/SEPDICommonServices";
    private static final String REPLY_TO = ""; // TODO: l’endpoint a cui inviare la callback.
    private static final String FROM = ""; // TODO: l’endpoint del sistema chiamante.
    private static final String MESSAGE_ID_FIELD = "MessageID";
    private static final String REPLY_TO_FIELD = "ReplyTo";
    private static final String FROM_FIELD = "From";

    public String soapActivationMessageBuilder() {

        String commandId = UUID.randomUUID().toString();
        ActivateSINIComponent activateSINIComponent = createActivateSINIComponent(commandId);

        try {
            // SOAPMessage
            SOAPMessage soapMessage = MessageFactory.newInstance(SOAP_1_2_PROTOCOL).createMessage();

            // SOAPEnvelope
            SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();

            // Change namespace declaration
            soapEnvelope.removeNamespaceDeclaration(ENV);
            soapEnvelope.addNamespaceDeclaration(SEP_PREFIX, SEP_URL);

            // Set new prefix
            SOAPBody soapBody = soapMessage.getSOAPBody();
            SOAPHeader soapHeader = soapMessage.getSOAPHeader();

            soapEnvelope.setPrefix(SOAP);
            soapHeader.setPrefix(SOAP);
            soapBody.setPrefix(SOAP);

            // Add WS-Addressing headers
            addWSAddressingHeaders(soapHeader, commandId);

            // Marshalling
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

        } catch (SOAPException | TransformerException | JAXBException ex) {
            throw new PnAddressManagerException(ex.getMessage(), null,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI);
        }
    }

    private ActivateSINIComponent createActivateSINIComponent(String commandId) {
        ActivateSINIComponent activateSINIComponent = new ActivateSINIComponent();
        activateSINIComponent.setCommandId(commandId);
        activateSINIComponent.setActivationMessage(createActivationMessage(commandId));
        return activateSINIComponent;
    }

    private ActivationMessage createActivationMessage(String commandId) {
        ActivationMessage activationMessage = new ActivationMessage();
        activationMessage.setMsg(createMsg(commandId));
        return activationMessage;
    }

    private Item createItem() {
        Item item = new Item();
        item.setInput(createInput());
        return item;
    }

    private Input createInput() {
        Input input = new Input();
        input.setOutput(createOutput());
        input.setAttachedFile(createAttachedFile());
        input.setEsterno(createEsterno());
        input.setIdLotto(createIdLotto());
        input.setIdConf(createIdConf());

        return input;
    }

    private Output createOutput() {
        Output output = new Output();
        output.setPath(OUTPUT_PATH);
        return output;
    }

    private Command createCommand(String commandId) {
        Command command = new Command();
        command.setId(commandId);
        command.setAction(ACTION);
        command.setItem(createItem());
        return command;
    }

    private AttachedFile createAttachedFile() {
        AttachedFile attachedFile = new AttachedFile();
        attachedFile.setPathName(PATH_NAME);
        return attachedFile;
    }

    private IdConf createIdConf() {
        IdConf idConf = new IdConf();
        idConf.setValue(ID_CONF);
        return idConf;
    }

    private IdLotto createIdLotto() {
        IdLotto idLotto = new IdLotto();
        idLotto.setValue(generateUUID().toString());
        return idLotto;
    }

    private Esterno createEsterno() {
        Esterno esterno = new Esterno();
        esterno.setValue(ESTERNO_VALUE);
        return esterno;
    }

    private static UUID generateUUID() {
        return UUID.randomUUID();
    }

    private Msg createMsg(String commandId) {
        // Msg
        Msg msg = new Msg();
        msg.setSender(SENDER);
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        String formattedDateTime = now.format(formatter);
        msg.setTimestamp(formattedDateTime);
        msg.setCommand(createCommand(commandId));

        return msg;
    }

    private static void addWSAddressingHeaders(SOAPHeader soapHeader, String commandId) {
        try {
            // MessageID header
            SOAPHeaderElement messageIDElement;
            messageIDElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, MESSAGE_ID_FIELD));
            messageIDElement.setTextContent(commandId);

            // ReplyTo header
            SOAPHeaderElement replyToElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, REPLY_TO_FIELD));
            replyToElement.setTextContent(REPLY_TO);

            // From header
            SOAPHeaderElement fromElement = soapHeader.addHeaderElement(new QName(WS_ADDRESSING_NAMESPACE_URI, FROM_FIELD));
            fromElement.setTextContent(FROM);
        } catch (SOAPException ex) {
            throw new PnAddressManagerException(ex.getMessage(), null,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), ERROR_ADDRESS_MANAGER_POSTEL_ATTIVAZIONE_SINI);
        }
    }
}
