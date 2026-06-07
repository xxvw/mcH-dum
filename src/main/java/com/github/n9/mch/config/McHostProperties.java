package com.github.n9.mch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mch")
public class McHostProperties {

    private final Discord discord = new Discord();
    private final Minecraft minecraft = new Minecraft();

    public Discord getDiscord() {
        return discord;
    }

    public Minecraft getMinecraft() {
        return minecraft;
    }

    public static class Discord {
        private String token;
        private final ServerInfo serverInfo = new ServerInfo();

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public ServerInfo getServerInfo() {
            return serverInfo;
        }
    }

    public static class ServerInfo {
        private String guildName = "99B Works";
        private String channelName = "serverinfo";

        public String getGuildName() {
            return guildName;
        }

        public void setGuildName(String guildName) {
            this.guildName = guildName;
        }

        public String getChannelName() {
            return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }
    }

    public static class Minecraft {
        private String dataDir = "./data";
        private String javaCommand = "java";
        private String memory = "-Xmx750M";
        private int minPort = 25000;
        private int maxPort = 26000;
        private int maxServers = 50;
        private boolean autoUpdatePaper = true;

        public String getDataDir() {
            return dataDir;
        }

        public void setDataDir(String dataDir) {
            this.dataDir = dataDir;
        }

        public String getJavaCommand() {
            return javaCommand;
        }

        public void setJavaCommand(String javaCommand) {
            this.javaCommand = javaCommand;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public int getMinPort() {
            return minPort;
        }

        public void setMinPort(int minPort) {
            this.minPort = minPort;
        }

        public int getMaxPort() {
            return maxPort;
        }

        public void setMaxPort(int maxPort) {
            this.maxPort = maxPort;
        }

        public int getMaxServers() {
            return maxServers;
        }

        public void setMaxServers(int maxServers) {
            this.maxServers = maxServers;
        }

        public boolean isAutoUpdatePaper() {
            return autoUpdatePaper;
        }

        public void setAutoUpdatePaper(boolean autoUpdatePaper) {
            this.autoUpdatePaper = autoUpdatePaper;
        }
    }
}
