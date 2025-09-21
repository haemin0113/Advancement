package org.haemin.advancement.service;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;
import org.haemin.advancement.model.Progress;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventRouter implements Listener {
    private final AdvancementPlugin plugin;

    public EventRouter(AdvancementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        String id = e.getBlock().getType().name().toLowerCase(Locale.ROOT);
        handle(p, "block_break", id, 1);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String id = e.getEntityType().name().toLowerCase(Locale.ROOT);
        handle(killer, "mob_kill", id, 1);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        handle(p, "fish", "any", 1);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getRecipe()==null? null : e.getRecipe().getResult();
        String id = it==null? "any" : it.getType().name().toLowerCase(Locale.ROOT);
        int delta = it==null? 1 : Math.max(1, it.getAmount());
        handle(p, "craft", id, delta);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Player)) return;
        Player p = (Player) ent;
        ItemStack it = e.getItem().getItemStack();
        String id = it.getType().name().toLowerCase(Locale.ROOT);
        int delta = Math.max(1, it.getAmount());
        handle(p, "pickup", id, delta);
    }

    public void signalRegionStay(Player p, String regionId) {
        String id = regionId == null ? "" : regionId.toLowerCase(Locale.ROOT);
        for (GoalDef d : plugin.goals().trackedBy("region_stay")) {
            GoalDef.TrackMatcher matcher = d.trackMatchers.get("region_stay");
            if (matcher == null || !matcher.matches(id)) continue;
            plugin.progress().add(p, d, 1);
        }
    }

    private void handle(Player p, String kind, String id, long delta) {
        String normalizedKind = kind.toLowerCase(Locale.ROOT);
        String normalizedId = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (GoalDef d : plugin.goals().trackedBy(normalizedKind)) {
            if (d.type == GoalType.CHECKLIST) {
                boolean changed = false;
                Map<String, List<GoalDef.ChecklistMatcher>> matchers = d.checklistMatchers;
                if (matchers != null) {
                    List<GoalDef.ChecklistMatcher> items = matchers.get(normalizedKind);
                    if (items != null && !items.isEmpty()) {
                        Progress pr = null;
                        for (GoalDef.ChecklistMatcher matcher : items) {
                            if (!matcher.matches(normalizedId)) continue;
                            if (pr == null) pr = plugin.progress().get(p.getUniqueId(), d);
                            if ((pr.checklistBits & matcher.bit()) != 0) continue;
                            pr.checklistBits |= matcher.bit();
                            pr.value = Long.bitCount(pr.checklistBits);
                            if (!pr.completed && pr.value >= d.target) pr.completed = true;
                            changed = true;
                        }
                        if (changed) plugin.progress().flushAll();
                    }
                }
                continue;
            }

            GoalDef.TrackMatcher matcher = d.trackMatchers.get(normalizedKind);
            if (matcher == null || !matcher.matches(normalizedId)) continue;

            if (d.type == GoalType.UNIQUE) {
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                pr.value += delta;
                if (!pr.completed && pr.value >= d.target) pr.completed = true;
                plugin.progress().flushAll();
            } else {
                plugin.progress().add(p, d, delta);
            }
        }
    }
}
