package org.haemin.advancement.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.Progress;
import org.haemin.advancement.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class YamlPlayerStore {
    private final AdvancementPlugin plugin;
    private final ConcurrentHashMap<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();

    public YamlPlayerStore(AdvancementPlugin plugin) {
        this.plugin = plugin;
    }

    private File file(UUID uuid) {
        File dir = new File(plugin.getDataFolder(), "data/players");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, uuid.toString() + ".yml");
    }

    private YamlConfiguration conf(UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> YamlConfiguration.loadConfiguration(file(u)));
    }

    public Progress load(UUID uuid, String key, long target) {
        YamlConfiguration c = conf(uuid);
        String base = "goals." + key + ".";
        Progress p = new Progress();
        p.value = c.getLong(base + "value", 0);
        p.target = target <= 0 ? c.getLong(base + "target", target) : target;
        p.completed = c.getBoolean(base + "completed", false);
        p.period = c.getString(base + "period", null);
        p.checklistBits = c.getLong(base + "checklist_bits", 0);
        p.streak = c.getInt(base + "streak", 0);
        p.lastTs = c.getLong(base + "last_ts", 0);
        p.best = c.getInt(base + "best", 0);
        p.ttStart = c.getLong(base + "tt_start", 0);
        p.ttAccum = c.getLong(base + "tt_accum", 0);
        p.ttCooldownUntil = c.getLong(base + "tt_cd_until", 0);
        p.ttBest = c.getLong(base + "tt_best", 0);
        p.uniques = new java.util.LinkedHashSet<>(c.getStringList(base + "uniques"));
        return p;
    }

    public void save(UUID uuid, String key, Progress p, boolean async) {
        Runnable r = () -> {
            try {
                YamlConfiguration c = conf(uuid);
                String base = "goals." + key + ".";
                c.set(base + "value", p.value);
                c.set(base + "target", p.target);
                c.set(base + "completed", p.completed);
                c.set(base + "period", p.period);
                c.set(base + "checklist_bits", p.checklistBits);
                c.set(base + "streak", p.streak);
                c.set(base + "last_ts", p.lastTs);
                c.set(base + "best", p.best);
                c.set(base + "tt_start", p.ttStart);
                c.set(base + "tt_accum", p.ttAccum);
                c.set(base + "tt_cd_until", p.ttCooldownUntil);
                c.set(base + "tt_best", p.ttBest);
                c.set(base + "uniques", p.uniques == null ? null : new java.util.ArrayList<>(p.uniques));
                File f = file(uuid);
                File parent = f.getParentFile();
                if (!parent.exists()) parent.mkdirs();
                c.save(f);
            } catch (IOException e) {
                Log.severe("Failed to save player data: " + uuid + " (" + e.getMessage() + ")");
            } catch (Throwable t) {
                Log.severe("Unexpected save error for " + uuid + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        };
        if (async) plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
        else plugin.getServer().getScheduler().runTask(plugin, r);
    }

    public void flush() {
        for (UUID u : cache.keySet()) {
            YamlConfiguration c = cache.get(u);
            if (c == null) continue;
            try { c.save(file(u)); } catch (IOException e) { Log.severe("Flush error: " + u + " (" + e.getMessage() + ")"); }
        }
    }
}
