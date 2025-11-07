String ublContent = new String(ublBytes, StandardCharsets.UTF_8);
ublContent = ublContent.replaceFirst("^<\\?xml.*?\\?>", "").trim();
