package com.galspace.controller;

import com.galspace.model.Game;
import com.galspace.model.GameInfoResult;
import com.galspace.model.VndbVisualNovel;
import com.galspace.service.GameInfoService;
import com.galspace.service.JsonStorageService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class GameInfoController {

    private static final ExecutorService VNDB_EXECUTOR = Executors.newFixedThreadPool(6);

    private final GameInfoService gameInfoService;
    private final JsonStorageService jsonStorageService;

    @GetMapping("/api/vndb/fetch/{vndbId}")
    public ResponseEntity<GameInfoResult> fetchVndbInfo(@PathVariable String vndbId) {
        GameInfoResult result = gameInfoService.fetchAndTranslate(vndbId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/vndb/search")
    public ResponseEntity<List<VndbVisualNovel>> searchVndb(@RequestBody SearchRequest request) {
        List<VndbVisualNovel> results = gameInfoService.searchVndb(request.getKeyword());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/api/games/{id}/vndb-search")
    public ResponseEntity<List<VndbVisualNovel>> searchVndbByGameTitle(@PathVariable String id) {
        Game game = jsonStorageService.getGame(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        String keyword = game.getTitle();
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        String cleanKeyword = cleanGameTitleForSearch(keyword);
        List<VndbVisualNovel> results = gameInfoService.searchVndb(cleanKeyword);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/api/games/{id}/vndb-select")
    public ResponseEntity<GameInfoResult> selectVndbResult(@PathVariable String id, @RequestBody VndbIdRequest request) {
        Game game = jsonStorageService.getGame(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        game.setVndbId(request.getVndbId());
        jsonStorageService.updateGame(game);

        GameInfoResult result = gameInfoService.fetchAndTranslate(request.getVndbId());
        if (result != null) {
            gameInfoService.applyToGame(game, result);
            jsonStorageService.updateGame(game);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/games/{id}/enrich")
    public ResponseEntity<GameInfoResult> enrichGame(@PathVariable String id) {
        GameInfoResult result = gameInfoService.enrichGame(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/games/{id}/enrich-preview")
    public ResponseEntity<GameInfoResult> enrichGamePreview(@PathVariable String id) {
        GameInfoResult result = gameInfoService.enrichGamePreview(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/api/games/{id}/vndb")
    public ResponseEntity<Game> setVndbId(@PathVariable String id, @RequestBody VndbIdRequest request) {
        Game game = jsonStorageService.getGame(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        game.setVndbId(request.getVndbId());
        jsonStorageService.updateGame(game);
        return ResponseEntity.ok(game);
    }

    @PostMapping("/api/games/batch/vndb-search")
    public ResponseEntity<Map<String, List<VndbVisualNovel>>> batchVndbSearch(@RequestBody List<String> ids) {
        Map<String, List<VndbVisualNovel>> results = new LinkedHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String id : ids) {
            Game game = jsonStorageService.getGame(id);
            if (game == null) {
                results.put(id, Collections.emptyList());
                continue;
            }
            String keyword = game.getTitle();
            if (keyword == null || keyword.trim().isEmpty()) {
                results.put(id, Collections.emptyList());
                continue;
            }
            String cleanKeyword = cleanGameTitleForSearch(keyword);
            futures.add(CompletableFuture.runAsync(() -> {
                List<VndbVisualNovel> searchResults = gameInfoService.searchVndb(cleanKeyword);
                synchronized (results) {
                    results.put(id, searchResults);
                }
            }, VNDB_EXECUTOR));
        }

        futures.forEach(f -> { try { f.get(30, TimeUnit.SECONDS); } catch (Exception e) { log.warn("VNDB search timeout", e); } });
        return ResponseEntity.ok(results);
    }

    @PostMapping("/api/games/batch/vndb-enrich")
    public ResponseEntity<BatchEnrichResult> batchVndbEnrich(@RequestBody List<VndbSelectItem> items) {
        List<CompletableFuture<EnrichItemResult>> futures = new ArrayList<>();

        for (VndbSelectItem item : items) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Game game = jsonStorageService.getGame(item.getGameId());
                    if (game == null) {
                        return new EnrichItemResult(item.getGameId(), null, false, "游戏不存在");
                    }
                    GameInfoResult result = gameInfoService.fetchAndTranslate(item.getVndbId());
                    if (result != null) {
                        game.setVndbId(item.getVndbId());
                        gameInfoService.applyToGameNoScreenshots(game, result);
                        synchronized (jsonStorageService) {
                            jsonStorageService.updateGameNoSave(game);
                        }
                        String name = game.getTitleZh() != null ? game.getTitleZh() : game.getTitle();
                        return new EnrichItemResult(item.getGameId(), name, true, null);
                    }
                    String name = game.getTitleZh() != null ? game.getTitleZh() : game.getTitle();
                    return new EnrichItemResult(item.getGameId(), name, false, "VNDB 无数据");
                } catch (Exception e) {
                    log.warn("Failed to enrich game {}: {}", item.getGameId(), e.getMessage());
                    return new EnrichItemResult(item.getGameId(), null, false, e.getMessage());
                }
            }, VNDB_EXECUTOR));
        }

        List<EnrichItemResult> itemResults = new ArrayList<>();
        int success = 0;
        for (CompletableFuture<EnrichItemResult> f : futures) {
            try {
                EnrichItemResult r = f.get(60, TimeUnit.SECONDS);
                itemResults.add(r);
                if (r.isSuccess()) success++;
            } catch (Exception e) {
                log.warn("VNDB enrich timeout", e);
                itemResults.add(new EnrichItemResult(null, null, false, "请求超时"));
            }
        }

        jsonStorageService.saveAllGames();

        int failed = itemResults.size() - success;
        BatchEnrichResult batchResult = new BatchEnrichResult(items.size(), success, failed, itemResults);
        return ResponseEntity.ok(batchResult);
    }

    @Data
    static class SearchRequest {
        private String keyword;
    }

    @Data
    static class VndbIdRequest {
        private String vndbId;
    }

    @Data
    static class VndbSelectItem {
        private String gameId;
        private String vndbId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class EnrichItemResult {
        private String gameId;
        private String name;
        private boolean success;
        private String error;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class BatchEnrichResult {
        private int total;
        private int enriched;
        private int failed;
        private List<EnrichItemResult> results;
    }

    private String cleanGameTitleForSearch(String title) {
        String cleaned = title
            .replaceAll("[（(][^)）]*[)）]", "")
            .replaceAll("[-–—].*$", "")
            .replaceAll("ver\\.?\\s*\\d+[\\.\\d]*", "")
            .replaceAll("v\\d+[\\.\\d]*", "")
            .replaceAll("\\[.*?\\]", "")
            .replaceAll("【.*?】", "")
            .replaceAll("\\d+\\.\\d+[GM]B", "")
            .replaceAll("[Rr]epack", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isEmpty()) {
            cleaned = title;
        }
        return cleaned;
    }
}
