String xml = new String(xmlBytes, StandardCharsets.UTF_8);

String endpointUrl = xml.split("<EndpointURL>")[1]
                         .split("</EndpointURL>")[0]
                         .trim();
