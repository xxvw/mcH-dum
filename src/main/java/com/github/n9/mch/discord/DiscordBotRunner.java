package com.github.n9.mch.discord;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.config.McHostProperties;
import com.github.n9.mch.event.ReadyListener;
import com.github.n9.mch.event.command.addCommand;
import com.github.n9.mch.event.command.getIdCommand;
import com.github.n9.mch.event.command.leaveCommand;
import com.github.n9.mch.event.command.registerCommand;
import com.github.n9.mch.event.command.servercmdCommand;
import com.github.n9.mch.event.command.startCommand;
import com.github.n9.mch.event.command.stopCommand;
import com.github.n9.mch.thread.ServerInformationThread;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DiscordBotRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiscordBotRunner.class);

    private final McHostProperties properties;

    public DiscordBotRunner(McHostProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String token = properties.getDiscord().getToken();
        if (token == null || token.isBlank()) {
            log.info("Discord token is not configured. Web admin remains available.");
            return;
        }

        JDABuilder builder = JDABuilder.createDefault(token);
        builder.addEventListeners(new ReadyListener());
        builder.addEventListeners(new addCommand());
        builder.addEventListeners(new getIdCommand());
        builder.addEventListeners(new leaveCommand());
        builder.addEventListeners(new registerCommand());
        builder.addEventListeners(new servercmdCommand());
        builder.addEventListeners(new startCommand());
        builder.addEventListeners(new stopCommand());

        McHostApplication.jda = builder.build().awaitReady();
        try {
            new ServerInformationThread(properties).start();
        } catch (RuntimeException e) {
            log.warn("Discord server information thread was not started: {}", e.getMessage());
        }
    }
}
