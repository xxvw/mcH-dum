package com.github.n9.mch.event.command;

import com.github.n9.mch.McHostApplication;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class startCommand extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().equals("start")) {
                McHostApplication.manager.newServer(event.getChannel().getName(), event.getChannel());
            }
        }
    }

}
