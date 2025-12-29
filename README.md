public static String normalizeDate(String date) {

    // već ispravno
    if (date.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
        return date;
    }

    String d = date.endsWith("Z")
            ? date.substring(0, date.length() - 1)
            : date;

    // osiguraj T
    if (d.length() > 10 && d.charAt(10) == ' ') {
        d = d.replace(' ', 'T');
    }

    // cilj: yyyy-MM-ddTHH:mm:ss  → 19 znakova
    String target = "yyyy-MM-ddTHH:mm:ss";

    d = (d + "T00:00:00").substring(0, 19);

    return d + "Z";
}
