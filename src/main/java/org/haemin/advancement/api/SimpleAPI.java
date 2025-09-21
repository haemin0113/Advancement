package org.haemin.advancement.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;

import java.util.UUID;

public class SimpleAPI implements AdvancementAPI {
    private final AdvancementPlugin plugin;
    public SimpleAPI(AdvancementPlugin plugin) { this.plugin = plugin; }

    @Override
    public void increment(UUID player, String goalKey, long delta) {
        GoalDef def = plugin.goals().get(goalKey);
        if (def == null) return;
        Player p = Bukkit.getPlayer(player);
        if (p == null) return;
        plugin.progress().add(p, def, delta);
    }
}
