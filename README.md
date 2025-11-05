"args.vm.override.string": "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 -Djava.util.logging.config.file=\\\"d:\\\\apache-tomcat-9.0.106-windows-x64\\\\apache-tomcat-9.0.106\\\\conf\\\\logging.properties\\\"",





"args.vm.override.string": "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000 -Djava.util.logging.config.file=\"d:\\\\apache-tomcat-9.0.106-windows-x64\\\\apache-tomcat-9.0.106\\\\conf\\\\logging.properties\"",





import org.json.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.ByteArrayDataSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPOutputStream;


public class As4MessageService {

    // ebMS3 / WS-* konstante
    private static final String EB_NS   = "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";
    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSU_NS  = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String SOAP12  = "http://www.w3.org/2003/05/soap-envelope";


    public byte[] createAndSignAs4Invoice(byte[] ublXmlBytes,
                                          String senderOib,
                                          String receiverOib,
                                          String documentId) throws Exception {

        // gzip xml-a
        byte[] gzipInvoice = gzip(ublXmlBytes);
        
        
        // TODO: za invoice promjeniti type i proccesid, ovo je za test racuna 
        final String sbdhXml = buildSbdhXml(senderOib, receiverOib, documentId,
                "Invoice", "urn:fdc:peppol.eu:poacc:billing:3");

        // soap 1_2 poruka
        MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage soap = mf.createMessage();
        SOAPEnvelope env = soap.getSOAPPart().getEnvelope();
        SOAPHeader head = env.getHeader();

        env.addNamespaceDeclaration("eb",   EB_NS);
        env.addNamespaceDeclaration("wsse", WSSE_NS);
        env.addNamespaceDeclaration("wsu",  WSU_NS);

        // eb:Messaging - provjerit ovo jel ima nekakva formula za uuid ili moze random 
        SOAPElement messaging = head.addChildElement("Messaging", "eb");
        final String messagingId = "Messaging-" + UUID.randomUUID();
        messaging.addAttribute(env.createName("Id", "wsu", WSU_NS), messagingId);
        messaging.addAttribute(env.createName("mustUnderstand", "env", SOAP12), "true");

        // usermessage
        SOAPElement userMsg = messaging.addChildElement("UserMessage", "eb");
        // MPC – defaultMPC (HR profil dopušta default)
        userMsg.addAttribute(env.createName("mpc"),
                "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");

        // messageinfo
        SOAPElement mi = userMsg.addChildElement("MessageInfo", "eb");
        mi.addChildElement("Timestamp", "eb").addTextNode(nowUtc());
        mi.addChildElement("MessageId", "eb").addTextNode("uuid:" + UUID.randomUUID());

        // partyinfo 
        SOAPElement pi = userMsg.addChildElement("PartyInfo", "eb");
        addParty(env, pi, "From", "0088:" + senderOib, EB_NS + "/initiator");
        addParty(env, pi, "To",   "0088:" + receiverOib, EB_NS + "/responder");

        // collaborationinfo
        SOAPElement collab = userMsg.addChildElement("CollaborationInfo", "eb");
        // TODO izmjeniti za service/action dinamicki za invoice/odobrenje
        SOAPElement service = collab.addChildElement("Service", "eb");
        service.addTextNode("cenbii-procid-ubl");
        service.addAttribute(env.createName("type"), "cenbii-procid-ubl");

        SOAPElement action = collab.addChildElement("Action", "eb");
        action.addTextNode("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");

        collab.addChildElement("ConversationId", "eb").addTextNode("conv-" + UUID.randomUUID());
        // (Opcionalno) AgreementRef – prema lokalnom dogovoru/PMode
        // collab.addChildElement("AgreementRef", "eb").addTextNode("urn:fdc:eracun.hr:agreements:ap_provider");

        // mess properties
        SOAPElement props = userMsg.addChildElement("MessageProperties", "eb");
        addProperty(env, props, "originalSender",
                "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088", "0088:" + senderOib);
        addProperty(env, props, "finalRecipient",
                "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088", "0088:" + receiverOib);

        // payload info sbdh i gzipoan fajl
        SOAPElement payload = userMsg.addChildElement("PayloadInfo", "eb");

        // SBDH part – NE komprimiran, application/xml
        SOAPElement partSbdh = payload.addChildElement("PartInfo", "eb");
        partSbdh.addAttribute(env.createName("href"), "cid:sbdh.xml");
        SOAPElement sbdhPP = partSbdh.addChildElement("PartProperties", "eb");
        addSimpleProperty(env, sbdhPP, "MimeType", "application/xml");

        // Invoice part – GZIP, application/octet-stream; PartProperties: MimeType=text/xml + CompressionType=application/gzip
        SOAPElement partInv = payload.addChildElement("PartInfo", "eb");
        partInv.addAttribute(env.createName("href"), "cid:invoice.xml");
        SOAPElement invPP = partInv.addChildElement("PartProperties", "eb");
        addSimpleProperty(env, invPP, "MimeType", "text/xml");
        addSimpleProperty(env, invPP, "CompressionType", "application/gzip");

        // ws security - za potpis - kasnije dodajemo
        SOAPElement wsse = head.addChildElement("Security", "wsse");
        wsse.addAttribute(env.createName("mustUnderstand", "env", SOAP12), "true");

        // attachmenti
        DataSource dsSbdh = new ByteArrayDataSource(sbdhXml.getBytes(StandardCharsets.UTF_8),
                "application/xml; charset=UTF-8");
        AttachmentPart attSbdh = soap.createAttachmentPart(new DataHandler(dsSbdh));
        attSbdh.setContentId("<sbdh.xml>");
        soap.addAttachmentPart(attSbdh);

        DataSource dsInv = new ByteArrayDataSource(gzipInvoice, "application/octet-stream");
        AttachmentPart attInv = soap.createAttachmentPart(new DataHandler(dsInv));
        attInv.setContentId("<invoice.xml>");
        soap.addAttachmentPart(attInv);

        soap.saveChanges();

        // potpisivanje 
        String messagingXml = extractElementXml(messaging);
        // ovo vidjet u potpisivanju xml-a i prepisat tako 
        List<String> allowedCerts = null;     
        String serialNumber       = null;    
        String issuer             = null;     

        GenerateAuthorizationKeyBean authBean =
                SignatureUtils.generateAuthorizationKey(messagingXml, allowedCerts, serialNumber, issuer);

        if (authBean == null || authBean.getErrorId() != null) {
            throw new IllegalStateException("Potpisni servis greška: " +
                    (authBean == null ? "null" : authBean.getErrorMsg()));
        }

        // povlacme potpisa
        String signedXml = SignatureUtils.pullData(authBean.getAuthKey(), authBean.getIdZhtvRq());
        if (signedXml == null || signedXml.trim().isEmpty()) {
            throw new IllegalStateException("Potpisni servis vratio prazan potpis (signedXml).");
        }

        // insert potpisa u wse
        appendSignatureXml(wsse, signedXml);

        soap.saveChanges();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        soap.writeTo(bos);
        byte[] mimeBytes = bos.toByteArray();

        return mimeBytes;
    }
──────────────

