package com.galspace.controller;

import com.galspace.GalSpaceApplication;
import com.galspace.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final ConfigService configService;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @GetMapping("/dialog/file")
    public ResponseEntity<?> openFileDialog(@RequestParam(defaultValue = "false") boolean isDirectory,
                                            @RequestParam(required = false) String extension,
                                            @RequestParam(required = false) String currentPath) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        JFileChooser fileChooser = new JFileChooser();

        if (currentPath != null && !currentPath.trim().isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                if (isDirectory || currentFile.isDirectory()) {
                    fileChooser.setCurrentDirectory(currentFile.isDirectory() ? currentFile : currentFile.getParentFile());
                } else {
                    fileChooser.setCurrentDirectory(currentFile.getParentFile() != null ? currentFile.getParentFile() : currentFile);
                    fileChooser.setSelectedFile(currentFile);
                }
            }
        }

        fileChooser.setDialogTitle("请选择" + (isDirectory ? "目录" : "文件"));

        if (isDirectory) {
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (extension != null && !extension.trim().isEmpty()) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*" + extension, extension.replace(".", ""));
                fileChooser.setFileFilter(filter);
            }
        }

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return ResponseEntity.ok("{\"path\": \"" + selectedFile.getAbsolutePath().replace("\\", "\\\\") + "\"}");
        }

        return ResponseEntity.ok("{\"path\": \"\"}");
    }

    @PostMapping("/restart")
    public ResponseEntity<?> restart() {
        configService.saveConfig();

        new Thread(() -> {
            try {
                Thread.sleep(800);
                String javaHome = System.getProperty("java.home");
                String classpath = System.getProperty("java.class.path");
                List<String> cmd = new ArrayList<>();
                cmd.add(javaHome + File.separator + "bin" + File.separator + "java");
                cmd.add("-cp");
                cmd.add(classpath);
                cmd.add(GalSpaceApplication.class.getName());
                new ProcessBuilder(cmd).inheritIO().start();
            } catch (Exception ignored) {
            }
            System.exit(0);
        }).start();

        return ResponseEntity.ok(Map.of("restarting", true, "message", "服务正在重启，请稍后刷新页面"));
    }

    @PostMapping("/shutdown")
    public ResponseEntity<?> shutdown() {
        configService.saveConfig();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                applicationContext.close();
            } catch (Exception ignored) {
            }
            System.exit(0);
        }).start();

        return ResponseEntity.ok(Map.of("shuttingDown", true, "message", "服务正在关闭"));
    }
}