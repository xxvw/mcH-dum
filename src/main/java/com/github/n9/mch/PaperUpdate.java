package com.github.n9.mch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaperUpdate {

    public static void run() {
        try {
            run(new File(System.getProperty("user.dir"), "data"));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void run(File dataDir) throws IOException, InterruptedException {
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
        String latest = vers.get(vers.size() - 1);
        url = new URL("https://papermc.io/api/v2/projects/paper/versions/" + latest);
        http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        http.connect();
        reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        xml = "";
        line = "";
        while ((line = reader.readLine()) != null)
            xml += line;
        reader.close();
        obj = Utils.jsonStringToMap(xml);
        int build = ((List<Integer>) obj.get("builds")).get(((List<Integer>) obj.get("builds")).size() - 1);
        url = new URL("https://papermc.io/api/v2/projects/paper/versions/" + latest + "/builds/" + build);
        http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("GET");
        http.connect();
        reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        xml = "";
        line = "";
        while ((line = reader.readLine()) != null)
            xml += line;
        reader.close();
        obj = Utils.jsonStringToMap(xml);
        String jarName = ((String) ((LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) obj.get("downloads")).get("application")).get("name"));
        String command = "https://papermc.io/api/v2/projects/paper/versions/" + latest + "/builds/" + build + "/downloads/" + jarName;
        ProcessBuilder builder = new ProcessBuilder();
        File f = new File(dataDir, "base");
        f.mkdirs();
        builder.directory(f);
        builder.command("curl", command, "-o", "server.jar");
        if (new File(f, "server.jar").exists()) new File(f, "server.jar").delete();
        System.out.println("Start Update!");
        Process p = builder.start();
        p.waitFor();
        p.destroy();
        System.out.println("Finish Update!");
    }
}
