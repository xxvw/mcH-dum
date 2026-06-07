package com.github.n9.mch.web;

import com.github.n9.mch.minecraft.MinecraftError;
import com.github.n9.mch.minecraft.ServerManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminApiController {

    private final ServerManager serverManager;
    private final DashboardMetricsService metricsService;
    private final FileExplorerService fileExplorerService;

    public AdminApiController(
            ServerManager serverManager,
            DashboardMetricsService metricsService,
            FileExplorerService fileExplorerService
    ) {
        this.serverManager = serverManager;
        this.metricsService = metricsService;
        this.fileExplorerService = fileExplorerService;
    }

    @GetMapping("/admin/api/status")
    public Map<String, Object> status() {
        return metricsService.snapshot();
    }

    @GetMapping("/admin/api/servers/{name}/console")
    public Map<String, Object> console(@PathVariable String name) {
        Map<String, Object> response = new HashMap<>();
        response.put("server", name);
        response.put("lines", serverManager.getConsoleLines(name));
        return response;
    }

    @PostMapping("/admin/api/servers/{name}/command")
    public ResponseEntity<Map<String, Object>> command(@PathVariable String name, @RequestParam String command) {
        MinecraftError error = serverManager.commandServer(name, command);
        Map<String, Object> response = new HashMap<>();
        response.put("ok", error == null);
        response.put("message", error == null ? "コマンドを送信しました。" : error.getMsg());
        return ResponseEntity.status(error == null ? 200 : 409).body(response);
    }

    @GetMapping("/admin/api/servers/{name}/files")
    public Map<String, Object> files(@PathVariable String name, @RequestParam(required = false, defaultValue = "") String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("server", name);
        response.put("path", path);
        response.put("entries", fileExplorerService.list(name, path));
        return response;
    }

    @GetMapping("/admin/api/servers/{name}/files/content")
    public Map<String, Object> content(@PathVariable String name, @RequestParam String path) throws IOException {
        return fileExplorerService.read(name, path);
    }

    @PostMapping("/admin/api/servers/{name}/files/content")
    public Map<String, Object> save(@PathVariable String name, @RequestParam String path, @RequestParam String content) throws IOException {
        fileExplorerService.save(name, path, content);
        return ok("保存しました。");
    }

    @PostMapping("/admin/api/servers/{name}/files/upload")
    public Map<String, Object> upload(
            @PathVariable String name,
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        fileExplorerService.upload(name, path, file);
        return ok("アップロードしました。");
    }

    @PostMapping("/admin/api/servers/{name}/files/delete")
    public Map<String, Object> delete(@PathVariable String name, @RequestParam String path) {
        fileExplorerService.delete(name, path);
        return ok("削除しました。");
    }

    @ExceptionHandler({IllegalArgumentException.class, IOException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("ok", false);
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    private Map<String, Object> ok(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("message", message);
        return response;
    }
}
