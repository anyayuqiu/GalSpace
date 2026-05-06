package com.galspace.controller;

import com.galspace.model.Game;
import com.galspace.service.GameLauncherService;
import com.galspace.service.JsonStorageService;
import com.galspace.service.LocalScannerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final JsonStorageService jsonStorageService;
    private final LocalScannerService localScannerService;
    private final GameLauncherService gameLauncherService;

    @GetMapping
    public List<Game> getGames(@RequestParam(required = false) String query) {
        return jsonStorageService.searchGames(query);
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanGames(@RequestBody ScanRequest request) {
        int depth = request.getDepth() != null ? request.getDepth() : 3;
        boolean fuzzy = request.isFuzzy();
        int count = localScannerService.scanAndImport(request.getPath(), depth, fuzzy);
        return ResponseEntity.ok().body("{\"message\": \"Scanned and added " + count + " games.\", \"count\": " + count + "}");
    }

    @PostMapping
    public ResponseEntity<Game> addGame(@RequestBody Game game) {
        if (game.getId() == null) {
            game.setId(UUID.randomUUID().toString());
        }
        game.setAddTime(System.currentTimeMillis());
        jsonStorageService.addGame(game);
        return ResponseEntity.ok(game);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Game> updateGame(@PathVariable String id, @RequestBody Game game) {
        Game existing = jsonStorageService.getGame(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        game.setId(id);
        jsonStorageService.updateGame(game);
        return ResponseEntity.ok(game);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGame(@PathVariable String id, @RequestParam(defaultValue = "false") boolean deleteFiles) {
        Game existing = jsonStorageService.getGame(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        jsonStorageService.deleteGame(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch/delete")
    public ResponseEntity<?> batchDelete(@RequestBody List<String> ids) {
        int count = 0;
        for (String id : ids) {
            if (jsonStorageService.getGame(id) != null) {
                jsonStorageService.deleteGame(id);
                count++;
            }
        }
        return ResponseEntity.ok().body("{\"deleted\": " + count + "}");
    }

    @PutMapping("/reorder")
    public ResponseEntity<?> reorderGames(@RequestBody List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            Game game = jsonStorageService.getGame(ids.get(i));
            if (game != null) {
                game.setSortOrder(i);
                jsonStorageService.updateGameNoSave(game);
            }
        }
        jsonStorageService.saveAllGames();
        return ResponseEntity.ok().body("{\"reordered\": " + ids.size() + "}");
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<?> launchGame(@PathVariable String id) {
        try {
            gameLauncherService.launchGame(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/cover")
    public ResponseEntity<?> uploadCover(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        Game game = jsonStorageService.getGame(id);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String dataDir = System.getProperty("user.dir") + "/data";
            String ext = getExtension(file.getOriginalFilename());
            String fileName = id + ext;
            Path path = Paths.get(dataDir, "images", "covers", fileName);
            Files.write(path, file.getBytes());

            game.setCoverUrl("/images/covers/" + fileName);
            jsonStorageService.updateGame(game);

            return ResponseEntity.ok(game);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? ".jpg" : filename.substring(lastDot);
    }

    @Data
    static class ScanRequest {
        private String path;
        private Integer depth;
        private boolean fuzzy;
    }
}