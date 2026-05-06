package com.galspace.model;

import lombok.Data;

@Data
public class Config {
    private String leProcPath;
    private String languagePreference = "zh";
    private boolean autoScanEnabled = false;
    private String autoScanPath;
    private boolean globalBlur = false;
    private String deepseekApiKey;
    private String serverAddress = "0.0.0.0";
    private int serverPort = 8080;
}