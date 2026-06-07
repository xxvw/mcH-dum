package com.github.n9.mch.event.command;

import com.github.n9.mch.ConsoleLogger;
import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.Utils;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class leaveCommand extends ListenerAdapter {
    String channel_prefix = "mch-";
    String category = "mch";



    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().equalsIgnoreCase("leave")) {
                if (event.getChannel().getName().startsWith(channel_prefix)) {
                    event.getChannel().sendMessage("<@" + event.getMember().getIdLong() + "> \nleave confirmでメンバーから脱退することができます。\nサーバーを初めに作成したユーザーが脱退した場合サーバーは削除されます。\nその他のユーザーが脱退した場合このチャンネルにはアクセスできなくなります。").complete();
                    return;
                }
            } else if (event.getMessage().getContentRaw().equalsIgnoreCase("leave confirm")) {
                if (event.getChannel().getName().startsWith(channel_prefix)) {
                    Utils.sendMessage(event.getAuthor(), event.getChannel().getName() + "のメンバーから外れました。");
                    event.getChannel().sendMessage("<@" + event.getAuthor().getIdLong() + "> がメンバーから脱退しました。").complete();
                    Objects.requireNonNull(event.getChannel().getPermissionOverride(Objects.requireNonNull(event.getMember()))).delete().complete();
                    if (event.getChannel().getTopic().equals(event.getAuthor().getId())) {
                        File userdir = McHostApplication.manager.getUserdir(event.getChannel().getName());
                        if (userdir.exists()) Utils.delete(userdir.getPath());
                        if (userdir.exists()) ConsoleLogger.info("Delete userdir" + userdir.getPath());
                        McHostApplication.manager.deleteServer(event.getChannel().getName());
                        for (PermissionOverride pov : event.getChannel().getMemberPermissionOverrides()) {
                            try {
                                Utils.sendMessage(pov.getMember().getUser(), event.getChannel() + "が" + event.getMember().getUser().getAsTag() + "によって削除されました。");
                            } catch (NullPointerException ex) {
                                continue;
                            }
                        }
                        event.getChannel().delete().complete();
                    }
                    return;
                } else {
                    return;
                }
            }
        }
    }
}
