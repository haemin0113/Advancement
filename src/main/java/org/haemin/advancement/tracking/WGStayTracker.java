package org.haemin.advancement.tracking;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.haemin.advancement.AdvancementPlugin;

import java.util.*;

public class WGStayTracker {
    private final AdvancementPlugin plugin;
    private BukkitTask task;
    private final Map<UUID, Map<String, Integer>> stay = new HashMap<>();

    public WGStayTracker(AdvancementPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) { try { task.cancel(); } catch (Throwable ignored) {} task = null; }
        stay.clear();
    }

    private void tick() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Set<String> ids = currentRegions(query, p);
            if (ids.isEmpty()) { stay.remove(p.getUniqueId()); continue; }
            Map<String,Integer> map = stay.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
            for (String id : ids) {
                int sec = map.getOrDefault(id, 0) + 1;
                map.put(id, sec);
                plugin.router().signalRegionStay(p, id);
            }
            map.keySet().removeIf(k -> !ids.contains(k));
        }
    }

    private Set<String> currentRegions(RegionQuery query, Player p) {
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(p.getLocation()));
        if (set == null || set.size() == 0) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        set.getRegions().forEach(r -> out.add(r.getId().toLowerCase(Locale.ROOT)));
        return out;
    }
}
