package pt.rentporto.notification;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * The emails Mailpit received, read through its HTTP API.
 */
public class Mailbox {

    private final String api;
    private final HttpClient http = HttpClient.newHttpClient();

    Mailbox(String api) {
        this.api = api;
    }

    /**
     * The emails sent to an address.
     *
     * @param address the address
     * @return each email, most recent first
     */
    public List<Email> to(String address) {
        List<Map<String, Object>> messages = JsonPath.read(get("/api/v1/messages?limit=500"),
                "$.messages[?(@.To[0].Address == '" + address + "')]");
        return messages.stream()
                .map(message -> new Email(
                        (String) message.get("MessageID"),
                        (String) message.get("Subject"),
                        JsonPath.read(get("/api/v1/message/" + message.get("ID")), "$.Text")))
                .toList();
    }

    private String get(String path) {
        try {
            return http.send(HttpRequest.newBuilder(URI.create(api + path)).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Email received.
     *
     * @param messageId value of the {@code Message-ID} header, without the angle brackets
     * @param subject   subject
     * @param text      plain text
     */
    public record Email(String messageId, String subject, String text) {
    }
}
