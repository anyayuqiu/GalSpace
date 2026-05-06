package com.galspace.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameInfoResult {
    private String vndbId;
    private String title;
    private String alttitle;
    private String description;
    private String descriptionZh;
    private String developer;
    private String developerOriginal;
    private List<String> tags;
    private List<String> tagsZh;
    private String released;
    private Integer rating;
    private Integer votecount;
    private Integer length;
    private Integer lengthMinutes;
    private String coverUrl;
    private List<String> screenshotUrls;
    private List<String> platforms;
    private String olang;
    private List<String> languages;
}
