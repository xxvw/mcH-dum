package com.github.n9.mch.minecraft;

import com.github.n9.mch.Utils;
import com.github.n9.mch.config.McHostProperties;
import com.github.n9.mch.server.ManagedServer;
import com.github.n9.mch.server.ManagedServerRepository;
import com.github.n9.mch.server.ManagedServerStatus;
import net.dv8tion.jda.api.entities.TextChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ServerManager {

    private final ManagedServerRepository repository;
    private final McHostProperties properties;
    private final ConcurrentMap<String, MinecraftServer> runningServers = new ConcurrentHashMap<>();

    public final File maindir;
    public final int max;

    public ServerManager(ManagedServerRepository repository, McHostProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.maindir = new File(properties.getMinecraft().getDataDir());
        this.max = properties.getMinecraft().getMaxServers();
        this.maindir.mkdirs();
    }

    @PostConstruct
    @Transactional
    public void resetRuntimeState() {
        for (ManagedServer server : repository.findAll()) {
            server.setStatus(ManagedServerStatus.STOPPED);
        }
    }

    public List<MinecraftServer> getAll() {
        cleanupStoppedProcesses();
        return new ArrayList<>(runningServers.values());
    }

    public List<ManagedServer> listServers() {
        cleanupStoppedProcesses();
        return repository.findAll().stream()
                .sorted(Comparator.comparing(ManagedServer::getName))
                .collect(Collectors.toList());
    }

    @Transactional
    public ManagedServer createServer(String name, String source, String ownerDiscordId, String discordChannelId) {
        String normalizedName = normalizeName(name);
        return repository.findByName(normalizedName).orElseGet(() -> {
            ManagedServer server = new ManagedServer();
            server.setName(normalizedName);
            server.setPort(freePort());
            server.setStatus(ManagedServerStatus.STOPPED);
            server.setSource(source);
            server.setOwnerDiscordId(ownerDiscordId);
            server.setDiscordChannelId(discordChannelId);
            new File(maindir, normalizedName).mkdirs();
            return repository.save(server);
        });
    }

    public void newServer(String name, TextChannel channel) {
        MinecraftError error = startServer(name, channel);
        if (error != null && channel != null) {
            channel.sendMessage(error.getMsg()).queue();
        }
    }

    @Transactional
    public MinecraftError startServer(String name) {
        return startServer(name, null);
    }

    @Transactional
    public MinecraftError startServer(String name, TextChannel channel) {
        cleanupStoppedProcesses();
        if (runningServers.size() >= getMaxServers()) {
            return MinecraftError.RESOURCE_LIMIT;
        }

        ManagedServer record = createServer(name, channel == null ? "web" : "discord", null, channel == null ? null : channel.getId());
        MinecraftServer running = runningServers.get(record.getName());
        if (running != null && running.isAlive()) {
            return MinecraftError.STARTING;
        }

        Consumer<String> notifier = message -> {
            if (channel != null) {
                channel.sendMessage(message).queue();
            }
        };
        MinecraftServer server = new MinecraftServer(
                record.getName(),
                record.getPort(),
                maindir,
                notifier,
                () -> markStopped(record.getName()),
                properties.getMinecraft().getJavaCommand(),
                properties.getMinecraft().getMemory(),
                "-jar",
                "server.jar",
                "nogui"
        );
        MinecraftError error = server.start();
        if (error != null) {
            return error;
        }

        runningServers.put(record.getName(), server);
        record.setStatus(ManagedServerStatus.RUNNING);
        record.setLastStartedAt(Instant.now());
        repository.save(record);
        return null;
    }

    @Transactional
    public MinecraftError stopServer(String name) {
        MinecraftServer server = getServer(name);
        if (server == null || !server.isAlive()) {
            markStopped(name);
            return MinecraftError.NOT_FOUND;
        }
        return server.stop();
    }

    public MinecraftError commandServer(String name, String command) {
        MinecraftServer server = getServer(name);
        if (server == null || !server.isAlive()) {
            return MinecraftError.NOT_FOUND;
        }
        server.command(command);
        return null;
    }

    public List<String> getConsoleLines(String name) {
        MinecraftServer server = getServer(name);
        if (server == null) {
            return List.of();
        }
        return server.getConsoleLines();
    }

    @Transactional
    public void deleteServer(String name) {
        MinecraftServer server = getServer(name);
        if (server != null && server.isAlive()) {
            server.stop();
        }
        repository.findByName(normalizeName(name)).ifPresent(repository::delete);
        File userdir = getUserdir(name);
        if (userdir.exists()) {
            Utils.delete(userdir.getPath());
        }
    }

    public int freePort() {
        Set<Integer> used = new HashSet<>();
        for (ManagedServer server : repository.findAll()) {
            used.add(server.getPort());
        }
        for (MinecraftServer server : runningServers.values()) {
            used.add(server.port);
        }

        for (int port = properties.getMinecraft().getMinPort(); port <= properties.getMinecraft().getMaxPort(); port++) {
            if (!used.contains(port)) {
                return port;
            }
        }
        throw new IllegalStateException("利用可能なポートがありません。");
    }

    public int getMaxServers() {
        if (max > 0) {
            return max;
        }
        return properties.getMinecraft().getMaxServers();
    }

    public MinecraftServer getServer(String name) {
        MinecraftServer server = runningServers.get(normalizeName(name));
        if (server != null && !server.isAlive()) {
            runningServers.remove(server.name);
            markStopped(server.name);
            return null;
        }
        return server;
    }

    public File getUserdir(String name) {
        return new File(maindir, normalizeName(name));
    }

    public boolean isFound(String name) {
        MinecraftServer server = getServer(name);
        return server != null && server.isAlive();
    }

    @Transactional
    public void markStopped(String name) {
        String normalizedName = normalizeName(name);
        runningServers.remove(normalizedName);
        repository.findByName(normalizedName).ifPresent(server -> {
            server.setStatus(ManagedServerStatus.STOPPED);
            server.setLastStoppedAt(Instant.now());
            repository.save(server);
        });
    }

    private void cleanupStoppedProcesses() {
        for (MinecraftServer server : new ArrayList<>(runningServers.values())) {
            if (!server.isAlive()) {
                runningServers.remove(server.name);
                markStopped(server.name);
            }
        }
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("サーバー名を入力してください。");
        }
        String normalized = name.trim();
        if (!normalized.matches("^[0-9a-zA-Z_-]{1,64}$")) {
            throw new IllegalArgumentException("サーバー名は64文字以内の英数字、ハイフン、アンダースコアで入力してください。");
        }
        return normalized;
    }
}
