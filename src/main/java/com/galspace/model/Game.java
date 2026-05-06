package com.galspace.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String id;
    private String title;
    private String titleZh;
    private String gameDir;
    private String exePath;
    private String coverUrl;
    private List<String> tags = new ArrayList<>();
    private List<String> screenshots = new ArrayList<>();
    private String developer;
    private String vndbId;
    private String vndbTitle;
    private String vndbAlttitle;
    private String description;
    private String descriptionZh;
    private List<String> tagsZh = new ArrayList<>();
    private Integer vndbRating;
    private Integer vndbVotecount;
    private String vndbReleased;
    private Integer vndbLength;
    private Integer vndbLengthMinutes;
    private List<String> vndbPlatforms = new ArrayList<>();
    private String vndbOlang;
    private List<String> vndbLanguages = new ArrayList<>();
    private boolean needLocaleEmulator;
    private boolean isBlurred;
    private boolean favorite;
    private List<String> categories = new ArrayList<>();
    private Integer sortOrder;
    private Long addTime;
    private Long lastPlayTime;
}