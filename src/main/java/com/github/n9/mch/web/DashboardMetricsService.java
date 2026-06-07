package com.github.n9.mch.web;

import com.github.n9.mch.minecraft.ServerManager;
import com.github.n9.mch.server.ManagedServer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DashboardMetricsService {

    private final ServerManager serverManager;

    public DashboardMetricsService(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public Map<String, Object> snapshot() {
        List<ManagedServer> servers = serverManager.listServers();
        Runtime runtime = Runtime.getRuntime();
        File dataDir = serverManager.maindir == null ? new File("./data") : serverManager.maindir;
        dataDir.mkdirs();
        long dataBytes = directorySize(dataDir);
        long usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalServers", servers.size());
        metrics.put("runningServers", serverManager.getAll().size());
        metrics.put("maxServers", serverManager.getMaxServers());
        metrics.put("dataBytes", dataBytes);
        metrics.put("dataSize", formatBytes(dataBytes));
        metrics.put("diskFreeBytes", dataDir.getUsableSpace());
        metrics.put("diskTotalBytes", dataDir.getTotalSpace());
        metrics.put("diskFree", formatBytes(dataDir.getUsableSpace()));
        metrics.put("usedMemoryBytes", usedMemoryBytes);
        metrics.put("maxMemoryBytes", runtime.maxMemory());
        metrics.put("usedMemory", formatBytes(usedMemoryBytes));
        metrics.put("maxMemory", formatBytes(runtime.maxMemory()));
        metrics.put("systemCpuLoad", cpuLoad(true));
        metrics.put("processCpuLoad", cpuLoad(false));
        metrics.put("servers", servers.stream().map(this::serverSummary).toArray());
        return metrics;
    }

    private Map<String, Object> serverSummary(ManagedServer server) {
        Map<String, Object> summary = new HashMap<>();
        long size = directorySize(serverManager.getUserdir(server.getName()));
        summary.put("name", server.getName());
        summary.put("port", server.getPort());
        summary.put("status", server.getStatus() == null ? "UNKNOWN" : server.getStatus().name());
        summary.put("source", server.getSource());
        summary.put("sizeBytes", size);
        summary.put("size", formatBytes(size));
        summary.put("lastStartedAt", server.getLastStartedAt());
        summary.put("lastStoppedAt", server.getLastStoppedAt());
        return summary;
    }

    private long directorySize(File directory) {
        if (directory == null || !directory.exists()) {
            return 0L;
        }
        try (Stream<java.nio.file.Path> paths = Files.walk(directory.toPath())) {
            return paths
                    .filter(path -> path.toFile().isFile())
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (Exception e) {
            return 0L;
        }
    }

    private double cpuLoad(boolean system) {
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (!(osBean instanceof com.sun.management.OperatingSystemMXBean)) {
            return 0;
        }
        com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) osBean;
        double load = system ? bean.getSystemCpuLoad() : bean.getProcessCpuLoad();
        if (load < 0) {
            return 0;
        }
        return Math.round(load * 1000.0) / 10.0;
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
