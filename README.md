import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;

// negdje u metodi, random…
byte[] xmlBytes = pXmlContent; // ili kako god već imaš

String xml = new String(xmlBytes, StandardCharsets.UTF_8);

String endpointUrl = StringUtils.substringBetween(
        xml,
        "<EndpointURL>",
        "</EndpointURL>"
).trim();
