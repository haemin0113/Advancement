package org.haemin.advancement.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.gui.AdvGuiManager;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.Progress;

import java.util.*;
import java.util.stream.Collectors;

public class AdvCommand implements CommandExecutor, TabCompleter {
    private final AdvancementPlugin plugin;

    public AdvCommand(AdvancementPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            if (s instanceof Player p) { plugin.gui().open(p, AdvGuiManager.Tab.ALL); return true; }
            s.sendMessage("ingame only"); return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("list")) {
            if (!(s instanceof Player p)) return true;
            for (GoalDef d : plugin.goals().all()) {
                Progress pr = plugin.progress().get(p.getUniqueId(), d);
                String line = "&e" + d.title + " &7: &a" + pr.value + "&7/&f" + d.target + " &8(" + pr.percent() + "%)";
                s.sendMessage(org.haemin.advancement.util.Text.legacy(line));
            }
            return true;
        }
        if (sub.equals("focus")) {
            s.sendMessage("not implemented"); return true;
        }
        if (sub.equals("claim")) {
            if (!(s instanceof Player p)) return true;
            if (a.length < 2) { s.sendMessage("/"+label+" claim <goalKey>"); return true; }
            GoalDef def = plugin.goals().get(a[1]);
            if (def == null) { s.sendMessage("unknown goal"); return true; }
            if (!plugin.progress().claim(p, def)) s.sendMessage("not completed");
            return true;
        }
        if (sub.equals("admin")) {
            if (!s.isOp()) { s.sendMessage("OP only"); return true; }
            if (a.length < 2) { s.sendMessage("/"+label+" admin <reload|set|reset|item|new>"); return true; }
            String as = a[1].toLowerCase(Locale.ROOT);
            if (as.equals("reload")) {
                plugin.reloadAll();
                s.sendMessage("reloaded");
                return true;
            }
            if (as.equals("set")) {
                if (a.length < 5) { s.sendMessage("/"+label+" admin set <goal> <player> <value>"); return true; }
                GoalDef def = plugin.goals().get(a[2]);
                Player t = Bukkit.getPlayerExact(a[3]);
                long v = Long.parseLong(a[4]);
                if (def == null || t == null) { s.sendMessage("invalid"); return true; }
                Progress pr = plugin.progress().get(t.getUniqueId(), def);
                pr.value = v; pr.completed = pr.value >= def.target;
                plugin.progress().flushAll();
                s.sendMessage("ok");
                return true;
            }
            if (as.equals("reset")) {
                if (a.length < 3) { s.sendMessage("/"+label+" admin reset <goal> [player]"); return true; }
                GoalDef def = plugin.goals().get(a[2]);
                if (def == null) { s.sendMessage("unknown goal"); return true; }
                if (a.length >= 4) {
                    Player t = Bukkit.getPlayerExact(a[3]);
                    if (t != null) {
                        Progress pr = plugin.progress().get(t.getUniqueId(), def);
                        pr.value=0; pr.completed=false;
                        plugin.progress().flushAll();
                    }
                }
                s.sendMessage("ok");
                return true;
            }
            if (as.equals("save")) {
                plugin.progress().flushAll();
                s.sendMessage("saved");
                return true;
            }

            if (as.equals("item")) {
                if (a.length >= 3 && a[2].equalsIgnoreCase("import")) {
                    if (!(s instanceof Player p)) { s.sendMessage("ingame only"); return true; }
                    if (a.length < 4) { s.sendMessage("/"+label+" admin item import <name>"); return true; }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand == null || hand.getType()== Material.AIR) { s.sendMessage("hold an item"); return true; }
                    boolean ok = plugin.rewards().saveItem(a[3], hand);
                    s.sendMessage(ok? "saved" : "failed");
                    return true;
                }
                s.sendMessage("/"+label+" admin item import <name>");
                return true;
            }
            if (as.equals("new")) {
                s.sendMessage("wizard not installed");
                return true;
            }
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        if (a.length == 1) {
            List<String> base = Arrays.asList("list","focus","claim","admin");
            String p = a[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String it : base) if (it.startsWith(p)) out.add(it);
            return out;
        }
        if (a.length == 2 && a[0].equalsIgnoreCase("admin")) return Arrays.asList("reload","set","reset","item","new");
        if (a.length == 3 && a[0].equalsIgnoreCase("admin") && (a[1].equalsIgnoreCase("set") || a[1].equalsIgnoreCase("reset")))
            return plugin.goals().all().stream().map(g->g.key).toList();
        if (a.length == 3 && a[0].equalsIgnoreCase("admin") && a[1].equalsIgnoreCase("item"))
            return Collections.singletonList("import");
        if (a.length == 2 && a[0].equalsIgnoreCase("claim"))
            return plugin.goals().all().stream().map(g->g.key).toList();
        return Collections.emptyList();
    }
}
