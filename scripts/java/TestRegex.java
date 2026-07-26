import java.util.regex.*;

public class TestRegex {
    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("([\\d\\.,]+)(?:\\s*/\\s*([\\d\\.,]+)|%)?");
        String[] tests = {"8.8/10", "4/5", "87%"};
        for (String s : tests) {
            Matcher m = pattern.matcher(s);
            if (m.find()) {
                System.out.println("String: " + s + ", group 0: " + m.group(0) + ", group 1: " + m.group(1) + ", group 2: " + m.group(2));
                if (m.group(0).contains("%")) {
                    System.out.println("  Contains %");
                }
            } else {
                System.out.println("String: " + s + " NOT FOUND");
            }
        }
    }
}
