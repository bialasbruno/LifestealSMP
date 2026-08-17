package dev.lifesteal.scoreboard.placeholder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pre-parsed percent-placeholder template that avoids regular-expression work during refreshes. */
public final class PlaceholderTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([A-Za-z0-9_]+)%");

    private final String raw;
    private final List<Part> parts;
    private final boolean dynamic;

    private PlaceholderTemplate(String raw, List<Part> parts) {
        this.raw = raw;
        this.parts = parts;
        this.dynamic = parts.stream().anyMatch(Part::placeholder);
    }

    public static PlaceholderTemplate compile(String raw) {
        Objects.requireNonNull(raw, "raw");
        Matcher matcher = PLACEHOLDER.matcher(raw);
        List<Part> parts = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                parts.add(Part.literal(raw.substring(cursor, matcher.start())));
            }
            parts.add(Part.placeholder(matcher.group(), matcher.group(1)));
            cursor = matcher.end();
        }
        if (cursor < raw.length()) {
            parts.add(Part.literal(raw.substring(cursor)));
        }
        return new PlaceholderTemplate(raw, List.copyOf(parts));
    }

    public String raw() {
        return raw;
    }

    public boolean dynamic() {
        return dynamic;
    }

    public String render(Function<String, String> resolver) {
        Objects.requireNonNull(resolver, "resolver");
        if (!dynamic()) {
            return raw;
        }

        StringBuilder output = new StringBuilder(raw.length() + 16);
        for (Part part : parts) {
            if (!part.placeholder()) {
                output.append(part.source());
                continue;
            }
            String replacement = resolver.apply(part.key());
            output.append(replacement != null ? replacement : part.source());
        }
        return output.toString();
    }

    private record Part(String source, String key, boolean placeholder) {

        private static Part literal(String source) {
            return new Part(source, null, false);
        }

        private static Part placeholder(String source, String key) {
            return new Part(source, key, true);
        }
    }
}
