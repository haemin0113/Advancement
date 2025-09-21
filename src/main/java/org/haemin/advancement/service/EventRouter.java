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
        for (GoalDef d : plugin.goals().all()) {
            if (d.track == null || d.track.isEmpty()) continue;
            boolean match = false;
            for (Map<String,Object> m : d.track) {
                Object src = m.get("source");
                if (src == null) continue;
                String s = String.valueOf(src).toLowerCase(Locale.ROOT);
                if (!s.startsWith("region_stay:")) continue;
                String want = s.substring("region_stay:".length());
                if (want.equals("*") || want.equals("any") || want.equalsIgnoreCase(regionId)) { match = true; break; }
            }
            if (!match) continue;
            plugin.progress().add(p, d, 1);
        }
    }

    private void handle(Player p, String kind, String id, long delta) {
        for (GoalDef d : plugin.goals().all()) {
            if (d.track == null || d.track.isEmpty()) continue;
            boolean match = false;
            for (Map<String,Object> m : d.track) {
                Object src = m.get("source");
                if (src == null) continue;
                String s = String.valueOf(src).toLowerCase(Locale.ROOT);
                int i = s.indexOf(':');
                if (i < 0) continue;
                String k = s.substring(0, i);
                String vals = s.substring(i+1);
                if (!k.equals(kind)) continue;
                if (vals.equals("*") || vals.equals("any")) { match = true; break; }
                String[] arr = vals.split(",");
                for (String v : arr) {
                    if (v.equalsIgnoreCase(id)) { match = true; break; }
                }
                if (match) break;
            }
            if (!match) continue;
            if (d.type == GoalType.CHECKLIST) {
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                if (d.checklistItems != null) {
                    for (int idx=0; idx<d.checklistItems.size(); idx++) {
                        Map<String,Object> it = d.checklistItems.get(idx);
                        String when = String.valueOf(it.getOrDefault("when",""));
                        if (when.isEmpty()) continue;
                        String[] sp = when.split(":");
                        if (sp.length<2) continue;
                        String k = sp[0]; String v = sp[1];
                        if (k.equalsIgnoreCase(kind) && (v.equals("*") || v.equalsIgnoreCase(id))) {
                            long bit = 1L<<idx;
                            if ((pr.checklistBits & bit) == 0) {
                                pr.checklistBits |= bit;
                                pr.value = Long.bitCount(pr.checklistBits);
                                if (!pr.completed && pr.value >= d.target) pr.completed = true;
                                plugin.progress().flushAll();
                            }
                        }
                    }
                }
            } else if (d.type == GoalType.UNIQUE) {
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
