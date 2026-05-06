package com.galspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galspace.model.VndbResponse;
import com.galspace.model.VndbVisualNovel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class VndbService {

    private static final String VNDB_API_URL = "https://api.vndb.org/kana/vn";
    private static final String DETAILED_FIELDS =
        "title, alttitle, titles.lang, titles.title, titles.latin, titles.official, titles.main, " +
        "description, developers.id, developers.name, developers.original, " +
        "tags.id, tags.name, tags.category, tags.rating, " +
        "released, rating, votecount, image.url, image.thumbnail, image.thumbnail_dims, " +
        "length, length_minutes, platforms, olang, languages, " +
        "screenshots.id, screenshots.url, screenshots.thumbnail, screenshots.dims";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VndbService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public VndbVisualNovel fetchByVndbId(String vndbId) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filters", List.of("id", "=", vndbId));
        requestBody.put("fields", DETAILED_FIELDS);
        requestBody.put("results", 1);

        VndbResponse response = executeQuery(requestBody);
        if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
            return response.getResults().get(0);
        }
        return null;
    }

    public List<VndbVisualNovel> searchByTitle(String title, int maxResults) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("filters", List.of("search", "=", title));
        requestBody.put("fields", "title, alttitle, released, rating, developers.name, image.thumbnail");
        requestBody.put("results", Math.min(maxResults, 10));
        requestBody.put("sort", "searchrank");

        VndbResponse response = executeQuery(requestBody);
        if (response != null && response.getResults() != null) {
            return response.getResults();
        }
        return Collections.emptyList();
    }

    private VndbResponse executeQuery(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("VNDB query: {}", objectMapper.writeValueAsString(requestBody));

            ResponseEntity<VndbResponse> response = restTemplate.exchange(
                VNDB_API_URL,
                HttpMethod.POST,
                entity,
                VndbResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("VNDB API returned non-success status: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Failed to query VNDB API", e);
            return null;
        }
    }
}
