package org.haemin.advancement.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.Progress;

public class PapiHook extends PlaceholderExpansion {
    private final AdvancementPlugin plugin;

    public PapiHook(AdvancementPlugin plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "adv"; }
    @Override public String getAuthor() { return "adv"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override public String onPlaceholderRequest(Player p, String params) {
        if (p == null || params == null) return "";
        try {
            if (params.startsWith("value::")) {
                String k = params.substring(7);
                GoalDef d = plugin.goals().get(k);
                if (d == null) return "0";
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                return String.valueOf(pr.value);
            }
            if (params.startsWith("target::")) {
                String k = params.substring(8);
                GoalDef d = plugin.goals().get(k);
                if (d == null) return "0";
                return String.valueOf(d.target);
            }
            if (params.startsWith("percent::")) {
                String k = params.substring(9);
                GoalDef d = plugin.goals().get(k);
                if (d == null) return "0";
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                return String.valueOf(pr.percent());
            }
            if (params.startsWith("state::")) {
                String k = params.substring(7);
                GoalDef d = plugin.goals().get(k);
                if (d == null) return "NA";
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                return pr.completed ? "COMPLETED" : "ACTIVE";
            }
            if (params.startsWith("name::")) {
                String k = params.substring(6);
                GoalDef d = plugin.goals().get(k);
                return d == null ? "" : d.title;
            }
        } catch (Throwable ignored) {}
        return "";
    }
}
