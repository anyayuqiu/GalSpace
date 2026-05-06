package com.galspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.galspace.model.Config;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import com.galspace.service.ConfigService;
import com.galspace.service.LocalScannerService;

import java.io.File;

@SpringBootApplication
public class GalSpaceApplication {
    public static void main(String[] args) {
        applyServerConfig();

        SpringApplicationBuilder builder = new SpringApplicationBuilder(GalSpaceApplication.class);
        builder.headless(false);
        ApplicationContext context = builder.run(args);

        ConfigService configService = context.getBean(ConfigService.class);
        if (configService.getConfig().isAutoScanEnabled()) {
            LocalScannerService scannerService = context.getBean(LocalScannerService.class);
            String path = configService.getConfig().getAutoScanPath();
            if (path != null && !path.isEmpty()) {
                System.out.println("Auto-scanning directory on startup: " + path);
                scannerService.scanAndImport(path, 3);
            }
        }
    }

    private static void applyServerConfig() {
        try {
            File configFile = new File(System.getProperty("user.dir") + "/data/config.json");
            if (configFile.exists()) {
                Config config = new ObjectMapper().readValue(configFile, Config.class);
                if (config.getServerAddress() != null && !config.getServerAddress().isBlank()) {
                    System.setProperty("server.address", config.getServerAddress().trim());
                    System.out.println("Server address set to: " + config.getServerAddress().trim());
                }
                if (config.getServerPort() > 0 && config.getServerPort() <= 65535) {
                    System.setProperty("server.port", String.valueOf(config.getServerPort()));
                    System.out.println("Server port set to: " + config.getServerPort());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to apply server config from config.json, using defaults: " + e.getMessage());
        }
    }
}