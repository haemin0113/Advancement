package org.haemin.advancement.gui;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.Progress;
import org.haemin.advancement.util.GoalLore;
import org.haemin.advancement.util.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GuiConfig {
    private final AdvancementPlugin plugin;
    private YamlConfiguration yml;

    public GuiConfig(AdvancementPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File dir = new File(plugin.getDataFolder(), "config");
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "gui.yml");
        if (!f.exists()) {
            try { plugin.saveResource("config/gui.yml", false); }
            catch (Throwable t) {
                String s = "title: \"&6도전과제 — %tab%\"\n"
                        + "nav:\n"
                        + "  all:    { slot: 0, material: BOOKSHELF,   name: \"&e전체\", custom-model-data: 0 }\n"
                        + "  daily:  { slot: 1, material: SUNFLOWER,   name: \"&6일일\", custom-model-data: 0 }\n"
                        + "  weekly: { slot: 2, material: CLOCK,       name: \"&b주간\", custom-model-data: 0 }\n"
                        + "  season: { slot: 3, material: NETHER_STAR, name: \"&d시즌\", custom-model-data: 0 }\n"
                        + "list_slots: [9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53]\n"
                        + "item:\n"
                        + "  in_progress: { material: WHITE_STAINED_GLASS_PANE, custom-model-data: 0 }\n"
                        + "  completed:   { material: LIME_STAINED_GLASS_PANE,  custom-model-data: 0 }\n"
                        + "  locked:      { material: RED_STAINED_GLASS_PANE,   custom-model-data: 0, name: \"&c잠금: 선행 필요\" }\n"
                        + "  name: \"%title%\"\n"
                        + "  lore:\n"
                        + "    - \"&7진행: &a%value%&7/&f%target% &8(%percent%%)\"\n"
                        + "    - \"&7보상:\"\n"
                        + "    - \"%rewards%\"\n"
                        + "    - \"%goal_lore%\"\n"
                        + "    - \"{op}&8키: %key%\"\n";
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(s.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }
        }
        this.yml = YamlConfiguration.loadConfiguration(f);
    }

    public String title(String tabName) {
        String t = yml.getString("title", "&6도전과제 — %tab%");
        return Text.legacy(t.replace("%tab%", tabName));
    }

    public String title(AdvGuiManager.Tab tab) {
        String name;
        switch (tab) {
            case DAILY:  name = "일일"; break;
            case WEEKLY: name = "주간"; break;
            case SEASON: name = "시즌"; break;
            default: name = "전체";
        }
        return title(name);
    }

    public int slotOf(String tabKey) {
        if (!yml.isSet("nav."+tabKey+".slot")) {
            if ("all".equals(tabKey)) return 0;
            if ("daily".equals(tabKey)) return 1;
            if ("weekly".equals(tabKey)) return 2;
            if ("season".equals(tabKey)) return 3;
        }
        return yml.getInt("nav." + tabKey + ".slot", 0);
    }

    public ItemStack navItem(String tabKey, boolean active) {
        String path = "nav." + tabKey;
        String matName = yml.getString(path + ".material",
                "all".equals(tabKey) ? "BOOKSHELF" :
                        "daily".equals(tabKey) ? "SUNFLOWER" :
                                "weekly".equals(tabKey) ? "CLOCK" : "NETHER_STAR");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.BOOKSHELF;
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        String name = yml.getString(path + ".name",
                "all".equals(tabKey) ? "&e전체" :
                        "daily".equals(tabKey) ? "&6일일" :
                                "weekly".equals(tabKey) ? "&b주간" : "&d시즌");
        String prefix = active ? "&a» " : "&7";
        im.setDisplayName(Text.legacy(prefix + name));
        int cmd = yml.getInt(path + ".custom-model-data", 0);
        if (cmd > 0) try { im.setCustomModelData(cmd); } catch (Throwable ignored) {}
        it.setItemMeta(im);
        return it;
    }

    public List<Integer> listSlots() {
        List<Integer> def = new ArrayList<>();
        for (int i=9;i<54;i++) def.add(i);
        List<Integer> list = yml.getIntegerList("list_slots");
        return (list == null || list.isEmpty()) ? def : list;
    }

    public ItemStack goalItem(Player viewer, GoalDef d, Progress pr) {
        boolean active = plugin.progress().isActive(viewer, d);
        boolean done = pr.value >= pr.target;
        if (!active) {
            Material mat = Material.matchMaterial(yml.getString("item.locked.material", "RED_STAINED_GLASS_PANE"));
            if (mat == null) mat = Material.RED_STAINED_GLASS_PANE;
            ItemStack it = new ItemStack(mat);
            ItemMeta im = it.getItemMeta();
            String name = yml.getString("item.locked.name", "&c잠금: 선행 필요");
            im.setDisplayName(Text.legacy(name));
            List<String> lore = new ArrayList<>();
            lore.add(Text.legacy("&7- 선행 목표 필요"));
            if (d.requires != null && !d.requires.isEmpty()) {
                for (String req : d.requires) {
                    boolean ok = false;
                    Progress rp = plugin.progress().get(viewer.getUniqueId(), req, 0);
                    ok = rp != null && rp.completed;
                    lore.add(Text.legacy((ok?"&a✔ ":"&c✖ ") + req));
                }
            }
            if (d.activateIfHas != null && !d.activateIfHas.isEmpty()) {
                lore.add(Text.legacy("&7- 혹은 보유 시 활성화"));
                for (String s : d.activateIfHas) lore.add(Text.legacy("&8• &f"+s));
            }
            im.setLore(lore);
            it.setItemMeta(im);
            return it;
        }

        String state = done ? "completed" : "in_progress";
        Material mat = Material.matchMaterial(yml.getString("item."+state+".material",
                done? "LIME_STAINED_GLASS_PANE" : "WHITE_STAINED_GLASS_PANE"));
        if (mat == null) mat = done ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE;

        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();

        int cmd = yml.getInt("item."+state+".custom-model-data", 0);
        if (cmd > 0) try { im.setCustomModelData(cmd); } catch (Throwable ignored) {}

        String nameTpl = yml.getString("item.name", "%title%");
        im.setDisplayName(Text.legacy(nameTpl.replace("%title%", d.title)));

        List<String> loreTpl = yml.getStringList("item.lore");
        List<String> lore = new ArrayList<>();
        List<String> rewardLines = rewardsLines(viewer, d);
        List<String> goalLore = GoalLore.describe(d);
        boolean injectedGoalLore = false;

        for (String raw : loreTpl) {
            boolean opOnly = raw.startsWith("{op}");
            String line = opOnly ? raw.substring(4) : raw;

            if (line.contains("%goal_lore%")) {
                for (String gl : goalLore) lore.add(Text.legacy(gl));
                injectedGoalLore = true;
                continue;
            }
            if (line.contains("%rewards%")) {
                if (!rewardLines.isEmpty()) lore.addAll(rewardLines);
                continue;
            }

            line = line.replace("%key%", d.key)
                    .replace("%value%", String.valueOf(pr.value))
                    .replace("%target%", String.valueOf(pr.target))
                    .replace("%percent%", String.valueOf(pr.percent()));

            if (!opOnly || viewer.isOp()) lore.add(Text.legacy(line));
        }

        if (!injectedGoalLore && !goalLore.isEmpty()) {
            lore.add(Text.legacy("<#cfe8ff>▸ <#7fbfff>달성 조건"));
            for (String gl : goalLore) lore.add(Text.legacy(gl));
        }

        if (done) lore.add(Text.legacy("&a좌클릭: 보상 수령"));

        im.setLore(lore);
        it.setItemMeta(im);
        return it;
    }

    private List<String> rewardsLines(Player viewer, GoalDef d) {
        if (d.rewards == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Map<String,Object> r : d.rewards) {
            if (r.containsKey("money")) out.add(Text.legacy(" &8- &6머니 &f" + r.get("money")));
            if (r.containsKey("give")) {
                String spec = String.valueOf(r.get("give"));
                ItemStack sample = plugin.rewards().resolveSample(spec);
                String disp = prettyName(sample);
                out.add(Text.legacy(" &8- &a아이템 &f" + disp));
            }
            if (r.containsKey("cmd") && viewer.isOp()) out.add(Text.legacy(" &8- &7명령 &8/&f" + r.get("cmd")));
        }
        return out;
    }

    private String prettyName(ItemStack is) {
        if (is == null) return "Unknown";
        try {
            if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
                return Text.legacy(is.getItemMeta().getDisplayName());
            }
        } catch (Throwable ignored) {}
        String n = is.getType().name();
        char[] cs = n.toCharArray();
        StringBuilder sb = new StringBuilder(cs.length + 4);
        boolean sp = true;
        for (int i=0;i<cs.length;i++) {
            char c = cs[i];
            if (c == '_') { sb.append(' '); sp = true; continue; }
            sb.append(sp ? Character.toUpperCase(c) : Character.toLowerCase(c));
            sp = false;
        }
        return sb.toString();
    }
}
