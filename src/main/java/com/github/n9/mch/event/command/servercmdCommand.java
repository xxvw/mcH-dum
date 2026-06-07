package com.github.n9.mch.event.command;

import com.github.n9.mch.McHostApplication;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class servercmdCommand extends ListenerAdapter {

    String channel_prefix = "mch-";
    String category = "mch";



    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().startsWith("cmd")) {
                String cmd = event.getMessage().getContentRaw().replaceFirst("cmd", "");
                if (cmd.isEmpty() || cmd.isBlank()) {
                    event.getChannel().sendMessage("cmd [commands...]").complete();
                    return;
                } else if (McHostApplication.manager.commandServer(event.getChannel().getName(), cmd) == null) {
                    event.getChannel().sendMessage("コマンドを送信しました。").complete();
                } else {
                    event.getChannel().sendMessage("サーバーが見つかりませんでした。").complete();
                    return;
                }
            }
        }
    }
}
