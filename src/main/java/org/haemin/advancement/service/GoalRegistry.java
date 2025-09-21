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
    private Map<String, List<GoalDef>> trackIndex = Collections.emptyMap();

    public GoalRegistry(AdvancementPlugin plugin) {
        this.plugin = plugin;
        ensureGoalFiles();
        ensureGuideFile();
        reload();
    }

    public void reload() {
        goals.clear();
        Map<String, LinkedHashSet<GoalDef>> indexBuilder = new HashMap<>();
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
                    if (def != null) {
                        goals.put(def.key, def);
                        registerIndex(def, indexBuilder);
                    }
                }
            } catch (Exception e) {
                Log.info("Failed to load " + f.getName() + " : " + e.getMessage());
            }
        }
        Log.info("Loaded " + goals.size() + " goals from " + files.size() + " file(s).");

        Map<String, List<GoalDef>> finalized = new HashMap<>();
        for (Map.Entry<String, LinkedHashSet<GoalDef>> e : indexBuilder.entrySet()) {
            finalized.put(e.getKey(), List.copyOf(e.getValue()));
        }
        trackIndex = finalized.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(finalized);
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
        String uniqueRaw = cs.getString("unique_by", "");
        if (uniqueRaw != null) {
            uniqueRaw = uniqueRaw.trim();
            if (uniqueRaw.isEmpty() || uniqueRaw.equalsIgnoreCase("none") || uniqueRaw.equals("*")) {
                uniqueRaw = null;
            }
        }
        def.uniqueBy = uniqueRaw == null ? null : uniqueRaw.toLowerCase(Locale.ROOT);
        if ((def.uniqueBy == null || def.uniqueBy.isBlank()) && def.type == GoalType.UNIQUE) {
            def.uniqueBy = "entity_type";
        }
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
        def.trackMatchers = buildTrackMatchers(def.track);
        def.trackSpecs = buildTrackSpecs(def.track);
        def.checklistMatchers = buildChecklistMatchers(def.checklistItems);
        return def;
    }

    private Map<String, GoalDef.TrackMatcher> buildTrackMatchers(List<Map<String, Object>> track) {
        if (track == null || track.isEmpty()) return Collections.emptyMap();
        Map<String, Boolean> matchAny = new HashMap<>();
        Map<String, Set<String>> values = new HashMap<>();
        Map<String, List<String>> wildcards = new HashMap<>();
        for (Map<String, Object> entry : track) {
            Object srcObj = entry.get("source");
            if (srcObj == null) continue;
            String src = String.valueOf(srcObj).trim();
            if (src.isEmpty()) continue;
            String lower = src.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(':');
            if (idx < 0) continue;
            String kind = lower.substring(0, idx);
            String payload = lower.substring(idx + 1);
            if (payload.equals("*") || payload.equals("any")) {
                matchAny.put(kind, true);
                continue;
            }
            String[] parts = payload.split(",");
            Set<String> set = values.computeIfAbsent(kind, k -> new LinkedHashSet<>());
            for (String part : parts) {
                String v = part.trim();
                if (!v.isEmpty()) set.add(v);
                if (!v.isEmpty() && v.contains("*")) {
                    wildcards.computeIfAbsent(kind, k -> new ArrayList<>()).add(v);
                }
            }
        }
        if (matchAny.isEmpty() && values.isEmpty()) return Collections.emptyMap();
        Set<String> kinds = new LinkedHashSet<>();
        kinds.addAll(matchAny.keySet());
        kinds.addAll(values.keySet());
        Map<String, GoalDef.TrackMatcher> out = new HashMap<>();
        for (String kind : kinds) {
            boolean any = matchAny.containsKey(kind);
            Set<String> set = values.get(kind);
            List<String> wc = wildcards.get(kind);
            if (set != null && !set.isEmpty()) {
                set.removeIf(s -> s.contains("*"));
                if (set.isEmpty()) set = null;
            }
            out.put(kind, new GoalDef.TrackMatcher(any, set, wc));
        }
        return out.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(out);
    }

    private Map<String, List<GoalDef.TrackSpec>> buildTrackSpecs(List<Map<String, Object>> track) {
        if (track == null || track.isEmpty()) return Collections.emptyMap();
        Map<String, List<GoalDef.TrackSpec>> out = new HashMap<>();
        for (int i = 0; i < track.size(); i++) {
            Map<String, Object> entry = track.get(i);
            if (entry == null) continue;
            Object srcObj = entry.get("source");
            if (srcObj == null) continue;
            String src = String.valueOf(srcObj).trim();
            if (src.isEmpty()) continue;
            String lower = src.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(':');
            if (idx < 0) continue;
            String kind = lower.substring(0, idx);
            String payload = lower.substring(idx + 1);
            boolean any = payload.equals("*") || payload.equals("any");
            LinkedHashSet<String> exact = new LinkedHashSet<>();
            List<String> wild = new ArrayList<>();
            if (!any) {
                String[] parts = payload.split(",");
                for (String part : parts) {
                    String v = part.trim();
                    if (v.isEmpty()) continue;
                    if (v.contains("*")) wild.add(v);
                    else exact.add(v);
                }
            }
            String preset = String.valueOf(entry.getOrDefault("preset", kind));
            int levelMin = parseInt(entry.get("level_min"), -1);
            int levelMax = parseInt(entry.get("level_max"), -1);
            String merchant = normalizedString(entry.get("merchant_profession"));
            int sampleMs = parseInt(entry.get("distance_sample_ms"), 250);
            double minMeters = parseDouble(entry.get("distance_min_m"), 0.2D);
            String mode = normalizedString(entry.get("mode"));
            GoalDef.TrackSpec spec = new GoalDef.TrackSpec(i, kind, any, exact, wild, preset, levelMin, levelMax, merchant, sampleMs, minMeters, mode);
            out.computeIfAbsent(kind, k -> new ArrayList<>()).add(spec);
        }
        if (out.isEmpty()) return Collections.emptyMap();
        for (Map.Entry<String, List<GoalDef.TrackSpec>> e : out.entrySet()) {
            e.setValue(List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private int parseInt(Object obj, int def) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt(((String) obj).trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    private double parseDouble(Object obj, double def) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble(((String) obj).trim()); } catch (Exception ignored) {}
        }
        return def;
    }

    private String normalizedString(Object obj) {
        if (obj == null) return null;
        String s = String.valueOf(obj).trim();
        return s.isEmpty() ? null : s.toLowerCase(Locale.ROOT);
    }

    private Map<String, List<GoalDef.ChecklistMatcher>> buildChecklistMatchers(List<Map<String, Object>> checklistItems) {
        if (checklistItems == null || checklistItems.isEmpty()) return Collections.emptyMap();
        Map<String, List<GoalDef.ChecklistMatcher>> out = new HashMap<>();
        for (int i = 0; i < checklistItems.size(); i++) {
            Map<String, Object> item = checklistItems.get(i);
            if (item == null) continue;
            Object whenObj = item.get("when");
            if (whenObj == null) continue;
            String when = String.valueOf(whenObj).trim();
            if (when.isEmpty()) continue;
            String lower = when.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf(':');
            if (idx < 0) continue;
            String kind = lower.substring(0, idx);
            String payload = lower.substring(idx + 1);
            boolean any = payload.equals("*") || payload.equals("any");
            Set<String> values = Collections.emptySet();
            if (!any) {
                String[] parts = payload.split(",");
                LinkedHashSet<String> set = new LinkedHashSet<>();
                for (String part : parts) {
                    String v = part.trim();
                    if (!v.isEmpty()) set.add(v);
                }
                values = set;
            }
            GoalDef.ChecklistMatcher matcher = new GoalDef.ChecklistMatcher(i, any, values);
            out.computeIfAbsent(kind, k -> new ArrayList<>()).add(matcher);
        }
        if (out.isEmpty()) return Collections.emptyMap();
        for (Map.Entry<String, List<GoalDef.ChecklistMatcher>> e : out.entrySet()) {
            e.setValue(List.copyOf(e.getValue()));
        }
        return Collections.unmodifiableMap(out);
    }

    private void registerIndex(GoalDef def, Map<String, LinkedHashSet<GoalDef>> indexBuilder) {
        if (def.trackSpecs != null) {
            for (String kind : def.trackSpecs.keySet()) {
                indexBuilder.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def);
            }
        } else if (def.trackMatchers != null) {
            for (String kind : def.trackMatchers.keySet()) {
                indexBuilder.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def);
            }
        }
        if (def.checklistMatchers != null) {
            for (String kind : def.checklistMatchers.keySet()) {
                indexBuilder.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(def);
            }
        }
    }

    private void applySimpleDsl(GoalDef def, ConfigurationSection cs) {
        ConfigurationSection whereSection = cs.getConfigurationSection("where");
        Map<String, Object> extras = new LinkedHashMap<>();
        if (whereSection != null) {
            if (whereSection.isSet("level_min")) extras.put("level_min", whereSection.getInt("level_min"));
            if (whereSection.isSet("level_max")) extras.put("level_max", whereSection.getInt("level_max"));
            if (whereSection.isSet("merchant_profession")) extras.put("merchant_profession", whereSection.getString("merchant_profession"));
            if (whereSection.isSet("distance_sample_ms")) extras.put("distance_sample_ms", whereSection.getInt("distance_sample_ms"));
            if (whereSection.isSet("distance_min_m")) extras.put("distance_min_m", whereSection.getDouble("distance_min_m"));
            if (whereSection.isSet("mode")) extras.put("mode", whereSection.getString("mode"));
        }

        if (def.track == null || def.track.isEmpty()) {
            String preset = cs.getString("preset", null);
            String when = cs.getString("when", null);
            if (preset != null && when != null) {
                String src = mapPresetToSource(preset.trim(), when.trim());
                if (src != null) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("source", src);
                    e.put("preset", preset.trim().toLowerCase(Locale.ROOT));
                    if (!extras.isEmpty()) {
                        for (Map.Entry<String, Object> ex : extras.entrySet()) {
                            e.put(ex.getKey(), ex.getValue());
                        }
                    }
                    def.track = List.of(e);
                    if ("mythic_kill".equalsIgnoreCase(preset) && (def.uniqueBy == null || def.uniqueBy.isBlank())) def.uniqueBy = "mythic_id";
                    if ("advancement".equalsIgnoreCase(preset) && (def.uniqueBy == null || def.uniqueBy.isBlank())) def.uniqueBy = "advancement_key";
                }
            }
        }
        if ((def.filter == null || def.filter.isBlank()) && whereSection != null) {
            ConfigurationSection w = whereSection;
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
        if (p.equals("harvest")) return "harvest:" + list;
        if (p.equals("shear")) return "shear:" + list;
        if (p.equals("breed")) return "breed:" + list;
        if (p.equals("tame")) return "tame:" + list;
        if (p.equals("trade")) return "trade:" + list;
        if (p.equals("enchant")) return "enchant:" + list;
        if (p.equals("anvil")) return "anvil:" + list;
        if (p.equals("smithing")) return "smithing:" + list;
        if (p.equals("brew")) return "brew:" + list;
        if (p.equals("consume")) return "consume:" + list;
        if (p.equals("distance")) return "distance:" + list;
        if (p.equals("advancement")) return "advancement:" + list;
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
    public Collection<GoalDef> trackedBy(String kind) {
        if (kind == null || kind.isEmpty()) return Collections.emptyList();
        return trackIndex.getOrDefault(kind.toLowerCase(Locale.ROOT), Collections.emptyList());
    }
}
