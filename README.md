import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    private static final DateTimeFormatter OUTPUT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC);

    public static String normalizeToSecondsUTC(String input) {

        // već je u ispravnom formatu sa sekundama i Z
        if (input.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
            return input;
        }

        // bez sekundi (yyyy-MM-ddTHH:mm)
        LocalDateTime ldt = LocalDateTime.parse(input);
        return OUTPUT.format(ldt.toInstant(ZoneOffset.UTC));
    }
}
