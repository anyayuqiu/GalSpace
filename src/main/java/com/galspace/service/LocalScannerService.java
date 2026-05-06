package com.galspace.service;

import com.galspace.model.Game;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalScannerService {

    private final JsonStorageService jsonStorageService;

    private static final Set<String> ENGINE_FILES = Set.of(
        "data.xp3", "arc.nsa", "rio.ini", "system.lua", "BGI.exe", "SiglusEngine.exe", "Malie.exe"
    );

    public int scanAndImport(String rootDirPath, int maxDepth) {
        return scanAndImport(rootDirPath, maxDepth, false);
    }

    public int scanAndImport(String rootDirPath, int maxDepth, boolean fuzzy) {
        if (rootDirPath == null || rootDirPath.trim().isEmpty()) {
            log.warn("Scan path is empty, skipping scan.");
            return 0;
        }

        Path rootPath = Paths.get(rootDirPath);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            log.error("Invalid root directory: {}", rootDirPath);
            return 0;
        }

        List<Game> newGames = new ArrayList<>();
        Set<String> existingExePaths = new HashSet<>();
        Set<String> existingGameDirs = new HashSet<>();
        for (Game g : jsonStorageService.getAllGames()) {
            existingExePaths.add(g.getExePath());
            if (g.getGameDir() != null) {
                existingGameDirs.add(g.getGameDir());
            }
        }

        try {
            Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString().toLowerCase();
                    if (fileName.endsWith(".exe") && !fileName.contains("uninstall") && !fileName.contains("unins")) {
                        String exePath = file.toAbsolutePath().toString();
                        File dir = file.getParent().toFile();
                        String dirPath = dir.getAbsolutePath();
                        if (!existingExePaths.contains(exePath) && !existingGameDirs.contains(dirPath)) {
                            if (fuzzy || isGameDirectory(dir)) {
                                Game game = createGameFromFile(file.toFile());
                                newGames.add(game);
                                existingExePaths.add(exePath);
                                existingGameDirs.add(dirPath);
                                return FileVisitResult.SKIP_SIBLINGS;
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Game game : newGames) {
                jsonStorageService.addGame(game);
            }
            log.info("Scanned and added {} new games.", newGames.size());
            return newGames.size();
        } catch (IOException e) {
            log.error("Error scanning directory", e);
            return 0;
        }
    }

    private boolean isGameDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;

        long totalSize = 0;
        for (File f : files) {
            if (f.isFile()) {
                totalSize += f.length();
                String name = f.getName().toLowerCase();
                if (ENGINE_FILES.contains(name) || name.endsWith(".xp3") || name.endsWith(".pck") || name.endsWith(".arc")) {
                    return true;
                }
            }
        }
        return totalSize > 50 * 1024 * 1024;
    }

    private Game createGameFromFile(File exeFile) {
        Game game = new Game();
        game.setId(UUID.randomUUID().toString());
        String folderName = exeFile.getParentFile().getName();
        game.setTitle(folderName);
        game.setTitleZh(folderName);
        game.setGameDir(exeFile.getParentFile().getAbsolutePath());
        game.setExePath(exeFile.getAbsolutePath());
        game.setAddTime(System.currentTimeMillis());
        game.setNeedLocaleEmulator(false);
        return game;
    }
}