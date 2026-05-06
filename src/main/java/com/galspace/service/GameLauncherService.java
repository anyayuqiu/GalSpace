package com.galspace.service;

import com.galspace.model.Game;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameLauncherService {

    private final JsonStorageService jsonStorageService;
    private final ConfigService configService;

    public void launchGame(String gameId) throws IOException {
        Game game = jsonStorageService.getGame(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        List<String> command = new ArrayList<>();

        if (game.isNeedLocaleEmulator()) {
            String lePath = configService.getLeProcPath();
            if (lePath == null || lePath.trim().isEmpty() || !new File(lePath).exists()) {
                throw new IllegalStateException("Locale Emulator path is not configured or invalid.");
            }
            command.add(lePath);
            command.add("-run");
        }

        command.add(game.getExePath());

        log.info("Launching game: {} with command: {}", game.getTitle(), command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(game.getGameDir()));
        pb.start();

        game.setLastPlayTime(System.currentTimeMillis());
        jsonStorageService.updateGame(game);
    }
}