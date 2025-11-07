try {
        File outDir = new File(System.getProperty("user.home"), "Desktop/TestSbd");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "sbd_" + documentId + ".xml");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(sbdXml.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println(" SBD uspješno spremljen u: " + outFile.getAbsolutePath());
    } catch (IOException e) {
        e.printStackTrace()
