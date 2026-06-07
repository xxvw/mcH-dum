package com.github.n9.mch.minecraft;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.Utils;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MinecraftServer {

    private final Consumer<String> notifier;
    private final Runnable stoppedCallback;
    private final File datadir;
    private final String[] command;

    public final String name;
    public final int port;
    public Process process;
    private Thread thread;
    public final File userdir;

    public MinecraftServer(String name, TextChannel channel, int port, String... command) {
        this(name, port, new File(System.getProperty("user.dir"), "data"), message -> {
            if (channel != null) {
                channel.sendMessage(message).queue();
            }
        }, () -> {
        }, command);
    }

    public MinecraftServer(String name, int port, File datadir, Consumer<String> notifier, Runnable stoppedCallback, String... command) {
        this.port = port;
        this.name = name;
        this.notifier = notifier == null ? message -> {
        } : notifier;
        this.stoppedCallback = stoppedCallback == null ? () -> {
        } : stoppedCallback;
        this.datadir = datadir;
        this.command = command;
        this.userdir = new File(datadir, name);
        if (!this.userdir.exists()) {
            this.userdir.mkdirs();
        }
    }

    public static File genUserDir(String name) {
        return new File(new File(System.getProperty("user.dir"), "data"), name);
    }

    public boolean isAlive() {
        return process != null && process.isAlive() && thread != null && thread.isAlive();
    }

    public synchronized MinecraftError start() {
        if (isAlive()) {
            return MinecraftError.STARTING;
        }

        try {
            first();
        } catch (IOException e) {
            e.printStackTrace();
            return MinecraftError.PROCESS_ERROR;
        }

        thread = new Thread(() -> {
            ProcessBuilder builder = new ProcessBuilder();
            if (Utils.getOS() == Utils.OS.WINDOWS) {
                builder.command(windowsCommand());
            } else {
                builder.command(command);
            }
            builder.directory(userdir);

            try {
                process = builder.start();
                System.out.println("StartServer " + name + " p:" + port);
                notifier.accept("サーバーが起動しました。\nIP: " + McHostApplication.IP + ":" + port);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                if (process != null) {
                    process.destroy();
                }
                System.out.println("End Process " + name);
                notifier.accept("サーバーが停止しました。");
                stoppedCallback.run();
            }
        }, "minecraft-" + name);
        thread.start();
        return null;
    }

    private List<String> windowsCommand() {
        String[] windows = Arrays.copyOf(command, command.length);
        for (int i = 0; i < windows.length; i++) {
            if ("java".equals(windows[i])) {
                windows[i] = "java";
            }
        }
        return Arrays.asList(windows);
    }

    private void first() throws IOException {
        deleteIfExists(new File(userdir, "logs"));
        deleteIfExists(new File(userdir, "cache"));
        deleteIfExists(new File(userdir, "server.jar"));
        deleteIfExists(new File(userdir, "server.properties"));
        deleteIfExists(new File(userdir, "plugins"));

        File base = new File(datadir, "base");
        copyFileIfExists(new File(base, "server.jar"), new File(userdir, "server.jar"));
        copyDirectoryIfExists(new File(base, "cache"), new File(userdir, "cache"));
        copyDirectoryIfExists(new File(base, "plugins"), new File(userdir, "plugins"));

        File eula = new File(userdir, "eula.txt");
        if (!eula.exists()) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(eula)))) {
                pw.println("eula=true");
            }
        }

        File properties = new File(userdir, "server.properties");
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(properties), StandardCharsets.UTF_8)))) {
            for (String s : serverProperties()) {
                pw.println(s);
            }
        }
    }

    private void deleteIfExists(File file) {
        if (file.exists()) {
            Utils.delete(file.getPath());
        }
    }

    private void copyFileIfExists(File source, File target) throws IOException {
        if (source.exists()) {
            try {
                Files.copy(source.toPath(), target.toPath());
            } catch (FileAlreadyExistsException ignored) {
            }
        }
    }

    private void copyDirectoryIfExists(File source, File target) throws IOException {
        if (!source.exists()) {
            return;
        }
        try (Stream<java.nio.file.Path> paths = Files.walk(source.toPath())) {
            paths.forEach(path -> {
                try {
                    File dest = target.toPath().resolve(source.toPath().relativize(path)).toFile();
                    if (path.toFile().isDirectory()) {
                        dest.mkdirs();
                    } else {
                        Files.copy(path, dest.toPath());
                    }
                } catch (FileAlreadyExistsException ignored) {
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private List<String> serverProperties() {
        return List.of(
                "spawn-protection=16",
                "max-tick-time=60000",
                "query.port=" + port,
                "generator-settings=",
                "sync-chunk-writes=true",
                "force-gamemode=false",
                "allow-nether=true",
                "enforce-whitelist=false",
                "gamemode=survival",
                "broadcast-console-to-ops=true",
                "enable-query=false",
                "player-idle-timeout=0",
                "difficulty=easy",
                "spawn-monsters=true",
                "broadcast-rcon-to-ops=true",
                "op-permission-level=4",
                "pvp=true",
                "entity-broadcast-range-percentage=100",
                "snooper-enabled=true",
                "level-type=default",
                "hardcore=false",
                "enable-status=true",
                "enable-command-block=true",
                "max-players=10",
                "network-compression-threshold=256",
                "resource-pack-sha1=",
                "max-world-size=29999984",
                "function-permission-level=2",
                "rcon.port=25575",
                "server-port=" + port,
                "debug=false",
                "server-ip=",
                "spawn-npcs=true",
                "allow-flight=true",
                "level-name=world",
                "view-distance=6",
                "resource-pack=",
                "spawn-animals=true",
                "white-list=false",
                "rcon.password=",
                "generate-structures=true",
                "max-build-height=256",
                "online-mode=true",
                "level-seed=",
                "use-native-transport=true",
                "prevent-proxy-connections=false",
                "enable-jmx-monitoring=false",
                "enable-rcon=false",
                "motd=A Minecraft Server"
        );
    }

    public MinecraftError stop() {
        if (!isAlive()) {
            return MinecraftError.NOT_FOUND;
        }
        command("save-all", "stop");
        return null;
    }

    public void command(String... commands) {
        if (!isAlive()) {
            return;
        }
        PrintWriter out = new PrintWriter(process.getOutputStream());
        for (String cmd : commands) {
            out.print(cmd);
            out.print(System.lineSeparator());
            out.flush();
        }
    }
}
