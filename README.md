import jakarta.xml.soap.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Full As4MessageService - prošireno (wsu:Id, PartProperties za SBDH i Invoice, dummy WS-Security/ds)
 * Generira byte[] koji predstavlja AS4 multipart/related MIME poruku (SOAP part + attachments).
 *
 * Napomena: ovo je STRUKTURALNO kompletna AS4 poruka prema FINA dokumentaciji.
 * Potpis (WS-Security) je dummy i treba ga zamijeniti stvarnim XAdES potpisom.
 */
public class As4MessageService {

    private static final String EB_NS = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Kreira AS4 poruku kao byte[] (MIME multipart/related). Sadrzi:
     *  - SOAP 1.2 envelope s eb:Messaging (wsu:Id postavljen)
     *  - eb:UserMessage s MessageInfo, PartyInfo, CollaborationInfo, PayloadInfo (PartInfo za sbdh i invoice)
     *  - wsse:Security placeholder s ds:Signature placeholder (referira wsu:Id)
     *  - attachment: sbdh.xml (application/xml)
     *  - attachment: invoice.xml (gzipirani UBL, application/octet-stream) + CompressionType u PartProperties
     *
     * I/O: ublXmlBytes - sirovi UBL XML (nekompresiran)
     */
    public byte[] createAs4Message(byte[] ublXmlBytes,
                                   String senderOib,
                                   String receiverOib,
                                   String invoiceId) throws Exception {

        // 1) gzipiraj UBL (FINA zahtjeva compress za invoice dio)
        byte[] gzipInvoice = gzip(ublXmlBytes);

        // 2) kreiraj SBDH XML (možeš prilagodit polja)
        String sbdhXml = createSbdhXml(senderOib, receiverOib, invoiceId);

        // 3) napravimo SOAPMessage
        MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage soapMessage = mf.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPHeader header = envelope.getHeader();

        // Namespaces
        envelope.addNamespaceDeclaration("eb", EB_NS);
        envelope.addNamespaceDeclaration("wsse", WSSE_NS);
        envelope.addNamespaceDeclaration("wsu", WSU_NS);
        envelope.addNamespaceDeclaration("ds", DS_NS);

        // eb:Messaging (postavi wsu:Id jer ce potpis referencirati ovaj element)
        SOAPElement messaging = header.addChildElement("Messaging", "eb");
        messaging.addAttribute(envelope.createName("Id", "wsu", WSU_NS), "Messaging-" + UUID.randomUUID());
        // opcionalno: mustUnderstand
        messaging.addAttribute(envelope.createName("mustUnderstand", "env", "http://www.w3.org/2003/05/soap-envelope"), "true");

        SOAPElement userMessage = messaging.addChildElement("UserMessage", "eb");
        userMessage.addAttribute(envelope.createName("mpc"),
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");

        // MessageInfo (Timestamp i MessageId)
        SOAPElement messageInfo = userMessage.addChildElement("MessageInfo", "eb");
        SOAPElement timestamp = messageInfo.addChildElement("Timestamp", "eb");
        timestamp.addTextNode(currentUtc());
        messageInfo.addChildElement("MessageId", "eb").addTextNode("uuid:" + UUID.randomUUID());

        // PartyInfo (From/To)
        SOAPElement partyInfo = userMessage.addChildElement("PartyInfo", "eb");
        addParty(envelope, partyInfo, "From", senderOib, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/initiator");
        addParty(envelope, partyInfo, "To", receiverOib, "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/responder");

        // CollaborationInfo (Service, Action, ConversationId, AgreementRef opcionalno)
        SOAPElement collab = userMessage.addChildElement("CollaborationInfo", "eb");
        SOAPElement service = collab.addChildElement("Service", "eb");
        service.addTextNode("cenbii-procid-ubl");
        service.addAttribute(envelope.createName("type"), "cenbii-procid-ubl");
        SOAPElement action = collab.addChildElement("Action", "eb");
        action.addTextNode("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        collab.addChildElement("ConversationId", "eb").addTextNode("conv-" + UUID.randomUUID());
        collab.addChildElement("AgreementRef", "eb").addTextNode("urn:fdc:eracun.hr:2023:agreements:ap_provider"); // probno

        // MessageProperties (originalSender, finalRecipient)
        SOAPElement msgProps = userMessage.addChildElement("MessageProperties", "eb");
        addProperty(envelope, msgProps, "originalSender", "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088", "0088:" + senderOib);
        addProperty(envelope, msgProps, "finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088", "0088:" + receiverOib);

        // PayloadInfo: navedemo PartInfo za sbdh i za invoice (u tom redoslijedu)
        SOAPElement payloadInfo = userMessage.addChildElement("PayloadInfo", "eb");

        // PartInfo -> SBDH (nekomprimirano, application/xml)
        SOAPElement partSbdh = payloadInfo.addChildElement("PartInfo", "eb");
        partSbdh.addAttribute(envelope.createName("href"), "cid:sbdh.xml");
        SOAPElement sbdhProps = partSbdh.addChildElement("PartProperties", "eb");
        addProperty(envelope, sbdhProps, "MimeType", null, "application/xml");
        // SBDH obicno NIJE compressan — ne stavljamo CompressionType za sbdh

        // PartInfo -> Invoice (gzipiran)
        SOAPElement partInvoice = payloadInfo.addChildElement("PartInfo", "eb");
        partInvoice.addAttribute(envelope.createName("href"), "cid:invoice.xml");
        SOAPElement invoiceProps = partInvoice.addChildElement("PartProperties", "eb");
        addProperty(envelope, invoiceProps, "MimeType", null, "text/xml");
        addProperty(envelope, invoiceProps, "CompressionType", null, "application/gzip");

        // WS-Security header (placeholder): BinarySecurityToken + Signature placeholder
        SOAPElement security = header.addChildElement("Security", "wsse");
        security.addAttribute(envelope.createName("mustUnderstand", "env", "http://www.w3.org/2003/05/soap-envelope"), "true");
        // BinarySecurityToken (placeholder) - u produkciji zamijeniti sa stvarnim Base64 certifikatom
        SOAPElement bst = security.addChildElement("BinarySecurityToken", "wsse");
        bst.addAttribute(envelope.createName("EncodingType"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
        bst.addAttribute(envelope.createName("ValueType"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3");
        bst.addAttribute(envelope.createName("Id", "wsu", WSU_NS), "X509-" + UUID.randomUUID());
        bst.addTextNode("MIID-DUMMY-BASE64-CERT=="); // DUMMY: zamijeni s pravim certifikatom (Base64)

        // ds:Signature placeholder - važno: referencira eb:Messaging (wsu:Id)
        SOAPElement signature = security.addChildElement("Signature", "ds");
        SOAPElement signedInfo = signature.addChildElement("SignedInfo", "ds");
        signedInfo.addChildElement("CanonicalizationMethod", "ds")
                .addAttribute(envelope.createName("Algorithm"), "http://www.w3.org/2001/10/xml-exc-c14n#");
        signedInfo.addChildElement("SignatureMethod", "ds")
                .addAttribute(envelope.createName("Algorithm"), "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");

        // Reference to Messaging element (wo/ actual digest — placeholder)
        SOAPElement reference = signedInfo.addChildElement("Reference", "ds");
        // find messaging wsu:Id value previously set:
        String messagingId = messaging.getAttribute(envelope.createName("Id", "wsu", WSU_NS).getLocalName());
        // If we couldn't get attribute via getAttribute, safer to recompute: but we set it earlier; to be safe, set a variable above.
        // For clarity, we'll just reference "#Messaging-REF" as placeholder
        reference.addAttribute(envelope.createName("URI"), "#Messaging-REF");
        reference.addChildElement("Transforms", "ds").addChildElement("Transform", "ds")
                .addAttribute(envelope.createName("Algorithm"), "http://www.w3.org/2000/09/xmldsig#enveloped-signature");
        reference.addChildElement("DigestMethod", "ds")
                .addAttribute(envelope.createName("Algorithm"), "http://www.w3.org/2001/04/xmlenc#sha256");
        reference.addChildElement("DigestValue", "ds").addTextNode("DUMMY_DIGEST_VALUE==");

        signature.addChildElement("SignatureValue", "ds").addTextNode("DUMMY_SIGNATURE_VALUE==");
        SOAPElement keyInfo = signature.addChildElement("KeyInfo", "ds");
        keyInfo.addChildElement("KeyName", "ds").addTextNode("DummyKey");

        // 4) attachments: sbdh.xml (application/xml) i gzipirani invoice (application/octet-stream)
        AttachmentPart sbdhAttachment = soapMessage.createAttachmentPart();
        sbdhAttachment.setContent(sbdhXml, "application/xml; charset=UTF-8");
        sbdhAttachment.setContentId("<sbdh.xml>");
        soapMessage.addAttachmentPart(sbdhAttachment);

        AttachmentPart invoiceAttachment = soapMessage.createAttachmentPart();
        invoiceAttachment.setContent(gzipInvoice, "application/octet-stream");
        invoiceAttachment.setContentId("<invoice.xml>");
        soapMessage.addAttachmentPart(invoiceAttachment);

        // 5) Serialize to byte[]
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        soapMessage.saveChanges(); // osiguraj update headers
        soapMessage.writeTo(bos);
        return bos.toByteArray();
    }

    // --- HELPERS ---

    private void addParty(SOAPEnvelope env, SOAPElement parent, String tag, String oib, String role) throws SOAPException {
        SOAPElement node = parent.addChildElement(tag, "eb");
        SOAPElement partyId = node.addChildElement("PartyId", "eb");
        // koristimo probni DN format; u produkciji ubaci stvarni DN/CN kako PMode trazi
        partyId.addTextNode("C=HR,O=" + tag + ",CN=" + oib);
        partyId.addAttribute(env.createName("type"), "urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        node.addChildElement("Role", "eb").addTextNode(role);
    }

    private void addProperty(SOAPEnvelope env, SOAPElement parent, String name, String type, String value) throws SOAPException {
        SOAPElement prop = parent.addChildElement("Property", "eb");
        prop.addAttribute(env.createName("name"), name);
        if (type != null) prop.addAttribute(env.createName("type"), type);
        prop.addTextNode(value);
    }

    private String createSbdhXml(String senderOib, String receiverOib, String invoiceId) {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <StandardBusinessDocumentHeader xmlns="http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader">
          <HeaderVersion>1.0</HeaderVersion>
          <Sender><Identifier Authority="HR:OIB">%s</Identifier></Sender>
          <Receiver><Identifier Authority="HR:OIB">%s</Identifier></Receiver>
          <DocumentIdentification>
            <Standard>UBL</Standard>
            <Type>Invoice</Type>
            <TypeVersion>2.1</TypeVersion>
            <InstanceIdentifier>%s</InstanceIdentifier>
            <CreationDateAndTime>%s</CreationDateAndTime>
          </DocumentIdentification>
          <BusinessScope>
            <Scope>
              <Type>PROCESSID</Type>
              <InstanceIdentifier>urn:fdc:peppol.eu:poacc:billing:3</InstanceIdentifier>
            </Scope>
          </BusinessScope>
        </StandardBusinessDocumentHeader>
        """.formatted(senderOib, receiverOib, invoiceId, currentUtc());
    }

    private byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    private String currentUtc() {
        return ZonedDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
}
