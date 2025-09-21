package org.haemin.advancement.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;
import org.haemin.advancement.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class GoalRegistry {
    private final AdvancementPlugin plugin;
    private final Map<String, GoalDef> goals = new LinkedHashMap<>();

    public GoalRegistry(AdvancementPlugin plugin) {
        this.plugin = plugin;
        ensureGoalFiles();
        ensureGuideFile();
        reload();
    }

    public void reload() {
        goals.clear();
        File dir = new File(new File(plugin.getDataFolder(), "config"), "goals");
        List<File> files = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] arr = dir.listFiles((d, f) -> f.toLowerCase(Locale.ROOT).endsWith(".yml"));
            if (arr != null) files.addAll(Arrays.asList(arr));
        }
        File legacy = new File(new File(plugin.getDataFolder(), "config"), "goals.yml");
        if (legacy.exists()) files.add(legacy);

        for (File f : files) {
            try {
                YamlConfiguration yc = YamlConfiguration.loadConfiguration(f);
                ConfigurationSection root = yc.getConfigurationSection("goals");
                if (root == null) continue;
                for (String key : root.getKeys(false)) {
                    ConfigurationSection cs = root.getConfigurationSection(key);
                    if (cs == null) continue;
                    GoalDef def = parseGoal(key, cs);
                    if (def != null) goals.put(def.key, def);
                }
            } catch (Exception e) {
                Log.info("Failed to load " + f.getName() + " : " + e.getMessage());
            }
        }
        Log.info("Loaded " + goals.size() + " goals from " + files.size() + " file(s).");
    }

    private GoalDef parseGoal(String key, ConfigurationSection cs) {
        GoalDef def = new GoalDef();
        def.key = key;
        def.title = cs.getString("title", key);
        try { def.type = GoalType.valueOf(cs.getString("type", "counter").toUpperCase(Locale.ROOT)); }
        catch (Exception e) { def.type = GoalType.COUNTER; }

        def.track = (List<Map<String, Object>>)(List<?>) cs.getList("track", Collections.emptyList());
        def.filter = cs.getString("filter", "");
        def.target = cs.getLong("target", 1L);
        def.reset = cs.getString("reset", "daily");
        def.uniqueBy = cs.getString("unique_by", "entity_type");
        def.rewards = (List<Map<String, Object>>)(List<?>) cs.getList("rewards", null);

        List<?> items = cs.getList("items", null);
        if (items == null) items = cs.getList("checklist", null);
        def.checklistItems = (items == null) ? null : (List<Map<String, Object>>)(List<?>) items;
        def.checklistRequire = cs.getInt("require", 0);
        if (def.type == GoalType.CHECKLIST) {
            int n = def.checklistItems == null ? 0 : def.checklistItems.size();
            if (def.checklistRequire <= 0) def.checklistRequire = n;
            def.target = def.checklistRequire;
        }

        ConfigurationSection streak = cs.getConfigurationSection("streak");
        def.streakConf = (streak == null) ? null : streak.getValues(true);
        ConfigurationSection tt = cs.getConfigurationSection("timetrial");
        def.timetrialConf = (tt == null) ? null : tt.getValues(true);

        List<String> loreList = cs.getStringList("lore");
        if (loreList == null || loreList.isEmpty()) {
            String one = cs.getString("lore", null);
            if (one != null && !one.isEmpty()) loreList = List.of(one);
        }
        if (loreList == null || loreList.isEmpty()) loreList = cs.getStringList("goal_lore");
        if (loreList == null || loreList.isEmpty()) loreList = cs.getStringList("conditions");
        def.lore = (loreList == null) ? Collections.emptyList() : loreList;

        def.requires = cs.getStringList("requires");
        if (def.requires != null && def.requires.isEmpty()) def.requires = null;
        def.activateIfHas = cs.getStringList("activate_if_has");
        if (def.activateIfHas != null && def.activateIfHas.isEmpty()) def.activateIfHas = null;
        def.boosts = (List<Map<String, Object>>)(List<?>) cs.getList("boosts", null);

        applySimpleDsl(def, cs);
        return def;
    }

    private void applySimpleDsl(GoalDef def, ConfigurationSection cs) {
        if (def.track == null || def.track.isEmpty()) {
            String preset = cs.getString("preset", null);
            String when = cs.getString("when", null);
            if (preset != null && when != null) {
                String src = mapPresetToSource(preset.trim(), when.trim());
                if (src != null) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("source", src);
                    def.track = List.of(e);
                    if ("mythic_kill".equalsIgnoreCase(preset) && def.uniqueBy == null) def.uniqueBy = "mythic_id";
                }
            }
        }
        if ((def.filter == null || def.filter.isBlank()) && cs.isConfigurationSection("where")) {
            ConfigurationSection w = cs.getConfigurationSection("where");
            List<String> parts = new ArrayList<>();
            String world = w.getString("world", null);
            if (world != null && !world.isBlank()) {
                String[] arr = world.split(",");
                StringBuilder b = new StringBuilder();
                for (int i=0;i<arr.length;i++) {
                    if (i>0) b.append(',');
                    b.append('\'').append(arr[i].trim()).append('\'');
                }
                if (b.length()>0) parts.add("world in [" + b + "]");
            }
            String region = w.getString("region", null);
            if (region != null && !region.isBlank()) {
                String[] arr = region.split(",");
                StringBuilder b = new StringBuilder();
                for (int i=0;i<arr.length;i++) {
                    if (i>0) b.append(',');
                    b.append('\'').append(arr[i].trim()).append('\'');
                }
                if (b.length()>0) parts.add("region in [" + b + "]");
            }
            String regionNot = w.getString("region_not", null);
            if (regionNot != null && !regionNot.isBlank()) parts.add("region!='" + regionNot.trim() + "'");
            String time = w.getString("time", null);
            if (time != null && !time.isBlank()) parts.add("time.in('" + time.trim() + "')");
            String tool = w.getString("tool", null);
            if (tool != null && !tool.isBlank()) parts.add("tool in " + tool.trim());
            String yb = w.getString("y_between", null);
            if (yb != null && yb.contains("..")) {
                String[] ab = yb.split("\\.\\.");
                if (ab.length == 2) parts.add("y.between(" + ab[0].trim() + "," + ab[1].trim() + ")");
            }
            if (!parts.isEmpty()) def.filter = String.join(" && ", parts);
        }
    }

    private String mapPresetToSource(String preset, String when) {
        String list = when.replace(" ", "");
        String p = preset.toLowerCase(Locale.ROOT);
        if (p.equals("break") || p.equals("mine")) return "block_break:" + list;
        if (p.equals("place")) return "block_place:" + list;
        if (p.equals("kill")) return "mob_kill:" + list;
        if (p.equals("mythic_kill")) return "mob_kill:mythic:" + list;
        if (p.equals("craft")) return "craft:" + list;
        if (p.equals("smelt")) return "smelt:" + list;
        if (p.equals("pickup")) return "pickup:" + list;
        if (p.equals("fish")) return "fish:" + list;
        if (p.equals("stay") || p.equals("region_stay")) return "region_stay:" + list;
        return null;
    }

    private void ensureGoalFiles() {
        File cfgDir = new File(plugin.getDataFolder(), "config");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        File dir = new File(cfgDir, "goals");
        if (!dir.exists()) dir.mkdirs();

        File daily = new File(dir, "daily.yml");
        if (!daily.exists()) {
            YamlConfiguration yc = new YamlConfiguration();
            yc.set("goals.daily_wheat.title", "&e일일 채집가");
            yc.set("goals.daily_wheat.reset", "daily");
            yc.set("goals.daily_wheat.preset", "break");
            yc.set("goals.daily_wheat.when", "wheat,carrots,potatoes");
            Map<String,Object> where = new LinkedHashMap<>();
            where.put("world", "world");
            where.put("region_not", "spawn");
            where.put("time", "06:00-23:00");
            where.put("tool", "HOE");
            yc.set("goals.daily_wheat.where", where);
            yc.set("goals.daily_wheat.target", 300);
            yc.set("goals.daily_wheat.rewards", List.of(
                    Collections.singletonMap("money", 800),
                    Collections.singletonMap("give", "@빵세트 8")
            ));
            yc.set("goals.daily_wheat.boosts", List.of(
                    Map.of("if_has","ia:server:diamond_gloves","multiplier",1.25),
                    Map.of("if_has","mmoitems:PICKAXE:ELITE_PICK","multiplier",1.5)
            ));
            try { yc.save(daily); } catch (IOException ignored) {}
        }

        File repeat = new File(dir, "repeat.yml");
        if (!repeat.exists()) {
            YamlConfiguration yc = new YamlConfiguration();
            yc.set("goals.mob_hunter_repeat.title", "&a사냥꾼 (반복)");
            yc.set("goals.mob_hunter_repeat.reset", "repeat");
            yc.set("goals.mob_hunter_repeat.preset", "kill");
            yc.set("goals.mob_hunter_repeat.when", "any");
            yc.set("goals.mob_hunter_repeat.target", 20);
            yc.set("goals.mob_hunter_repeat.rewards", List.of(
                    Collections.singletonMap("money", 200)
            ));
            try { yc.save(repeat); } catch (IOException ignored) {}
        }

        File weekly = new File(dir, "weekly.yml");
        if (!weekly.exists()) {
            YamlConfiguration yc = new YamlConfiguration();
            yc.set("goals.weekly_bosses.title", "&c주간 보스 헌터");
            yc.set("goals.weekly_bosses.reset", "weekly");
            yc.set("goals.weekly_bosses.type", "unique");
            yc.set("goals.weekly_bosses.preset", "mythic_kill");
            yc.set("goals.weekly_bosses.when", "*");
            yc.set("goals.weekly_bosses.unique_by", "mythic_id");
            yc.set("goals.weekly_bosses.target", 10);
            yc.set("goals.weekly_bosses.requires", List.of("daily_wheat"));
            yc.set("goals.weekly_bosses.rewards", List.of(
                    Collections.singletonMap("money", 5000),
                    Collections.singletonMap("cmd", "broadcast &c%player%&f가 &e주간 보스 헌터&f 달성!")
            ));
            try { yc.save(weekly); } catch (IOException ignored) {}
        }

        File season = new File(dir, "season.yml");
        if (!season.exists()) {
            YamlConfiguration yc = new YamlConfiguration();
            yc.set("goals.season_explorer.title", "&b시즌 탐험가");
            yc.set("goals.season_explorer.reset", "season:2025S3");
            yc.set("goals.season_explorer.type", "checklist");
            yc.set("goals.season_explorer.require", 3);
            List<Map<String,Object>> items = new ArrayList<>();
            items.add(Map.of("key","ancient_ruins","title","고대 유적 방문","when","region_enter:ancient_ruins"));
            items.add(Map.of("key","frozen_peak","title","얼어붙은 봉우리 방문","when","region_enter:frozen_peak"));
            items.add(Map.of("key","kill_golem","title","보스 골렘 처치","when","mob_kill:mythic:BOSS_GOLEM"));
            items.add(Map.of("key","nether","title","네더 입장","when","world_enter:world_nether"));
            items.add(Map.of("key","elytra","title","겉날개 획득","when","pickup:elytra"));
            yc.set("goals.season_explorer.items", items);
            yc.set("goals.season_explorer.rewards", List.of(
                    Collections.singletonMap("give", "@시즌보상상자 1"),
                    Collections.singletonMap("cmd", "lp user %player% permission settemp cosmetic.trail.wings true 30d")
            ));
            yc.set("goals.season_explorer.activate_if_has", List.of("ia:server:adventurer_badge"));
            try { yc.save(season); } catch (IOException ignored) {}
        }
    }

    private void ensureGuideFile() {
        File cfgDir = new File(plugin.getDataFolder(), "config");
        if (!cfgDir.exists()) cfgDir.mkdirs();
        File guide = new File(cfgDir, "GOALS_GUIDE.md");
        if (guide.exists()) return;
        String md = ""
                + "# GOALS Quick Reference\n\n"
                + "## 프리셋과 확장 필드\n"
                + "- requires: [선행목표키]\n"
                + "- activate_if_has: [\"ia:ns:id\", \"mmoitems:TYPE:ID\", \"mc:minecraft:bread\"]\n"
                + "- boosts:\n"
                + "  - { if_has: \"ia:ns:id\", multiplier: 1.5, add: 0 }\n"
                + "\n";
        try (FileWriter fw = new FileWriter(guide)) { fw.write(md); } catch (IOException ignored) {}
    }

    public Collection<GoalDef> all() { return goals.values(); }
    public GoalDef get(String key) { return goals.get(key); }
}
