package net.collapse.antiaddiction.util;

public class TimeParser {
    public static long parseToTicks(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty time string");
        }
        input = input.trim().toLowerCase();
        if (input.matches("\\d+")) {
            return Long.parseLong(input);
        }
        long totalMs = 0;
        int i = 0;
        while (i < input.length()) {
            int start = i;
            while (i < input.length() && (Character.isDigit(input.charAt(i)) || input.charAt(i) == '.')) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("Expected number at position " + start + " in: " + input);
            }
            double value = Double.parseDouble(input.substring(start, i));

            if (i >= input.length()) {
                throw new IllegalArgumentException("Missing unit after number in: " + input);
            }

            long multiplier;
            if (input.startsWith("min", i)) {
                multiplier = 60000;
                i += 3;
            } else {
                char unit = input.charAt(i);
                i++;
                switch (unit) {
                    case 't':
                        multiplier = 50;
                        break;
                    case 's':
                        multiplier = 1000;
                        break;
                    case 'h':
                        multiplier = 3600000;
                        break;
                    case 'd':
                        multiplier = 86400000;
                        break;
                    case 'm':
                        multiplier = 30L * 86400000;
                        break;
                    case 'y':
                        multiplier = 365L * 86400000;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown unit: " + unit);
                }
            }
            totalMs += (long) (value * multiplier);
        }
        return totalMs / 50;
    }
}
