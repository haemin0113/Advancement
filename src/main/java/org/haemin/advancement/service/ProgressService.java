package org.haemin.advancement.service;

import org.bukkit.entity.Player;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.Progress;
import org.haemin.advancement.util.ItemSpec;
import org.haemin.advancement.util.Text;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProgressService {
    private final AdvancementPlugin plugin;
    private final Map<UUID, Map<String, Progress>> cache = new ConcurrentHashMap<>();
    private final YamlPlayerStore store;

    public ProgressService(AdvancementPlugin plugin) {
        this.plugin = plugin;
        this.store = new YamlPlayerStore(plugin);
    }

    private Progress loadRaw(UUID uuid, String key, long target) {
        Map<String, Progress> m = cache.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        Progress p = m.get(key);
        if (p == null) { p = store.load(uuid, key, target); m.put(key, p); }
        if (p.target != target) p.target = target;
        return p;
    }

    public Progress get(UUID uuid, GoalDef def) {
        Progress p = loadRaw(uuid, def.key, def.target);
        String current = plugin.resets().periodIdFor(def.reset);
        if (!Objects.equals(current, p.period)) {
            p.period = current;
            p.value = 0; p.completed = false;
            p.checklistBits = 0; p.streak = 0; p.lastTs = 0; p.best = 0;
            p.ttStart = 0; p.ttAccum = 0; p.ttCooldownUntil = 0; p.ttBest = 0;
            store.save(uuid, def.key, p, plugin.cfg().saveAsync());
        }
        return p;
    }

    public boolean isActive(Player p, GoalDef def) {
        if (def == null) return false;
        if (def.requires != null && !def.requires.isEmpty()) {
            boolean ok = true;
            for (String r : def.requires) {
                Progress pr = get(p.getUniqueId(), r, 0);
                if (pr == null || !pr.completed) { ok = false; break; }
            }
            if (!ok) {
                if (def.activateIfHas != null && !def.activateIfHas.isEmpty()) {
                    for (String s : def.activateIfHas) if (ItemSpec.has(p, s)) return true;
                }
                return false;
            }
        }
        return true;
    }

    public double calcMultiplier(Player p, GoalDef def) {
        if (def.boosts == null || def.boosts.isEmpty()) return 1.0;
        double mul = 1.0;
        for (Map<String,Object> b : def.boosts) {
            String cond = String.valueOf(b.getOrDefault("if_has",""));
            if (cond.isEmpty()) continue;
            if (ItemSpec.has(p, cond)) {
                double m = 1.0;
                try { m = Double.parseDouble(String.valueOf(b.getOrDefault("multiplier", "1.0"))); } catch (Exception ignored) {}
                mul *= m;
            }
        }
        return mul;
    }

    public long calcFlatAdd(Player p, GoalDef def) {
        if (def.boosts == null || def.boosts.isEmpty()) return 0;
        long add = 0;
        for (Map<String,Object> b : def.boosts) {
            String cond = String.valueOf(b.getOrDefault("if_has",""));
            if (cond.isEmpty()) continue;
            if (ItemSpec.has(p, cond)) {
                try { add += Long.parseLong(String.valueOf(b.getOrDefault("add", "0"))); } catch (Exception ignored) {}
            }
        }
        return add;
    }

    public void add(Player player, GoalDef def, long delta) {
        if (player == null || def == null || delta <= 0) return;
        if (!isActive(player, def)) return;
        Progress pr = get(player.getUniqueId(), def);
        boolean repeat = plugin.resets().isRepeat(def.reset);
        if (pr.completed && !repeat) return;

        double mul = calcMultiplier(player, def);
        long bonus = calcFlatAdd(player, def);
        long gain = Math.max(1, Math.round(delta * mul)) + Math.max(0, bonus);

        pr.value = Math.max(0, pr.value + gain);
        if (!pr.completed && pr.value >= def.target) {
            pr.completed = true;
            Text.msg(player, plugin.cfg().m("completed", Map.of("goal_title", def.title)));
            plugin.sounds().play(player, "completed");
        } else {
            Text.msg(player, plugin.cfg().m("progress_add", Map.of(
                    "goal_title", def.title, "delta", String.valueOf(gain),
                    "value", String.valueOf(pr.value), "target", String.valueOf(pr.target))));
            plugin.sounds().play(player, "progress_add");
        }
        store.save(player.getUniqueId(), def.key, pr, plugin.cfg().saveAsync());
    }

    public boolean claim(Player player, GoalDef def) {
        Progress pr = get(player.getUniqueId(), def);
        if (!pr.completed) return false;

        RewardExecutor.execute(player, def.rewards);
        Text.msg(player, plugin.cfg().m("claimed", Map.of("goal_title", def.title)));
        plugin.sounds().play(player, "claimed");

        boolean repeat = plugin.resets().isRepeat(def.reset);
        if (repeat) { pr.value = 0; pr.completed = false; }
        else { pr.value = pr.target; pr.completed = true; }
        store.save(player.getUniqueId(), def.key, pr, plugin.cfg().saveAsync());
        return true;
    }

    public void flushAll() { store.flush(); }

    public Progress get(UUID uuid, String goalKey, long target) {
        GoalDef def = plugin.goals().get(goalKey);
        if (def == null) { def = new GoalDef(); def.key = goalKey; def.title = goalKey; def.target = target; def.reset = "daily"; }
        return get(uuid, def);
    }
    public Progress get(UUID uuid, String goalKey) {
        GoalDef def = plugin.goals().get(goalKey);
        return (def == null) ? loadRaw(uuid, goalKey, 0) : get(uuid, def);
    }
    public boolean claim(Player p, String goalKey) {
        GoalDef def = plugin.goals().get(goalKey);
        return def != null && claim(p, def);
    }
}
