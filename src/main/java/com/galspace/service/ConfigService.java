package com.galspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galspace.model.Config;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class ConfigService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Config config = new Config();

    private final String configJsonPath = System.getProperty("user.dir") + "/data/config.json";

    @PostConstruct
    public void init() {
        try {
            File file = new File(configJsonPath);
            if (file.exists()) {
                config = objectMapper.readValue(file, Config.class);
            } else {
                saveConfig();
            }
        } catch (IOException e) {
            log.error("Failed to load config.json", e);
        }
    }

    public Config getConfig() {
        return config;
    }

    public synchronized void saveConfig() {
        try {
            Path path = Paths.get(configJsonPath);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
        } catch (IOException e) {
            log.error("Failed to save config.json", e);
        }
    }

    public String getLeProcPath() {
        return config.getLeProcPath();
    }
}