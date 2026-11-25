package com.ia.ia_base.util;

import com.ia.ia_base.models.Category;
import com.ia.ia_base.models.Event;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches events from external APIs and converts them to Event objects.
 *
 * How to add your own API (IB IA hint):
 *   1. Add API_X_URL and API_X_KEY to your .env file.
 *   2. Change parseEvents() to match your API's JSON structure.
 *
 * No external JSON library is used here — JSON is parsed with
 * java.util.regex which is part of the standard Java library.
 */
public class ApiClient {

    // -----------------------------------------------------------------------
    // ApiEndpoint — a simple data record for one configured API source.
    // A Java record is like a class but with getters generated automatically.
    // -----------------------------------------------------------------------
    public record ApiEndpoint(String name, String url, String key) {}

    // -----------------------------------------------------------------------
    // loadEndpoints() reads API_1_URL, API_2_URL, … from the .env file.
    // Returns an empty list if no endpoints are configured.
    // -----------------------------------------------------------------------
    public static List<ApiEndpoint> loadEndpoints() {
        Properties props = loadEnvFile();
        List<ApiEndpoint> endpoints = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            String url = props.getProperty("API_" + i + "_URL");
            if (url == null || url.isBlank()) break; // stop at first missing number

            String key  = props.getProperty("API_" + i + "_KEY",  "");
            String name = props.getProperty("API_" + i + "_NAME", "API " + i);
            endpoints.add(new ApiEndpoint(name.trim(), url.trim(), key.trim()));
        }

        return endpoints;
    }

    // -----------------------------------------------------------------------
    // fetchJson() sends an HTTP GET request and returns the response body.
    // Uses java.net.http.HttpClient — built into Java 11+, no extra library.
    // If apiKey is set, it is sent as a Bearer token in the Authorization header.
    // -----------------------------------------------------------------------
    public static String fetchJson(String url, String apiKey) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();

        // Some APIs require a key in the Authorization header
        if (apiKey != null && !apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = client.send(
            builder.build(),
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException("Server returned HTTP " + response.statusCode());
        }

        return response.body();
    }

    // -----------------------------------------------------------------------
    // parseEvents() converts a comealone-style JSON array into Event objects.
    //
    // JSON fields used:
    //   "title"       → event title
    //   "description" → event description
    //   "start"       → ISO 8601 datetime  e.g. "2026-12-07T18:00:00+02:00"
    //   "category"    → category name matched against our DB categories
    //
    // IB hint: if your API has a different structure, update this method.
    // -----------------------------------------------------------------------
    public static List<Event> parseEvents(String json, List<Category> categories) {
        List<Event> events = new ArrayList<>();
        int userId = SessionManager.getInstance().getCurrentUser().getId();

        // If the response is a wrapped object (e.g. {"events":[...]}),
        // extract the array first. If it starts with '[', use it directly.
        String arrayJson = json.stripLeading().startsWith("[")
            ? json
            : extractJsonArray(json, "events");

        // Split the JSON array into individual object strings
        List<String> objects = splitJsonObjects(arrayJson);

        for (String obj : objects) {
            Event event = new Event();
            event.setTitle(getString(obj, "title", "Untitled"));
            event.setDescription(buildDescription(obj));
            event.setUserId(userId);

            // Parse the "start" field — ISO 8601 datetime → LocalDate
            String start = getString(obj, "start", null);
            if (start != null && !start.isEmpty()) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(start, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    event.setDate(odt.toLocalDate());
                } catch (Exception ignored) {
                    // leave date as null if the format is unexpected
                }
            }

            // Match the API category name to one of our DB categories
            String apiCategory = getString(obj, "category", "");
            event.setCategory(matchCategory(apiCategory, categories));

            events.add(event);
        }

        return events;
    }

    // -----------------------------------------------------------------------
    // JSON helpers — no external library needed
    // -----------------------------------------------------------------------

    /**
     * Splits a JSON array string into individual top-level object strings.
     * Tracks brace depth and whether we are inside a string value,
     * so braces inside string values do not break the split.
     */
    private static List<String> splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '\\' && inString) {
                i++; // skip the escaped character (e.g. \" or \\)
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        objects.add(json.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }

        return objects;
    }

    /**
     * Finds a JSON array value by key inside a JSON object string.
     * e.g. extractJsonArray(response, "events") returns the "[...]" part.
     * Returns "[]" if the key is not found.
     */
    private static String extractJsonArray(String json, String key) {
        int keyPos = json.indexOf("\"" + key + "\"");
        if (keyPos < 0) return "[]";

        int bracketStart = json.indexOf('[', keyPos);
        if (bracketStart < 0) return "[]";

        int depth = 0;
        boolean inString = false;
        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && inString) { i++; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (!inString) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) return json.substring(bracketStart, i + 1);
                }
            }
        }
        return "[]";
    }

    /**
     * Extracts the value of a string field from a JSON object string.
     * Handles escaped characters inside the value (e.g. \", \\, \n).
     * Returns defaultValue if the field is missing or null.
     */
    private static String getString(String jsonObject, String field, String defaultValue) {
        // Match:  "field" : "value"   (value may contain escaped characters)
        Pattern p = Pattern.compile(
            "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(jsonObject);
        if (!m.find()) return defaultValue;

        // Unescape common JSON escape sequences
        return m.group(1)
            .replace("\\n",  "\n")
            .replace("\\t",  "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }

    /**
     * Builds a readable description from multiple API fields.
     * Adjust this if your API uses different field names.
     */
    private static String buildDescription(String obj) {
        StringBuilder sb = new StringBuilder();

        String desc = getString(obj, "description", "");
        if (!desc.isBlank()) sb.append(desc);

        String venue = getString(obj, "venue", "");
        if (!venue.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Venue: ").append(venue);
        }

        String price = getString(obj, "price", "");
        if (!price.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("Price: ").append(price);
        }

        return sb.toString();
    }

    /**
     * Finds the best matching Category by name (case-insensitive).
     * Falls back to "Other", then to the first category in the list.
     */
    private static Category matchCategory(String name, List<Category> categories) {
        if (categories == null || categories.isEmpty()) return null;

        return categories.stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> categories.stream()
                .filter(c -> c.getName().equalsIgnoreCase("Other"))
                .findFirst()
                .orElse(categories.get(0)));
    }

    // -----------------------------------------------------------------------
    // .env loader
    // -----------------------------------------------------------------------

    /**
     * Reads key=value pairs from the .env file.
     * Lines starting with # are treated as comments and ignored.
     */
    private static Properties loadEnvFile() {
        Properties props = new Properties();
        File envFile = new File(".env");
        if (!envFile.exists()) return props;

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    props.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("ApiClient: could not read .env — " + e.getMessage());
        }

        return props;
    }
}
