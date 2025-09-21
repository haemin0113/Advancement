package org.haemin.advancement.service;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.haemin.advancement.AdvancementPlugin;

import java.io.File;

public class SoundsService {
    private final AdvancementPlugin plugin;
    private YamlConfiguration yml;

    public SoundsService(AdvancementPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File f = new File(new File(plugin.getDataFolder(),"config"), "sounds.yml");
        if (!f.exists()) {
            try { plugin.saveResource("config/sounds.yml", false); } catch (Throwable ignored) {}
        }
        yml = YamlConfiguration.loadConfiguration(f);
    }

    public void play(Player p, String key) {
        if (p == null || key == null) return;
        Sound fallback = Sound.UI_BUTTON_CLICK;
        try {
            ConfigurationSection s = yml.getConfigurationSection("events." + key);
            if (s == null) { p.playSound(p.getLocation(), fallback, 0.7f, 1.0f); return; }
            String name = s.getString("sound", fallback.name());
            float v = (float) s.getDouble("volume", 1.0);
            float pit = (float) s.getDouble("pitch", 1.0);
            Sound snd;
            try { snd = Sound.valueOf(name); } catch (IllegalArgumentException e) { snd = fallback; }
            p.playSound(p.getLocation(), snd, v, pit);
        } catch (Throwable t) {
            p.playSound(p.getLocation(), fallback, 0.7f, 1.0f);
        }
    }
}
