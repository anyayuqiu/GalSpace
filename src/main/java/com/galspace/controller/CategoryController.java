package com.galspace.controller;

import com.galspace.model.Category;
import com.galspace.service.JsonStorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final JsonStorageService jsonStorageService;

    @GetMapping
    public List<Category> getAllCategories() {
        return jsonStorageService.getAllCategories();
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CreateCategoryRequest request) {
        String name = request.getName() != null ? request.getName().trim() : "";
        if (name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "CATEGORY_NAME_EMPTY", "message", "分类名称不能为空"));
        }
        if (name.length() > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "CATEGORY_NAME_TOO_LONG", "message", "分类名称不能超过50个字符"));
        }
        Category existing = jsonStorageService.getCategoryByName(name);
        if (existing != null) {
            return ResponseEntity.status(409).body(Map.of("error", "CATEGORY_EXISTS", "message", "分类 '" + name + "' 已存在", "id", existing.getId()));
        }
        Category cat = jsonStorageService.addCategory(name, request.getColor());
        return ResponseEntity.ok(cat);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable String id, @RequestBody UpdateCategoryRequest request) {
        Category cat = jsonStorageService.getCategory(id);
        if (cat == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (newName.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "CATEGORY_NAME_EMPTY", "message", "分类名称不能为空"));
            }
            Category dup = jsonStorageService.getCategoryByName(newName);
            if (dup != null && !dup.getId().equals(id)) {
                return ResponseEntity.status(409).body(Map.of("error", "CATEGORY_EXISTS", "message", "分类 '" + newName + "' 已存在"));
            }
            cat.setName(newName);
        }
        if (request.getColor() != null) cat.setColor(request.getColor());
        if (request.getSortOrder() != null) cat.setSortOrder(request.getSortOrder());
        jsonStorageService.updateCategory(cat);
        return ResponseEntity.ok(cat);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable String id) {
        Category cat = jsonStorageService.getCategory(id);
        if (cat == null) {
            return ResponseEntity.notFound().build();
        }
        jsonStorageService.deleteCategory(id);
        return ResponseEntity.ok().body(Map.of("deleted", true));
    }

    @PostMapping("/{categoryId}/games/{gameId}")
    public ResponseEntity<?> addGameToCategory(@PathVariable String categoryId, @PathVariable String gameId) {
        Category cat = jsonStorageService.getCategory(categoryId);
        if (cat == null) {
            return ResponseEntity.status(404).body(Map.of("error", "CATEGORY_NOT_FOUND", "message", "分类不存在"));
        }
        jsonStorageService.addGameToCategory(gameId, categoryId);
        return ResponseEntity.ok(Map.of("added", true));
    }

    @DeleteMapping("/{categoryId}/games/{gameId}")
    public ResponseEntity<?> removeGameFromCategory(@PathVariable String categoryId, @PathVariable String gameId) {
        Category cat = jsonStorageService.getCategory(categoryId);
        if (cat == null) {
            return ResponseEntity.status(404).body(Map.of("error", "CATEGORY_NOT_FOUND", "message", "分类不存在"));
        }
        jsonStorageService.removeGameFromCategory(gameId, categoryId);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    @PostMapping("/{categoryId}/games/batch")
    public ResponseEntity<?> batchAddGames(@PathVariable String categoryId, @RequestBody BatchGameRequest request) {
        Category cat = jsonStorageService.getCategory(categoryId);
        if (cat == null) {
            return ResponseEntity.status(404).body(Map.of("error", "CATEGORY_NOT_FOUND", "message", "分类不存在"));
        }
        if (request.getGameIds() == null || request.getGameIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "GAME_IDS_EMPTY", "message", "游戏ID列表不能为空"));
        }
        jsonStorageService.batchAddGamesToCategory(request.getGameIds(), categoryId);
        return ResponseEntity.ok(Map.of("added", request.getGameIds().size()));
    }

    @PutMapping("/games/{gameId}")
    public ResponseEntity<?> setGameCategories(@PathVariable String gameId, @RequestBody SetCategoriesRequest request) {
        jsonStorageService.setGameCategories(gameId, request.getCategoryIds());
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @Data
    static class CreateCategoryRequest {
        private String name;
        private String color;
    }

    @Data
    static class UpdateCategoryRequest {
        private String name;
        private String color;
        private Integer sortOrder;
    }

    @Data
    static class BatchGameRequest {
        private List<String> gameIds;
    }

    @Data
    static class SetCategoriesRequest {
        private List<String> categoryIds;
    }
}
