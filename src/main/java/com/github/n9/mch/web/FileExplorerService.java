package com.github.n9.mch.web;

import com.github.n9.mch.Utils;
import com.github.n9.mch.minecraft.ServerManager;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileExplorerService {

    private final ServerManager serverManager;

    public FileExplorerService(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public List<Map<String, Object>> list(String serverName, String path) {
        File directory = resolve(serverName, path).toFile();
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("ディレクトリが見つかりません。");
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return List.of();
        }
        return java.util.Arrays.stream(files)
                .sorted(Comparator.comparing(File::isFile).thenComparing(File::getName))
                .map(file -> entry(serverName, file))
                .collect(Collectors.toList());
    }

    public Map<String, Object> read(String serverName, String path) throws IOException {
        Path file = resolve(serverName, path);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("ファイルが見つかりません。");
        }
        if (Files.size(file) > 1024 * 1024) {
            throw new IllegalArgumentException("1MB を超えるファイルは画面上で編集できません。");
        }
        Map<String, Object> result = entry(serverName, file.toFile());
        result.put("content", Files.readString(file, StandardCharsets.UTF_8));
        return result;
    }

    public void save(String serverName, String path, String content) throws IOException {
        Path file = resolve(serverName, path);
        if (Files.isDirectory(file)) {
            throw new IllegalArgumentException("ディレクトリには保存できません。");
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    public void upload(String serverName, String path, MultipartFile multipartFile) throws IOException {
        Path directory = resolve(serverName, path);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("アップロード先がディレクトリではありません。");
        }
        String filename = multipartFile.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("ファイル名が不正です。");
        }
        multipartFile.transferTo(directory.resolve(filename).toFile());
    }

    public void delete(String serverName, String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("削除するファイルを選択してください。");
        }
        File file = resolve(serverName, path).toFile();
        if (!file.exists()) {
            return;
        }
        Utils.delete(file.getPath());
    }

    private Map<String, Object> entry(String serverName, File file) {
        Path root = serverManager.getUserdir(serverName).toPath().toAbsolutePath().normalize();
        Path path = file.toPath().toAbsolutePath().normalize();
        Map<String, Object> entry = new HashMap<>();
        entry.put("name", file.getName());
        entry.put("path", root.relativize(path).toString().replace(File.separatorChar, '/'));
        entry.put("directory", file.isDirectory());
        entry.put("size", file.isDirectory() ? "" : formatBytes(file.length()));
        entry.put("sizeBytes", file.isDirectory() ? 0 : file.length());
        entry.put("modifiedAt", Instant.ofEpochMilli(file.lastModified()).toString());
        return entry;
    }

    private Path resolve(String serverName, String requestedPath) {
        Path root = serverManager.getUserdir(serverName).toPath().toAbsolutePath().normalize();
        String cleanPath = requestedPath == null ? "" : requestedPath.trim();
        Path resolved = root.resolve(cleanPath).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("サーバーディレクトリ外のファイルは操作できません。");
        }
        return resolved;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unit = -1;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.1f %s", value, units[unit]);
    }
}
