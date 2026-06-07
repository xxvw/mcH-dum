package com.github.n9.mch.web;

import com.github.n9.mch.McHostApplication;
import com.github.n9.mch.config.McHostProperties;
import com.github.n9.mch.minecraft.MinecraftError;
import com.github.n9.mch.minecraft.ServerManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final ServerManager serverManager;
    private final McHostProperties properties;

    public AdminController(ServerManager serverManager, McHostProperties properties) {
        this.serverManager = serverManager;
        this.properties = properties;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String index(Model model) {
        model.addAttribute("servers", serverManager.listServers());
        model.addAttribute("runningCount", serverManager.getAll().size());
        model.addAttribute("maxServers", serverManager.getMaxServers());
        model.addAttribute("publicIp", McHostApplication.IP);
        model.addAttribute("discordEnabled", properties.getDiscord().getToken() != null && !properties.getDiscord().getToken().isBlank());
        return "admin";
    }

    @PostMapping("/admin/servers")
    public String create(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "false") boolean start,
            RedirectAttributes redirectAttributes
    ) {
        try {
            serverManager.createServer(name, "web", null, null);
            if (start) {
                addResult(redirectAttributes, serverManager.startServer(name), "サーバーを登録して起動しました。");
            } else {
                redirectAttributes.addFlashAttribute("message", "サーバーを登録しました。");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/servers/{name}/start")
    public String start(@PathVariable String name, RedirectAttributes redirectAttributes) {
        try {
            addResult(redirectAttributes, serverManager.startServer(name), "サーバーを起動しました。");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/servers/{name}/stop")
    public String stop(@PathVariable String name, RedirectAttributes redirectAttributes) {
        addResult(redirectAttributes, serverManager.stopServer(name), "サーバーに停止コマンドを送信しました。");
        return "redirect:/admin";
    }

    @PostMapping("/admin/servers/{name}/command")
    public String command(@PathVariable String name, @RequestParam String command, RedirectAttributes redirectAttributes) {
        if (command == null || command.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "コマンドを入力してください。");
            return "redirect:/admin";
        }
        addResult(redirectAttributes, serverManager.commandServer(name, command), "コマンドを送信しました。");
        return "redirect:/admin";
    }

    @PostMapping("/admin/servers/{name}/delete")
    public String delete(@PathVariable String name, RedirectAttributes redirectAttributes) {
        serverManager.deleteServer(name);
        redirectAttributes.addFlashAttribute("message", "サーバーを削除しました。");
        return "redirect:/admin";
    }

    private void addResult(RedirectAttributes redirectAttributes, MinecraftError error, String successMessage) {
        if (error == null) {
            redirectAttributes.addFlashAttribute("message", successMessage);
        } else {
            redirectAttributes.addFlashAttribute("error", error.getMsg());
        }
    }
}
