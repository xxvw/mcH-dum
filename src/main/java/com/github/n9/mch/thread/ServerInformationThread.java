package com.github.n9.mch.thread;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.Utils;
import com.github.n9.mch.config.McHostProperties;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

public class ServerInformationThread extends Thread {

    Message message;
    public ServerInformationThread(McHostProperties properties) throws IOException {
        Guild g = McHostApplication.jda.getGuildsByName(properties.getDiscord().getServerInfo().getGuildName(), false).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("server info guild not found"));
        TextChannel ch = g.getTextChannelsByName(properties.getDiscord().getServerInfo().getChannelName(), false).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("server info channel not found"));
        URL url = new URL("https://papermc.io/api/v2/projects/paper");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestMethod("GET");
        http.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        String xml = "", line = "";
        while((line = reader.readLine()) != null)
            xml += line;
        reader.close();
        Map<String, Object> obj = Utils.jsonStringToMap(xml);
        ArrayList<String> vers = (ArrayList<String>) obj.get("versions");
        String version = vers.get(vers.size()-1);
        MessageEmbed em = new EmbedBuilder()
                        .setDescription("現在のサーバーステータスです。3分間に一度更新されます。")
                        .setColor(new Color(2293710))
                        .setAuthor("Server Information", null, null)
                        .addField("サーバーバージョン", version, false)
                        .addField("起動中のサーバー数", String.valueOf(McHostApplication.manager.getAll().size()) + " / " + McHostApplication.manager.max, true)
                        .addField("合計登録サーバー数", String.valueOf(McHostApplication.manager.maindir.listFiles().length), true)
                        .build();
        this.message = ch.sendMessage(em).complete();
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Thread.sleep(1000 * 60 * 3);

                URL url = new URL("https://papermc.io/api/v2/projects/paper");
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("GET");
                http.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
                String xml = "", line = "";
                while ((line = reader.readLine()) != null)
                    xml += line;
                reader.close();
                Map<String, Object> obj = Utils.jsonStringToMap(xml);
                ArrayList<String> vers = (ArrayList<String>) obj.get("versions");
                String version = vers.get(vers.size() - 1);
                File[] files = McHostApplication.manager.maindir.listFiles();
                this.message.editMessage(
                        new EmbedBuilder()
                                .setDescription("現在のサーバーステータスです。3分間に一度更新されます。")
                                .setColor(new Color(2293710))
                                .setAuthor("Server Information", null, null)
                                .addField("サーバーバージョン", version, false)
                                .addField("起動中のサーバー数", String.valueOf(McHostApplication.manager.getAll().size()) + " / " + McHostApplication.manager.max, true)
                                .addField("合計登録サーバー数", String.valueOf(files == null ? 0 : files.length), true)
                                .build()
                ).complete();
            } catch (InterruptedException e) {
                interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
