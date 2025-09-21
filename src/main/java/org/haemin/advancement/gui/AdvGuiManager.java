package org.haemin.advancement.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.Progress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdvGuiManager implements Listener {
    public enum Tab { ALL, DAILY, WEEKLY, SEASON }

    private final AdvancementPlugin plugin;

    public AdvGuiManager(AdvancementPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p, Tab tab) {
        String title = plugin.guiCfg().title(tab);
        Inventory inv = Bukkit.createInventory(new Holder(tab), 54, title);
        inv.setItem(plugin.guiCfg().slotOf("all"),    plugin.guiCfg().navItem("all",    tab==Tab.ALL));
        inv.setItem(plugin.guiCfg().slotOf("daily"),  plugin.guiCfg().navItem("daily",  tab==Tab.DAILY));
        inv.setItem(plugin.guiCfg().slotOf("weekly"), plugin.guiCfg().navItem("weekly", tab==Tab.WEEKLY));
        inv.setItem(plugin.guiCfg().slotOf("season"), plugin.guiCfg().navItem("season", tab==Tab.SEASON));

        List<Integer> slots = plugin.guiCfg().listSlots();
        List<GoalDef> list = filtered(tab);
        int n = Math.min(slots.size(), list.size());
        for (int i=0;i<n;i++) {
            GoalDef d = list.get(i);
            Progress pr = plugin.progress().get(p.getUniqueId(), d);
            inv.setItem(slots.get(i), plugin.guiCfg().goalItem(p, d, pr));
        }

        p.openInventory(inv);
        Bukkit.getScheduler().runTask(plugin, () -> plugin.sounds().play(p, "gui_open"));
    }

    private List<GoalDef> filtered(Tab tab) {
        List<GoalDef> out = new ArrayList<>();
        for (GoalDef g : plugin.goals().all()) {
            String r = g.reset == null ? "" : g.reset.toLowerCase(Locale.ROOT);
            boolean keep = tab == Tab.ALL
                    || (tab == Tab.DAILY  && r.startsWith("daily"))
                    || (tab == Tab.WEEKLY && r.startsWith("weekly"))
                    || (tab == Tab.SEASON && r.startsWith("season"));
            if (keep) out.add(g);
        }
        return out;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Holder)) return;
        Holder h = (Holder) holder;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();

        int raw = e.getRawSlot();
        if (raw == plugin.guiCfg().slotOf("all"))    { open(p, Tab.ALL); return; }
        if (raw == plugin.guiCfg().slotOf("daily"))  { open(p, Tab.DAILY); return; }
        if (raw == plugin.guiCfg().slotOf("weekly")) { open(p, Tab.WEEKLY); return; }
        if (raw == plugin.guiCfg().slotOf("season")) { open(p, Tab.SEASON); return; }

        List<Integer> slots = plugin.guiCfg().listSlots();
        int idx = slots.indexOf(raw);
        if (idx < 0) return;
        List<GoalDef> list = filtered(h.tab);
        if (idx >= list.size()) return;
        GoalDef def = list.get(idx);
        Progress pr = plugin.progress().get(p.getUniqueId(), def);
        if (pr.completed) {
            if (plugin.progress().claim(p, def)) open(p, h.tab);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (!(holder instanceof Holder)) return;
        if (e.getPlayer() instanceof Player) {
            Player p = (Player) e.getPlayer();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.sounds().play(p, "gui_close"));
        }
    }

    private static class Holder implements InventoryHolder {
        final Tab tab;
        Holder(Tab t) { this.tab = t; }
        @Override public Inventory getInventory() { return null; }
    }
}
