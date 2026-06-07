package com.github.n9.mch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.entities.User;

import java.io.File;
import java.util.Map;

public class Utils {

    public static int getRandom(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " greater than max " + max);
        }
        return (int) ( (long) min + Math.random() * ((long)max - min + 1));
    }

    public static Map<String, Object> jsonStringToMap(String json) {
        java.util.Map<String, Object> map = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            map = mapper.readValue(json, new TypeReference<Map<String, Object>>(){});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void delete(String path) {
        File filePath = new File(path);
        if (!filePath.exists()) {
            return;
        }
        if (filePath.isFile()) {
            filePath.delete();
            return;
        }
        String[] list = filePath.list();
        if (list == null) {
            filePath.delete();
            return;
        }
        for(String file : list) {
            File f = new File(path + File.separator + file);
            if(f.isDirectory()) {
                delete(path + File.separator + file);
            } else {
                f.delete();
            }
        }
        filePath.delete();
    }

    public static void sendMessage(User user, String msg) {
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessage("```\n" + msg + "\n```"))
                .queue();
    }

    public enum OS {
        WINDOWS, LINUX, MAC, SOLARIS
    };// Operating systems.

    private static OS os = null;

    public static OS getOS() {
        if (os == null) {
            String operSys = System.getProperty("os.name").toLowerCase();
            if (operSys.contains("win")) {
                os = OS.WINDOWS;
            } else if (operSys.contains("nix") || operSys.contains("nux")
                    || operSys.contains("aix")) {
                os = OS.LINUX;
            } else if (operSys.contains("mac")) {
                os = OS.MAC;
            } else if (operSys.contains("sunos")) {
                os = OS.SOLARIS;
            }
        }
        return os;
    }

}
