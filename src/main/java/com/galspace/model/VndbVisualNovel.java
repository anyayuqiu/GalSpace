package com.galspace.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VndbVisualNovel {
    private String id;
    private String title;
    private String alttitle;
    private List<Title> titles;
    private String description;
    private List<Developer> developers;
    private List<Tag> tags;
    private String released;
    private Integer rating;
    private Integer votecount;
    private Image image;
    private Integer length;
    @JsonProperty("length_minutes")
    private Integer lengthMinutes;
    private List<String> platforms;
    private String olang;
    private List<String> languages;
    private List<Screenshot> screenshots;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Title {
        private String lang;
        private String title;
        private String latin;
        private boolean official;
        private boolean main;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Developer {
        private String id;
        private String name;
        private String original;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tag {
        private String id;
        private String name;
        private String category;
        private Double rating;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        private String id;
        private String url;
        private String thumbnail;
        @JsonProperty("thumbnail_dims")
        private List<Integer> thumbnailDims;
        private List<Integer> dims;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Screenshot {
        private String id;
        private String url;
        private String thumbnail;
        private List<Integer> dims;
        @JsonProperty("thumbnail_dims")
        private List<Integer> thumbnailDims;
    }
}
