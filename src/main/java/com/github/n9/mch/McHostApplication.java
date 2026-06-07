package com.github.n9.mch;

import com.github.n9.mch.config.McHostProperties;
import com.github.n9.mch.minecraft.MinecraftServer;
import com.github.n9.mch.minecraft.ServerManager;
import net.dv8tion.jda.api.JDA;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

@SpringBootApplication
@EnableConfigurationProperties(McHostProperties.class)
public class McHostApplication {

    public static JDA jda;
    public static ServerManager manager;
    public static String IP = "127.0.0.1";
    public static ConsoleLogger logger;

    public static void main(String[] args) {
        SpringApplication.run(McHostApplication.class, args);
    }

    @Component
    @Order(0)
    static class Bootstrap implements ApplicationRunner {

        private final ServerManager serverManager;
        private final McHostProperties properties;

        Bootstrap(ServerManager serverManager, McHostProperties properties) {
            this.serverManager = serverManager;
            this.properties = properties;
        }

        @Override
        public void run(ApplicationArguments args) {
            manager = serverManager;
            logger = new ConsoleLogger();
            IP = resolvePublicIp();

            if (properties.getMinecraft().isAutoUpdatePaper()) {
                try {
                    PaperUpdate.run(new File(properties.getMinecraft().getDataDir()));
                } catch (Exception e) {
                    throw new IllegalStateException("Paper server jar update failed.", e);
                }
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (MinecraftServer server : manager.getAll()) {
                    if (server.process != null) {
                        server.process.destroy();
                    }
                }
            }));
        }

        private String resolvePublicIp() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream()))) {
                return in.readLine();
            } catch (Exception e) {
                return "127.0.0.1";
            }
        }
    }
}
