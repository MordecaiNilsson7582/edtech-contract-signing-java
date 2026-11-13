package infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraiClient {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public InfraiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void createBucket(String bucket) throws IOException, InterruptedException {
        try {
            request("POST", "/v1/storage/bucket/create", "{\"name\":\"" + json(bucket) + "\"}");
        } catch (IOException error) {
            if (!error.getMessage().contains("STORAGE_BUCKET_EXISTS")) throw error;
        }
    }

    public byte[] sign(String pdfBase64, String certPem, String keyPem) throws IOException, InterruptedException {
        // Canonical request boundary: POST /v1/pdf/sign
        String body = "{\"pdf\":\"" + json(pdfBase64) + "\",\"cert_pem\":\"" + json(certPem)
                + "\",\"key_pem\":\"" + json(keyPem) + "\",\"reason\":\"Course agreement\",\"location\":\"Education service\",\"retention_days\":30}";
        String response = request("POST", "/v1/pdf/sign", body);
        String data = value(response, "data_base64");
        if (data.isEmpty()) throw new IOException("Signed PDF response did not include data_base64");
        return Base64.getDecoder().decode(data);
    }

    public String putObject(String bucket, String key, byte[] bytes) throws IOException, InterruptedException {
        String path = "/v1/storage/object/put/" + encode(bucket) + "/" + encode(key);
        String body = "{\"data_base64\":\"" + Base64.getEncoder().encodeToString(bytes) + "\"}";
        return request("PUT", path, body);
    }

    public String presignGet(String bucket, String key) throws IOException, InterruptedException {
        String path = "/v1/storage/object/presign/" + encode(bucket) + "/" + encode(key);
        String body = "{\"op\":\"get\",\"expires_seconds\":900,\"response_disposition\":\"attachment\"}";
        return value(request("POST", path, body), "url");
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!value(envelope, "ok").equals("true")) throw new IOException("Infrai request rejected: " + value(envelope, "code"));
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(200L * (1L << attempt));
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport response: " + response.statusCode());
            return envelope;
        }
        throw new IOException("Infrai request retry budget exhausted");
    }

    private static String value(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start < json.length() && json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end < 0 ? "" : json.substring(start + 1, end);
        }
        int end = start;
        while (end < json.length() && ",}".indexOf(json.charAt(end)) < 0) end++;
        return json.substring(start, end).trim();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n")
                .replace("\b", "\\b").replace("\f", "\\f").replace("\t", "\\t");
    }
    private static String encode(String value) { return value.replace("%", "%25").replace("/", "%2F"); }
}
