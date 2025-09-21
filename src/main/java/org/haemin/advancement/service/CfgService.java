package org.haemin.advancement.service;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.util.Text;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CfgService {
    private final AdvancementPlugin plugin;
    private FileConfiguration cfg;

    public CfgService(AdvancementPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File dir = new File(plugin.getDataFolder(), "config");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "config.yml");
        if (!f.exists()) {
            YamlConfiguration y = new YamlConfiguration();
            y.set("general.timezone", "Asia/Seoul");
            y.set("general.save_async", true);
            y.set("messages.prefix", "<#F6D365>[Adv]&r ");
            y.set("messages.progress_add", "%prefix%&a%goal_title% 진행 +%delta% &7(%value%/%target%)");
            y.set("messages.completed", "%prefix%&b%goal_title% &a완료! &7보상을 수령하세요.");
            y.set("messages.claimed", "%prefix%&a%goal_title% 보상 수령 완료!");
            y.set("messages.cooldown", "%prefix%&c쿨다운 중: %remain%");
            y.set("messages.not_eligible", "%prefix%&7조건 미충족: %reason%");
            y.set("messages.admin_reset_ok", "%prefix%&e%goal% 진행이 초기화되었습니다.");
            try { y.save(f); } catch (IOException ignored) {}
        }
        cfg = YamlConfiguration.loadConfiguration(f);
    }

    public FileConfiguration raw() { return cfg; }
    public boolean saveAsync() { return cfg.getBoolean("general.save_async", true); }

    public String m(String key, Map<String,String> vars) {
        String p = cfg.getString("messages.prefix", "");
        String s = cfg.getString("messages." + key, key);
        if (s.contains("%prefix%")) s = s.replace("%prefix%", p);
        if (vars != null) for (Map.Entry<String,String> e : vars.entrySet())
            s = s.replace("%"+e.getKey()+"%", e.getValue()==null?"":e.getValue());
        return Text.legacy(s);
    }
}
