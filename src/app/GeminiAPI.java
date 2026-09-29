package app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/** All Gemini communication lives here; agents only call generate() / generateJson(). */

public final class GeminiAPI {
	
	
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final List<String> MODELS = Stream
            .concat(Stream.of(Config.GEMINI_MODEL), Arrays.stream(Config.GEMINI_FALLBACK_MODELS))
            .toList();

    // Global throttle shared by every agent thread, so the RPM limit holds for the whole app.
    private static final Object LOCK = new Object();
    private static long lastCallMillis = 0;

    private GeminiAPI() {}

    /** Free-form text answer. */
    public static String generate(String prompt) throws Exception {
        return ask(prompt, false);
    }

    /** Answer forced to be a JSON document. Code fences are stripped in case the model adds them anyway. */
    public static String generateJson(String prompt) throws Exception {
        return ask(prompt, true).replaceAll("^```(?:json)?\\s*|\\s*```$", "").trim();
    }

    private static String ask(String prompt, boolean json) throws Exception {
    	
        String key = Config.GEMINI_API_KEY;
        
        if (key == null || key.isBlank() || key.startsWith("PASTE_")) {
            throw new IllegalStateException("Gemini API key not set (Config.GEMINI_API_KEY or GEMINI_API_KEY env var)");
        }

        Exception lastError = null;
        
        for (String model : MODELS) {
            throttle(); // every attempt counts against the RPM, whichever model it targets
            try {
                return callModel(model, prompt, json);
            } catch (ModelUnavailableException e) {
                lastError = e; // move on to the next model
            }
        }
        
        throw new RuntimeException("All Gemini models unavailable (last error: " + lastError + ")");
    }

    private static void throttle() throws InterruptedException {
        synchronized (LOCK) {
            long wait = lastCallMillis + Config.GEMINI_MIN_INTERVAL_MS - System.currentTimeMillis();
            if (wait > 0) Thread.sleep(wait);
            lastCallMillis = System.currentTimeMillis();
        }
    }

    private static String callModel(String model, String prompt, boolean json) throws Exception {
        JSONObject content = new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));
        if (json) {
            body.put("generationConfig", new JSONObject().put("responseMimeType", "application/json"));
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL.formatted(model)))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", Config.GEMINI_API_KEY) // header instead of ?key= keeps the key out of logs
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        int status = resp.statusCode();
        if (status == 503 || status == 429) throw new ModelUnavailableException(model + " returned " + status);
        if (status != 200) throw new RuntimeException("Gemini error " + status + ": " + resp.body());
        return extractText(resp.body());
    }

    /** Joins the text parts of the first candidate; fails with a readable message if the answer was blocked/empty. */
    private static String extractText(String responseBody) {
    	
        JSONArray candidates = new JSONObject(responseBody).optJSONArray("candidates");
        JSONObject content = (candidates == null || candidates.length() == 0)
                ? null : candidates.getJSONObject(0).optJSONObject("content");
        if (content == null) {
            throw new RuntimeException("Gemini returned no answer (prompt blocked or empty): " + responseBody);
        }
        StringBuilder text = new StringBuilder();
        for (Object part : content.getJSONArray("parts")) {
            text.append(((JSONObject) part).optString("text"));
        }
        return text.toString().trim();
        
    }

    /** Marker: a 503/429 means "try the next model", not a hard failure. */
    private static class ModelUnavailableException extends Exception {
        ModelUnavailableException(String msg) { super(msg); }
    }
}
