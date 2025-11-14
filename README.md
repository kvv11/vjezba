String xml = new String(xmlBytes, StandardCharsets.UTF_8);

String participant = xml.split("<ParticipantIdentifier")[1]
                        .split("</ParticipantIdentifier>")[0];

// Maknemo sve prije '>' jer zbog atributa ide ovako:
// <ParticipantIdentifier scheme="...">OVO_MI_TREBA</ParticipantIdentifier>
participant = participant.substring(participant.indexOf('>') + 1).trim();
