import java.nio.file.Files;
import java.nio.file.Paths;

byte[] test = Files.readAllBytes(
        Paths.get(System.getProperty("user.home") + "/Desktop/test.xml")
);
