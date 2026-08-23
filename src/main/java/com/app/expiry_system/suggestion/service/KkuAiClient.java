package com.app.expiry_system.suggestion.service;

import com.app.expiry_system.suggestion.dto.SuggestedMenuResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KkuAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public KkuAiClient(@Value("${app.ai.kku.base-url}") String baseUrl,
                       @Value("${app.ai.kku.api-key}") String apiKey,
                       @Value("${app.ai.kku.model}") String model) {
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<SuggestedMenuResponse> suggestMenus(String systemPrompt, String userPrompt) {
        try {
            return parseMenus(complete(systemPrompt, userPrompt));
        } catch (Exception exception) {
            throw new IllegalArgumentException("KKU AI request failed: " + exception.getMessage());
        }
    }

    public String completeJson(String systemPrompt, String userPrompt) {
        try {
            return cleanJsonContent(extractMessageContent(complete(systemPrompt, userPrompt)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("KKU AI request failed: " + exception.getMessage());
        }
    }

    private String complete(String systemPrompt, String userPrompt) {
        ensureConfigured();

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "stream", false
        );

        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return response;
        } catch (RestClientResponseException exception) {
            throw new IllegalArgumentException("KKU AI request failed: " + exception.getStatusCode()
                    + " " + exception.getResponseBodyAsString());
        }
    }

    private List<SuggestedMenuResponse> parseMenus(String chatCompletionResponse) throws Exception {
        String content = cleanJsonContent(extractMessageContent(chatCompletionResponse));
        JsonNode menuRoot = objectMapper.readTree(content);

        JsonNode menusNode = menuRoot.isArray() ? menuRoot : menuRoot.path("menus");
        if (!menusNode.isArray()) {
            throw new IllegalArgumentException("KKU AI response did not contain a menus array");
        }

        List<SuggestedMenuResponse> menus = objectMapper.readerForListOf(SuggestedMenuResponse.class).readValue(menusNode);
        if (menus.isEmpty()) {
            throw new IllegalArgumentException("KKU AI returned an empty menus array");
        }
        return menus;
    }

    private String extractMessageContent(String chatCompletionResponse) throws Exception {
        JsonNode root = objectMapper.readTree(chatCompletionResponse);
        JsonNode errorMessageNode = root.path("error").path("message");
        if (errorMessageNode.isTextual() && !errorMessageNode.asText().isBlank()) {
            throw new IllegalArgumentException("KKU AI returned an error: " + errorMessageNode.asText());
        }

        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        String rawContent = extractContent(contentNode);
        if (rawContent.isBlank()) {
            throw new IllegalArgumentException("KKU AI response did not contain message content. Response preview: "
                    + preview(chatCompletionResponse));
        }
        return rawContent;
    }

    private String extractContent(JsonNode contentNode) {
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : contentNode) {
                JsonNode textNode = part.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText());
                } else if (part.isTextual()) {
                    builder.append(part.asText());
                }
            }
            return builder.toString();
        }
        return contentNode.asText("");
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        int maxLength = 500;
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength) + "...";
    }

    private String cleanJsonContent(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        trimmed = trimmed.trim();

        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        if (objectStart < 0 && arrayStart < 0) {
            return trimmed;
        }

        boolean startsWithArray = arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart);
        if (startsWithArray) {
            int arrayEnd = trimmed.lastIndexOf(']');
            return arrayEnd > arrayStart ? trimmed.substring(arrayStart, arrayEnd + 1) : trimmed;
        }

        int objectEnd = trimmed.lastIndexOf('}');
        return objectEnd > objectStart ? trimmed.substring(objectStart, objectEnd + 1) : trimmed;
    }

    private void ensureConfigured() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("replace-with-your-kku-api-key")) {
            throw new IllegalArgumentException("KKU AI API key is not configured");
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://gen.ai.kku.ac.th/api/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
