package com.galspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galspace.model.DeepSeekRequest;
import com.galspace.model.DeepSeekResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL = "deepseek-v4-flash";

    private final ConfigService configService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TranslationResult translateGameInfo(String description, List<String> tags) {
        String apiKey = configService.getConfig().getDeepseekApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek API key not configured, skipping translation");
            return createEmptyResult();
        }

        try {
            Map<String, Object> input = new LinkedHashMap<>();
            if (description != null && !description.trim().isEmpty()) {
                String cleaned = description.replaceAll("\\[[^\\]]*\\]", "").replaceAll("\\s+", " ").trim();
                if (!cleaned.isEmpty()) {
                    input.put("description", cleaned);
                }
            }
            if (tags != null && !tags.isEmpty()) {
                input.put("tags", tags);
            }

            if (input.isEmpty()) {
                return createEmptyResult();
            }

            String inputJson = objectMapper.writeValueAsString(input);
            String systemPrompt = buildSystemPrompt();
            String userPrompt = "Translate the following JSON values from English/Japanese to Simplified Chinese. " +
                "IMPORTANT: Do NOT translate game titles, developer names, or proper nouns. " +
                "Only translate descriptions and tag names. Return a JSON object with the same structure, " +
                "using '_zh' suffix for translated fields. Keep all original field names unchanged.\n\n" + inputJson;

            DeepSeekRequest request = new DeepSeekRequest();
            request.setModel(MODEL);
            request.setMessages(List.of(
                new DeepSeekRequest.Message("system", systemPrompt),
                new DeepSeekRequest.Message("user", userPrompt)
            ));
            request.setResponseFormat(new DeepSeekRequest.ResponseFormat("json_object"));
            request.setTemperature(0.3);
            request.setMaxTokens(4096);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<DeepSeekRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending translation request to DeepSeek...");

            ResponseEntity<DeepSeekResponse> response = restTemplate.exchange(
                DEEPSEEK_API_URL,
                HttpMethod.POST,
                entity,
                DeepSeekResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                DeepSeekResponse deepSeekResponse = response.getBody();
                if (deepSeekResponse.getChoices() != null && !deepSeekResponse.getChoices().isEmpty()) {
                    String content = deepSeekResponse.getChoices().get(0).getMessage().getContent();
                    return parseTranslationResult(content);
                }
            }
            log.warn("DeepSeek API returned unexpected response");
            return createEmptyResult();
        } catch (Exception e) {
            log.error("Failed to translate via DeepSeek API", e);
            return createEmptyResult();
        }
    }

    private String buildSystemPrompt() {
        return "You are a professional translator specializing in translating visual novel / Galgame " +
            "information from English and Japanese to Simplified Chinese (zh-CN).\n\n" +
            "Rules:\n" +
            "1. NEVER translate game titles, developer names, brand names, or proper nouns - keep them as-is\n" +
            "2. Translate descriptions naturally and accurately, preserving the original meaning and tone\n" +
            "3. For tag names, use commonly accepted Chinese translations in the Galgame community\n" +
            "4. Preserve all JSON structure, only replace the text values with translations\n" +
            "5. Add translated fields with '_zh' suffix (e.g., 'description' -> 'description_zh', 'tags' -> 'tags_zh')\n" +
            "6. If a value is already in Chinese, keep it unchanged in the _zh field\n" +
            "7. Respond ONLY with a valid JSON object, no additional text";
    }

    private TranslationResult parseTranslationResult(String content) {
        TranslationResult result = new TranslationResult();
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root.has("description_zh")) {
                result.setDescriptionZh(root.get("description_zh").asText());
            }
            if (root.has("tags_zh") && root.get("tags_zh").isArray()) {
                List<String> tagsZh = new ArrayList<>();
                for (JsonNode tag : root.get("tags_zh")) {
                    tagsZh.add(tag.asText());
                }
                result.setTagsZh(tagsZh);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse DeepSeek translation response: {}", content, e);
        }
        return result;
    }

    private TranslationResult createEmptyResult() {
        return new TranslationResult();
    }

    @lombok.Data
    public static class TranslationResult {
        private String descriptionZh;
        private List<String> tagsZh = Collections.emptyList();
    }
}