    private static void addParty(SOAPEnvelope env, SOAPElement parent, String tag, String iso6523PartyId, String role)
            throws SOAPException {
        SOAPElement node = parent.addChildElement(tag, "eb");
        SOAPElement partyId = node.addChildElement("PartyId", "eb");
        partyId.addTextNode(iso6523PartyId);
        partyId.addAttribute(env.createName("type"), "urn:oasis:names:tc:ebcore:partyid-type:iso6523:0088");
        node.addChildElement("Role", "eb").addTextNode(role);
    }

    private static void addProperty(SOAPEnvelope env, SOAPElement parent, String name, String type, String value)
            throws SOAPException {
        SOAPElement prop = parent.addChildElement("Property", "eb");
        prop.addAttribute(env.createName("name"), name);
        if (type != null) prop.addAttribute(env.createName("type"), type);
        prop.addTextNode(value);
    }

    private static void addSimpleProperty(SOAPEnvelope env, SOAPElement parent, String name, String value)
            throws SOAPException {
        SOAPElement prop = parent.addChildElement("Property", "eb");
        prop.addAttribute(env.createName("name"), name);
        prop.addTextNode(value);
    }

    private static String buildSbdhXml(String senderOib, String receiverOib,
                                       String instanceId, String type, String processId) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<StandardBusinessDocumentHeader xmlns=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\">\n")
          .append("  <HeaderVersion>1.0</HeaderVersion>\n")
          .append("  <Sender><Identifier Authority=\"HR:OIB\">").append(senderOib).append("</Identifier></Sender>\n")
          .append("  <Receiver><Identifier Authority=\"HR:OIB\">").append(receiverOib).append("</Identifier></Receiver>\n")
          .append("  <DocumentIdentification>\n")
          .append("    <Standard>UBL</Standard>\n")
          .append("    <Type>").append(type).append("</Type>\n")
          .append("    <TypeVersion>2.1</TypeVersion>\n")
          .append("    <InstanceIdentifier>").append(instanceId).append("</InstanceIdentifier>\n")
          .append("    <CreationDateAndTime>").append(nowUtc()).append("</CreationDateAndTime>\n")
          .append("  </DocumentIdentification>\n")
          .append("  <BusinessScope>\n")
          .append("    <Scope>\n")
          .append("      <Type>PROCESSID</Type>\n")
          .append("      <InstanceIdentifier>").append(processId).append("</InstanceIdentifier>\n")
          .append("    </Scope>\n")
          .append("  </BusinessScope>\n")
          .append("</StandardBusinessDocumentHeader>\n");
        return sb.toString();
    }

    private static String nowUtc() {
        return ZonedDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    private static String extractElementXml(SOAPElement element) throws Exception {
        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        tr.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter sw = new StringWriter();
        tr.transform(new DOMSource(element), new StreamResult(sw));
        return sw.toString();
    }

    private static void appendSignatureXml(SOAPElement wsseSecurity, String signatureXml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document sigDoc = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(signatureXml.getBytes(StandardCharsets.UTF_8)));
        Node imported = wsseSecurity.getOwnerDocument().importNode(sigDoc.getDocumentElement(), true);
        wsseSecurity.appendChild(imported);
    }

    private static void writeToDesktop(byte[] content, String fileName) {
        try {
            File outDir = new File(System.getProperty("user.home"), "Desktop/TestAs4");
            if (!outDir.exists()) outDir.mkdirs();
            File f = new File(outDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(content);
            }
            System.out.println("✅ AS4 zapisano: " + f.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // sender 
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SendAs4Service {

    /**
     * Šalje AS4 MIME poruku (byte[]) prema Domibus endpointu putem HTTP POST zahtjeva.
     *
     * @param as4MessageBytes  byte[] koji je generiran iz As4MessageService.createAndSignAs4Invoice(...)
     * @param endpointUrl      URL Domibus endpointa (npr. https://test.domibus.fina.hr/domibus/services/msh)
     * @return                 String odgovor (obično AS4 Receipt XML)
     */
    public String sendToDomibus(byte[] as4MessageBytes, String endpointUrl) throws IOException {

        // TODO: Ako koristiš HTTPS, obavezno prije poziva postavi keystore/truststore:
        // System.setProperty("javax.net.ssl.keyStore", "path/to/keystore.jks");
        // System.setProperty("javax.net.ssl.keyStorePassword", "lozinka");
        // System.setProperty("javax.net.ssl.trustStore", "path/to/truststore.jks");
        // System.setProperty("javax.net.ssl.trustStorePassword", "lozinka");

        System.out.println("📤 Slanje AS4 poruke na endpoint: " + endpointUrl);

        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        // Ključni headeri koje Domibus očekuje
        conn.setRequestProperty("Content-Type",
                "multipart/related; type=\"application/soap+xml\"; boundary=\"----=_Part_0\"");
        conn.setRequestProperty("SOAPAction", "");
        conn.setRequestProperty("User-Agent", "Java-AS4-Client/1.0");
        conn.setRequestProperty("Connection", "Keep-Alive");

        // Zapisujemo bajtove poruke u tijelo zahtjeva
        try (OutputStream os = conn.getOutputStream()) {
            os.write(as4MessageBytes);
            os.flush();
        }

        // Provjera odgovora
        int status = conn.getResponseCode();
        System.out.println("📨 HTTP status: " + status);

        InputStream responseStream = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        String response = readStream(responseStream);

        // Ispiši AS4 ACK XML ako postoji
        System.out.println("🔁 Odgovor Domibusa:\n" + response);

        conn.disconnect();
        return response;
    }

    // Pomoćna metoda za čitanje HTTP streama u string
    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
public class TestAs4Send {
    public static void main(String[] args) throws Exception {

        As4MessageService as4Service = new As4MessageService();

        // TODO: zamijeni stvarnim podacima
        byte[] xmlBytes = "<Invoice>...</Invoice>".getBytes(StandardCharsets.UTF_8);
        String senderOib = "12345678901";
        String receiverOib = "10987654321";
        String docId = "INV-2025-0001";

        byte[] as4Message = as4Service.createAndSignAs4Invoice(xmlBytes, senderOib, receiverOib, docId);

        SendAs4Service sender = new SendAs4Service();

        // TODO: promijeni URL u tvoj Domibus endpoint
        String endpoint = "http://localhost:8080/domibus/services/msh";

        sender.sendToDomibus(as4Message, endpoint);
    }
}

{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "tomcat",
      "name": "Debug Tomcat Server (Local WAR)",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "server": "Tomcat 9.0", 
      "webApp": "${workspaceFolder}/WEBBACK/target/ime-tvoje-aplikacije.war",
      "port": 8080,
      "vmArgs": "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:50554",
      "stopOnEntry": false
    }
  ]
}

123
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Attach to Tomcat",
      "request": "attach",
      "hostName": "localhost",
      "port": 8000
    }
  ]
}


{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "communityServerConnector",
      "name": "Run Tomcat Server (Debug Mode)",
      "request": "launch",
      "serverId": "tomcat90x",
      "debugPort": 8000,
      "timeout": 120000
    }
  ]
}
