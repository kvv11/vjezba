import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SbdService {

    public static byte[] createSbd(String senderOib,
                                   String receiverOib,
                                   String documentId,
                                   String signedUblXml,
                                   String documentType) {

        String processId = "urn:fdc:peppol.eu:poacc:billing:3";

        String sbdXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<StandardBusinessDocument xmlns=\"http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader\">\n" +
                "  <StandardBusinessDocumentHeader>\n" +
                "    <HeaderVersion>1.0</HeaderVersion>\n" +
                "    <Sender>\n" +
                "      <Identifier Authority=\"HR:OIB\">" + senderOib + "</Identifier>\n" +
                "    </Sender>\n" +
                "    <Receiver>\n" +
                "      <Identifier Authority=\"HR:OIB\">" + receiverOib + "</Identifier>\n" +
                "    </Receiver>\n" +
                "    <DocumentIdentification>\n" +
                "      <Standard>UBL</Standard>\n" +
                "      <Type>" + documentType + "</Type>\n" +
                "      <TypeVersion>2.1</TypeVersion>\n" +
                "      <InstanceIdentifier>" + documentId + "</InstanceIdentifier>\n" +
                "      <CreationDateAndTime>" +
                        ZonedDateTime.now(java.time.ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_INSTANT) +
                "</CreationDateAndTime>\n" +
                "    </DocumentIdentification>\n" +
                "    <BusinessScope>\n" +
                "      <Scope>\n" +
                "        <Type>PROCESSID</Type>\n" +
                "        <InstanceIdentifier>" + processId + "</InstanceIdentifier>\n" +
                "      </Scope>\n" +
                "    </BusinessScope>\n" +
                "  </StandardBusinessDocumentHeader>\n" +
                signedUblXml.trim() + "\n" +
                "</StandardBusinessDocument>";

        return sbdXml.getBytes(StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        String senderOib = "12345678901";
        String receiverOib = "10987654321";
        String documentId = "INV-2025-001";
        String signedUbl = 
                "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">" +
                "<cbc:ID>INV-2025-001</cbc:ID>" +
                "<cbc:IssueDate>2025-11-04</cbc:IssueDate>" +
                "</Invoice>";

        byte[] sbdBytes = createSbd(senderOib, receiverOib, documentId, signedUbl, "Invoice");

        System.out.println(new String(sbdBytes, StandardCharsets.UTF_8));
    }
}
