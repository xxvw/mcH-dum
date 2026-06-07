package com.github.n9.mch.event.command;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public class registerCommand extends ListenerAdapter {

    String channel_prefix = "mch-";
    String category = "mch";

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if(!event.getMessage().getAuthor().isBot()){
            if(event.getMessage().getContentRaw().startsWith("register")){
                if(!event.getChannel().getName().equals("register")) {
                    return;
                }
                event.getMessage().delete().complete();
                String[] args = event.getMessage().getContentRaw().split(" ");
                if(args.length < 2) {
                    Utils.sendMessage(event.getAuthor(), "register [server name]");
                    return;
                } else {
                    String name = args[1];
                    if(!name.matches("^[0-9a-zA-Z]*$")){
                        Utils.sendMessage(event.getAuthor(), "サーバー名は英数字のみで入力してください。");
                        return;
                    }
                    Category maincg = null;
                    for (Category cg : event.getGuild().getCategories()){
                        if(cg.getName().equals(category)){
                            maincg = cg;
                            for(TextChannel ch : cg.getTextChannels()) {
                                if(ch.getName().equalsIgnoreCase(channel_prefix + name)){
                                    Utils.sendMessage(event.getAuthor(), "既に同じ名前のサーバーが存在します。別の名前に変更してください。");
                                    return;
                                }
                            }
                        }
                    }

                    if(maincg == null) {
                        Utils.sendMessage(event.getAuthor(), "カテゴリが存在しませんでした。\n管理者まで連絡してください。");
                        return;
                    }

                    /*for (TextChannel textch : maincg.getTextChannels()){
                        for (PermissionOverride po : textch.getMemberPermissionOverrides()){
                            System.out.println(textch.getName() + ": " + po.getMember().getUser().getAsTag());
                            if (po.getAllowed().contains(Permission.VIEW_CHANNEL)) {
                                Utils.sendMessage(event.getAuthor(), "あなたは既に別のサーバーのメンバーです。\nサーバーを新規に作成する場合は、既存サーバーのメンバーから脱退して下さい。");
                                return;
                            }
                        }
                    }*/


                    TextChannel ch = maincg.createTextChannel(channel_prefix + name).complete();
                    Role r = null;
                    for (Role rr : event.getGuild().getRoles()) {
                        if(rr.getName().equals("@everyone")) {
                            r = rr;
                            break;
                        }
                    }
                    if(r == null) {
                        Utils.sendMessage(event.getAuthor(), "Roleが存在しませんでした。\n管理者まで連絡してください。");
                        return;
                    }

                    ch.createPermissionOverride(r).setDeny(Permission.VIEW_CHANNEL).queue();
                    ch.createPermissionOverride(Objects.requireNonNull(event.getMember())).setAllow(Permission.VIEW_CHANNEL).queue();
                    ch.getManager().setTopic(event.getAuthor().getId()).queue();

                    event.getChannel().sendMessage("<@" + event.getAuthor().getId() + "> #" + ch.getName() + " を作成しました。").complete();
                    Message msg = ch.sendMessage("<@" + event.getAuthor().getId() + "> サーバーの作成が完了しました。\n\n" +
                            "このチャンネルでは以下コマンドを使用することができます。\n\n```\n" +
                            "start サーバー起動の信号を送る\n" +
                            "stop  サーバー停止の信号を送る\n" +
                            "cmd [コマンド] [コマンド]をサーバーに送信する（起動中のみ）\n" +
                            "（例: cmd op abc 「abcを管理者にする」）\n" +
                            "leave このチャンネルのメンバーから脱退する\n" +
                            "add [ユーザーID] [ユーザーID]のユーザーをこのチャンネルのメンバーに追加する。\n" +
                            "ユーザーIDに関してはMinecraftHosting Botのメッセージにgetidと入力することや、Discordの開発者モードで確認することができます。\n" +
                            "（例: 902495204358123521）\n```\n\n" +
                            "現在βテストであるため、負荷状態などで起動できる場合とできない場合があります。\n" +
                            "ご了承ください。\n" +
                            "サーバーを一つにまとめている機構のため、毎回IPが変更されます。\n" +
                            "「サーバーを追加」ではなく「直接接続」をおすすめします。\n" +
                            "3分間誰もサーバーにログインしていない場合サーバーは自動で停止します。").complete();
                    McHostApplication.manager.createServer(ch.getName(), "discord", event.getAuthor().getId(), ch.getId());
                    File userdir = McHostApplication.manager.getUserdir(ch.getName());
                    userdir.mkdir();
                    msg.pin().complete();
                }
            }
        }
    }
}
