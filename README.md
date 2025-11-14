String xml = new String(xmlBytes, StandardCharsets.UTF_8);

String participant = xml.split("<ParticipantIdentifier")[1]    // uzmi sve nakon otvarajućeg taga + atributa
                        .split("</ParticipantIdentifier>")[0]; // odreži do zatvarajućeg taga

participant = participant.substring(participant.indexOf('>') + 1).trim();

byte[] xmlBytes = ...;

DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
factory.setNamespaceAware(true);
DocumentBuilder builder = factory.newDocumentBuilder();

Document doc = builder.parse(new ByteArrayInputStream(xmlBytes));

XPath xp = XPathFactory.newInstance().newXPath();

String accessPointOIB = xp.evaluate(
        "/*[local-name()='SignedServiceMetaData']" +
        "/*[local-name()='ServiceMetadata']" +
        "/*[local-name()='AccessPointOIB']/text()",
        doc
);

aa

Document doc = null;
try {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    doc = builder.parse(new ByteArrayInputStream(xmlBytes));
} catch (Exception e) {
    throw new RuntimeException(e);
}
