package org.haemin.advancement.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.haemin.advancement.AdvancementPlugin;

import java.util.List;
import java.util.Map;

public class RewardExecutor {
    public static void execute(Player p, List<Map<String,Object>> rewards) {
        if (p == null || rewards == null || rewards.isEmpty()) return;
        AdvancementPlugin pl = (AdvancementPlugin) Bukkit.getPluginManager().getPlugin("Advancement");
        for (Map<String,Object> r : rewards) {
            if (r.containsKey("money")) {
                Economy e = pl.economy();
                if (e != null) try { e.depositPlayer(p, Double.parseDouble(String.valueOf(r.get("money")))); } catch (Exception ignored) {}
            }
            if (r.containsKey("give")) {
                pl.rewards().give(p, String.valueOf(r.get("give")));
            }
            if (r.containsKey("cmd")) {
                String raw = String.valueOf(r.get("cmd"));
                String cmd = raw.replace("%player%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }
}
