package com.betterbosshealthbar;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Parses the "Boss Name: 75, 50, 25" breakpoint config into a lookup of
 * lowercase boss name to ascending breakpoint fractions (0-1, exclusive).
 */
final class Breakpoints {
    static final float[] NONE = new float[0];

    private Breakpoints() {
    }

    static Map<String, float[]> parse(String config) {
        if (config == null || config.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, float[]> result = new HashMap<>();
        for (String rawLine : config.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int sep = line.lastIndexOf(':');
            if (sep < 0) {
                sep = line.lastIndexOf('=');
            }
            if (sep <= 0) {
                continue;
            }

            String name = normalizeName(line.substring(0, sep));
            if (name.isEmpty()) {
                continue;
            }

            TreeSet<Float> values = new TreeSet<>();
            for (String token : line.substring(sep + 1).split("[,;\\s]+")) {
                String number = token.replace("%", "").trim();
                if (number.isEmpty()) {
                    continue;
                }
                try {
                    float percent = Float.parseFloat(number);
                    if (percent > 0 && percent < 100) {
                        values.add(percent / 100f);
                    }
                } catch (NumberFormatException ignored) {
                    // skip invalid entries
                }
            }

            if (!values.isEmpty()) {
                float[] fractions = new float[values.size()];
                int i = 0;
                for (float value : values) {
                    fractions[i++] = value;
                }
                result.put(name, fractions);
            }
        }
        return result;
    }

    static String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
