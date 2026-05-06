package com.galspace.controller;

import com.galspace.model.Config;
import com.galspace.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Config getConfig() {
        return configService.getConfig();
    }

    @PutMapping
    public ResponseEntity<Config> updateConfig(@RequestBody Config config) {
        Config existing = configService.getConfig();
        existing.setLeProcPath(config.getLeProcPath());
        existing.setLanguagePreference(config.getLanguagePreference());
        existing.setAutoScanEnabled(config.isAutoScanEnabled());
        existing.setAutoScanPath(config.getAutoScanPath());
        existing.setGlobalBlur(config.isGlobalBlur());
        existing.setDeepseekApiKey(config.getDeepseekApiKey());
        if (config.getServerAddress() != null && !config.getServerAddress().isBlank()) {
            existing.setServerAddress(config.getServerAddress().trim());
        }
        if (config.getServerPort() >= 1024 && config.getServerPort() <= 65535) {
            existing.setServerPort(config.getServerPort());
        }
        configService.saveConfig();
        return ResponseEntity.ok(existing);
    }
}