package com.galspace.service;

import com.galspace.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameInfoService {

    private final VndbService vndbService;
    private final DeepSeekService deepSeekService;
    private final JsonStorageService jsonStorageService;

    public GameInfoResult fetchAndTranslate(String vndbId) {
        VndbVisualNovel vn = vndbService.fetchByVndbId(vndbId);
        if (vn == null) {
            log.warn("No VNDB result for id: {}", vndbId);
            return null;
        }
        return buildGameInfoResult(vn);
    }

    public GameInfoResult enrichGame(String gameId) {
        Game game = jsonStorageService.getGame(gameId);
        if (game == null) {
            log.warn("Game not found: {}", gameId);
            return null;
        }
        String vndbId = game.getVndbId();
        if (vndbId == null || vndbId.trim().isEmpty()) {
            log.warn("Game {} has no vndbId set", gameId);
            return null;
        }

        GameInfoResult result = fetchAndTranslate(vndbId);
        if (result != null) {
            applyToGame(game, result);
            jsonStorageService.updateGame(game);
            log.info("Enriched game {} with VNDB data", game.getTitle());
        }
        return result;
    }

    public GameInfoResult enrichGamePreview(String gameId) {
        Game game = jsonStorageService.getGame(gameId);
        if (game == null) {
            log.warn("Game not found: {}", gameId);
            return null;
        }
        String vndbId = game.getVndbId();
        if (vndbId == null || vndbId.trim().isEmpty()) {
            log.warn("Game {} has no vndbId set", gameId);
            return null;
        }
        return fetchAndTranslate(vndbId);
    }

    public List<VndbVisualNovel> searchVndb(String keyword) {
        return vndbService.searchByTitle(keyword, 10);
    }

    public void applyToGame(Game game, GameInfoResult info) {
        applyToGameFields(game, info);
    }

    public void applyToGameNoScreenshots(Game game, GameInfoResult info) {
        applyToGameFields(game, info);
    }

    public void downloadScreenshotsForGames(List<Game> games) {
        for (Game game : games) {
            if (game.getVndbId() == null) continue;
            if (game.getScreenshots() != null && !game.getScreenshots().isEmpty()) continue;
            VndbVisualNovel vn = vndbService.fetchByVndbId(game.getVndbId());
            if (vn == null || vn.getScreenshots() == null || vn.getScreenshots().isEmpty()) continue;

            List<String> urls = vn.getScreenshots().stream()
                .map(VndbVisualNovel.Screenshot::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if (!urls.isEmpty()) {
                List<String> localPaths = downloadScreenshots(game.getId(), urls);
                game.setScreenshots(localPaths);
            }
        }
    }

    private void applyToGameFields(Game game, GameInfoResult info) {
        game.setVndbId(info.getVndbId());
        if (info.getTitle() != null) {
            game.setVndbTitle(info.getTitle());
        }
        if (info.getAlttitle() != null) {
            game.setVndbAlttitle(info.getAlttitle());
            game.setTitle(info.getAlttitle());
        }
        if (info.getDescription() != null) {
            game.setDescription(info.getDescription());
        }
        if (info.getDescriptionZh() != null) {
            game.setDescriptionZh(info.getDescriptionZh());
        }
        if (info.getDeveloper() != null) {
            game.setDeveloper(info.getDeveloper());
        }
        if (info.getTags() != null) {
            game.setTags(info.getTags());
        }
        if (info.getTagsZh() != null && !info.getTagsZh().isEmpty()) {
            game.setTagsZh(info.getTagsZh());
        }
        if (info.getCoverUrl() != null && game.getCoverUrl() == null) {
            game.setCoverUrl(info.getCoverUrl());
        }
        if (info.getReleased() != null) {
            game.setVndbReleased(info.getReleased());
        }
        if (info.getRating() != null) {
            game.setVndbRating(info.getRating());
        }
        if (info.getVotecount() != null) {
            game.setVndbVotecount(info.getVotecount());
        }
        if (info.getLength() != null) {
            game.setVndbLength(info.getLength());
        }
        if (info.getLengthMinutes() != null) {
            game.setVndbLengthMinutes(info.getLengthMinutes());
        }
        if (info.getPlatforms() != null) {
            game.setVndbPlatforms(info.getPlatforms());
        }
        if (info.getOlang() != null) {
            game.setVndbOlang(info.getOlang());
        }
        if (info.getLanguages() != null) {
            game.setVndbLanguages(info.getLanguages());
        }
        if (info.getScreenshotUrls() != null && !info.getScreenshotUrls().isEmpty()
                && (game.getScreenshots() == null || game.getScreenshots().isEmpty())) {
            game.setScreenshots(new ArrayList<>(info.getScreenshotUrls()));
        }
    }

    private GameInfoResult buildGameInfoResult(VndbVisualNovel vn) {
        GameInfoResult result = new GameInfoResult();
        result.setVndbId(vn.getId());
        result.setTitle(vn.getTitle());
        result.setAlttitle(vn.getAlttitle());

        String bestAltTitle = findBestAltTitle(vn);
        if (bestAltTitle != null) result.setAlttitle(bestAltTitle);

        result.setDescription(vn.getDescription());

        String developer = extractPrimaryDeveloper(vn);
        result.setDeveloper(developer);

        String developerOriginal = extractPrimaryDeveloperOriginal(vn);
        result.setDeveloperOriginal(developerOriginal);

        List<String> tagNames = extractTagNames(vn);
        result.setTags(tagNames);

        result.setReleased(vn.getReleased());
        result.setRating(vn.getRating());
        result.setVotecount(vn.getVotecount());
        result.setLength(vn.getLength());
        result.setLengthMinutes(vn.getLengthMinutes());
        result.setPlatforms(vn.getPlatforms());
        result.setOlang(vn.getOlang());
        result.setLanguages(vn.getLanguages());

        if (vn.getImage() != null) {
            result.setCoverUrl(vn.getImage().getUrl());
        }

        if (vn.getScreenshots() != null && !vn.getScreenshots().isEmpty()) {
            List<String> screenshotUrls = vn.getScreenshots().stream()
                .map(VndbVisualNovel.Screenshot::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            result.setScreenshotUrls(screenshotUrls);
        }

        DeepSeekService.TranslationResult translation = deepSeekService.translateGameInfo(
            vn.getDescription(),
            tagNames
        );

        if (translation.getDescriptionZh() != null) {
            result.setDescriptionZh(translation.getDescriptionZh());
        }
        if (translation.getTagsZh() != null && !translation.getTagsZh().isEmpty()) {
            result.setTagsZh(translation.getTagsZh());
        }

        return result;
    }

    private String findBestAltTitle(VndbVisualNovel vn) {
        if (vn.getTitles() != null) {
            for (VndbVisualNovel.Title t : vn.getTitles()) {
                if ("ja".equals(t.getLang()) && t.getLatin() != null && !t.getLatin().isEmpty()) {
                    return t.getLatin();
                }
            }
            for (VndbVisualNovel.Title t : vn.getTitles()) {
                if ("en".equals(t.getLang()) && t.getTitle() != null && !t.getTitle().isEmpty()) {
                    return t.getTitle();
                }
            }
        }
        if (vn.getAlttitle() != null && !vn.getAlttitle().isEmpty()) {
            return vn.getAlttitle();
        }
        return null;
    }

    private String extractPrimaryDeveloper(VndbVisualNovel vn) {
        if (vn.getDevelopers() != null && !vn.getDevelopers().isEmpty()) {
            return vn.getDevelopers().get(0).getName();
        }
        return null;
    }

    private String extractPrimaryDeveloperOriginal(VndbVisualNovel vn) {
        if (vn.getDevelopers() != null && !vn.getDevelopers().isEmpty()) {
            return vn.getDevelopers().get(0).getOriginal();
        }
        return null;
    }

    private List<String> extractTagNames(VndbVisualNovel vn) {
        if (vn.getTags() == null || vn.getTags().isEmpty()) {
            return Collections.emptyList();
        }
        return vn.getTags().stream()
            .filter(t -> t.getRating() != null && t.getRating() >= 2.0)
            .sorted((a, b) -> Double.compare(b.getRating(), a.getRating()))
            .limit(10)
            .map(VndbVisualNovel.Tag::getName)
            .collect(Collectors.toList());
    }

    private List<String> downloadScreenshots(String gameId, List<String> urls) {
        List<String> localPaths = new ArrayList<>();
        try {
            String dataDir = System.getProperty("user.dir") + "/data";
            Path screenshotsDir = Paths.get(dataDir, "images", "screenshots", gameId);
            if (!Files.exists(screenshotsDir)) {
                Files.createDirectories(screenshotsDir);
            }

            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                try {
                    String ext = ".jpg";
                    int lastDot = url.lastIndexOf('.');
                    int qIdx = url.lastIndexOf('?');
                    if (lastDot >= 0 && (qIdx < 0 || lastDot < qIdx)) {
                        ext = url.substring(lastDot, qIdx > 0 ? qIdx : url.length());
                    }
                    if (ext.length() > 5) ext = ".jpg";

                    String fileName = (i + 1) + ext;
                    Path dest = screenshotsDir.resolve(fileName);
                    try (InputStream in = new URL(url).openStream()) {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        localPaths.add("/images/screenshots/" + gameId + "/" + fileName);
                        log.info("Downloaded screenshot {} for game {}", i + 1, gameId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to download screenshot {} for {}: {}", i, gameId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to setup screenshot dir for game {}", gameId, e);
        }
        return localPaths;
    }
}
