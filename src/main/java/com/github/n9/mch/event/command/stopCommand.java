package com.github.n9.mch.event.command;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.minecraft.MinecraftError;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class stopCommand extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().equals("stop")) {
                MinecraftError error = McHostApplication.manager.stopServer(event.getChannel().getName());
                if (error != null) {
                    event.getChannel().sendMessage("Result: " + error.getMsg()).complete();
                }
            }
        }
    }

}
