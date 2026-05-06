package com.galspace.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galspace.model.Category;
import com.galspace.model.Game;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JsonStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Game> gameCache = new ConcurrentHashMap<>();
    private final Map<String, Category> categoryCache = new ConcurrentHashMap<>();

    private final String dataDir = System.getProperty("user.dir") + "/data";
    private final String gamesJsonPath = dataDir + "/games.json";
    private final String categoriesJsonPath = dataDir + "/categories.json";

    @PostConstruct
    public void init() {
        try {
            ensureDirectories();
            loadGames();
            loadCategories();
        } catch (IOException e) {
            log.error("Failed to initialize JsonStorageService", e);
        }
    }

    private void ensureDirectories() throws IOException {
        Path dataPath = Paths.get(dataDir);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
        Path imagesPath = Paths.get(dataDir, "images", "covers");
        if (!Files.exists(imagesPath)) {
            Files.createDirectories(imagesPath);
        }
        Path gamesJson = Paths.get(gamesJsonPath);
        if (!Files.exists(gamesJson)) {
            Files.writeString(gamesJson, "[]");
        }
        Path categoriesJson = Paths.get(categoriesJsonPath);
        if (!Files.exists(categoriesJson)) {
            Files.writeString(categoriesJson, "[]");
        }
    }

    private void loadGames() throws IOException {
        File file = new File(gamesJsonPath);
        List<Game> games = objectMapper.readValue(file, new TypeReference<List<Game>>() {});
        for (Game game : games) {
            gameCache.put(game.getId(), game);
        }
        log.info("Loaded {} games from {}", gameCache.size(), gamesJsonPath);
    }

    public synchronized void saveGames() {
        try {
            List<Game> gamesList = new ArrayList<>(gameCache.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(gamesJsonPath), gamesList);
        } catch (IOException e) {
            log.error("Failed to save games.json", e);
        }
    }

    public List<Game> getAllGames() {
        return gameCache.values().stream()
                .sorted(Comparator.comparing(Game::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Game::getAddTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Game getGame(String id) {
        return gameCache.get(id);
    }

    public void addGame(Game game) {
        gameCache.put(game.getId(), game);
        saveGames();
    }

    public void updateGame(Game game) {
        gameCache.put(game.getId(), game);
        saveGames();
    }

    public void updateGameNoSave(Game game) {
        gameCache.put(game.getId(), game);
    }

    public void saveAllGames() {
        saveGames();
    }

    public void deleteGame(String id) {
        gameCache.remove(id);
        saveGames();
    }

    public List<Game> searchGames(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllGames();
        }
        String lowerQuery = query.toLowerCase();
        return gameCache.values().stream()
                .filter(g -> (g.getTitle() != null && g.getTitle().toLowerCase().contains(lowerQuery)) ||
                             (g.getTitleZh() != null && g.getTitleZh().toLowerCase().contains(lowerQuery)) ||
                             (g.getTags() != null && g.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerQuery))))
                .collect(Collectors.toList());
    }

    private void loadCategories() throws IOException {
        File file = new File(categoriesJsonPath);
        if (!file.exists() || file.length() == 0) {
            Files.writeString(Paths.get(categoriesJsonPath), "[]");
            return;
        }
        List<Category> categories = objectMapper.readValue(file, new TypeReference<List<Category>>() {});
        for (Category c : categories) {
            categoryCache.put(c.getId(), c);
        }
        recalcCategoryGameCounts();
        log.info("Loaded {} categories from {}", categoryCache.size(), categoriesJsonPath);
    }

    private synchronized void saveCategories() {
        try {
            List<Category> list = new ArrayList<>(categoryCache.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(categoriesJsonPath), list);
        } catch (IOException e) {
            log.error("Failed to save categories.json", e);
        }
    }

    public List<Category> getAllCategories() {
        recalcCategoryGameCounts();
        return categoryCache.values().stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getName))
                .collect(Collectors.toList());
    }

    public Category getCategory(String id) {
        return categoryCache.get(id);
    }

    public Category getCategoryByName(String name) {
        return categoryCache.values().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst().orElse(null);
    }

    public Category addCategory(String name, String color) {
        Category existing = getCategoryByName(name);
        if (existing != null) {
            return existing;
        }
        Category c = new Category();
        c.setId(UUID.randomUUID().toString());
        c.setName(name);
        c.setColor(color != null ? color : "#5b8def");
        c.setSortOrder(categoryCache.size());
        c.setCreatedTime(System.currentTimeMillis());
        c.setGameCount(0);
        categoryCache.put(c.getId(), c);
        saveCategories();
        return c;
    }

    public Category updateCategory(Category category) {
        categoryCache.put(category.getId(), category);
        saveCategories();
        return category;
    }

    public void deleteCategory(String id) {
        Category c = categoryCache.remove(id);
        if (c == null) return;
        for (Game g : gameCache.values()) {
            if (g.getCategories() != null && g.getCategories().remove(c.getName())) {
                updateGameNoSave(g);
            }
        }
        saveGames();
        saveCategories();
    }

    public void addGameToCategory(String gameId, String categoryId) {
        Category cat = categoryCache.get(categoryId);
        if (cat == null) return;
        Game game = gameCache.get(gameId);
        if (game == null) return;
        if (game.getCategories() == null) game.setCategories(new ArrayList<>());
        if (!game.getCategories().contains(cat.getName())) {
            game.getCategories().add(cat.getName());
            updateGameNoSave(game);
            saveGames();
            recalcCategoryGameCounts();
            saveCategories();
        }
    }

    public void removeGameFromCategory(String gameId, String categoryId) {
        Category cat = categoryCache.get(categoryId);
        if (cat == null) return;
        Game game = gameCache.get(gameId);
        if (game == null) return;
        if (game.getCategories() != null && game.getCategories().remove(cat.getName())) {
            updateGameNoSave(game);
            saveGames();
            recalcCategoryGameCounts();
            saveCategories();
        }
    }

    public void setGameCategories(String gameId, List<String> categoryIds) {
        Game game = gameCache.get(gameId);
        if (game == null) return;
        List<String> newNames = new ArrayList<>();
        for (String cid : categoryIds) {
            Category cat = categoryCache.get(cid);
            if (cat != null) newNames.add(cat.getName());
        }
        game.setCategories(newNames);
        updateGameNoSave(game);
        saveGames();
        recalcCategoryGameCounts();
        saveCategories();
    }

    public void batchAddGamesToCategory(List<String> gameIds, String categoryId) {
        Category cat = categoryCache.get(categoryId);
        if (cat == null) return;
        for (String gid : gameIds) {
            Game game = gameCache.get(gid);
            if (game == null) continue;
            if (game.getCategories() == null) game.setCategories(new ArrayList<>());
            if (!game.getCategories().contains(cat.getName())) {
                game.getCategories().add(cat.getName());
                updateGameNoSave(game);
            }
        }
        saveGames();
        recalcCategoryGameCounts();
        saveCategories();
    }

    private void recalcCategoryGameCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Game g : gameCache.values()) {
            if (g.getCategories() != null) {
                for (String cn : g.getCategories()) {
                    counts.merge(cn, 1, Integer::sum);
                }
            }
        }
        for (Category c : categoryCache.values()) {
            c.setGameCount(counts.getOrDefault(c.getName(), 0));
        }
    }
}