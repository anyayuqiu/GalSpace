package com.galspace.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    private Double temperature;
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseFormat {
        private String type;
    }
}
