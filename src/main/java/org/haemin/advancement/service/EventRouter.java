package org.haemin.advancement.service;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Merchant;
import org.bukkit.entity.Player;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;
import org.haemin.advancement.model.Progress;
import org.haemin.advancement.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventRouter implements Listener {
    private static final double DEFAULT_DISTANCE_MIN = 0.2D;
    private static final Set<String> BOAT_TYPES = new LinkedHashSet<>(List.of(
            "boat", "raft", "chest_boat", "chest_raft"
    ));

    private static final Method POTION_TYPE_KEY_METHOD = findPotionTypeKey();

    private final AdvancementPlugin plugin;
    private final Map<UUID, DistanceState> distanceStates = new HashMap<>();
    private final Map<UUID, Map<String, DistanceBuffer>> distanceProgress = new HashMap<>();

    public EventRouter(AdvancementPlugin plugin) {
        this.plugin = plugin;
        logHook("block_break");
        logHook("block_place");
        logHook("mob_kill");
        logHook("craft");
        logHook("fish");
        logHook("pickup");
        logHook("smelt");
        logHook("harvest");
        logHook("shear");
        logHook("breed");
        logHook("tame");
        logHook("trade");
        logHook("enchant");
        logHook("anvil");
        logHook("smithing");
        logHook("brew");
        logHook("consume");
        logHook("distance");
        logHook("advancement");
    }

    private void logHook(String preset) {
        Log.info("Hooked preset listener: " + preset);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        Material type = block.getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext breakCtx = TrackContext.of("block_break").block(type);
        route(p, "block_break", id, 1, breakCtx);

        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            if (ageable.getAge() >= ageable.getMaximumAge()) {
                TrackContext harvestCtx = TrackContext.of("harvest").block(type);
                route(p, "harvest", id, 1, harvestCtx);
            }
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlockPlaced();
        Material type = block.getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("block_place").block(type);
        route(p, "block_place", id, 1, ctx);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String id = e.getEntityType().name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("mob_kill").entity(e.getEntityType());
        route(killer, "mob_kill", id, 1, ctx);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player p = e.getPlayer();
        TrackContext ctx = TrackContext.of("fish");
        route(p, "fish", "any", 1, ctx);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getRecipe() == null ? null : e.getRecipe().getResult();
        String id = result == null ? "any" : result.getType().name().toLowerCase(Locale.ROOT);
        int delta = result == null ? 1 : Math.max(1, result.getAmount());
        TrackContext ctx = TrackContext.of("craft");
        if (result != null) ctx.item(result.getType());
        route(p, "craft", id, delta, ctx);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        Entity ent = e.getEntity();
        if (!(ent instanceof Player p)) return;
        ItemStack stack = e.getItem().getItemStack();
        Material type = stack.getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("pickup").item(type);
        int delta = Math.max(1, stack.getAmount());
        route(p, "pickup", id, delta, ctx);
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent e) {
        Player p = e.getPlayer();
        Material type = e.getItemType();
        String id = type.name().toLowerCase(Locale.ROOT);
        int delta = Math.max(1, e.getItemAmount());
        TrackContext ctx = TrackContext.of("smelt").item(type);
        route(p, "smelt", id, delta, ctx);
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        Player p = e.getPlayer();
        EntityType type = e.getEntity().getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("shear").entity(type);
        route(p, "shear", id, 1, ctx);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent e) {
        Entity breeder = e.getBreeder();
        if (!(breeder instanceof Player p)) return;
        EntityType type = e.getEntityType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("breed").entity(type);
        route(p, "breed", id, 1, ctx);
    }

    @EventHandler
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player p)) return;
        EntityType type = e.getEntityType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("tame").entity(type);
        route(p, "tame", id, 1, ctx);
    }

    @EventHandler
    public void onTrade(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        InventoryView view = e.getView();
        if (!(view.getTopInventory() instanceof MerchantInventory inv)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        Merchant merchant = inv.getMerchant();
        String profession = null;
        int level = 0;
        if (merchant instanceof Villager villager) {
            profession = villager.getProfession().name().toLowerCase(Locale.ROOT);
            level = villager.getVillagerLevel();
        } else if (merchant instanceof WanderingTrader) {
            profession = "wandering_trader";
            level = 1;
        }
        String levelName = villagerLevelName(level);
        String id = profession == null ? "any" : profession;
        TrackContext ctx = TrackContext.of("trade").trade(profession, level, levelName).item(current.getType());
        route(p, "trade", id, 1, ctx);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        Player p = e.getEnchanter();
        ItemStack item = e.getItem();
        Map<Enchantment, Integer> enchants = e.getEnchantsToAdd();
        if (enchants == null || enchants.isEmpty()) return;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue() == null ? 1 : entry.getValue();
            String key = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
            TrackContext ctx = TrackContext.of("enchant").enchant(enchantment, level);
            if (item != null) ctx.item(item.getType());
            route(p, "enchant", key, 1, ctx);
        }
    }

    @EventHandler
    public void onAnvil(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTopInventory().getType() != InventoryType.ANVIL) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;
        Material type = item.getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("anvil").item(type);
        route(p, "anvil", id, 1, ctx);
    }

    @EventHandler
    public void onSmithing(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTopInventory().getType() != InventoryType.SMITHING) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;
        Material type = item.getType();
        String id = type.name().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("smithing").item(type);
        route(p, "smithing", id, 1, ctx);
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        BrewerInventory inv = e.getContents();
        if (inv == null) return;
        List<Player> viewers = new ArrayList<>();
        for (HumanEntity viewer : inv.getViewers()) {
            if (viewer instanceof Player p) viewers.add(p);
        }
        if (viewers.isEmpty()) return;

        int count = 0;
        String key = null;
        Material itemType = null;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;
            count += Math.max(1, stack.getAmount());
            itemType = stack.getType();
            String potionKey = resolvePotionKey(stack);
            if (potionKey != null) key = potionKey;
        }
        if (count <= 0) return;
        String id = key == null ? (itemType == null ? "any" : itemType.name().toLowerCase(Locale.ROOT)) : key;
        for (Player viewer : viewers) {
            TrackContext ctx = TrackContext.of("brew");
            if (itemType != null) ctx.item(itemType);
            if (key != null) ctx.potion(key);
            route(viewer, "brew", id, count, ctx);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        ItemStack stack = e.getItem();
        if (stack == null) return;
        Material type = stack.getType();
        String itemId = type.name().toLowerCase(Locale.ROOT);
        String potionKey = resolvePotionKey(stack);
        String id = potionKey != null ? potionKey : itemId;
        TrackContext ctx = TrackContext.of("consume").item(type);
        if (potionKey != null) ctx.potion(potionKey);
        route(p, "consume", id, 1, ctx);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        Player p = e.getPlayer();
        NamespacedKey key = e.getAdvancement().getKey();
        String id = key.toString().toLowerCase(Locale.ROOT);
        TrackContext ctx = TrackContext.of("advancement").advancement(id);
        route(p, "advancement", id, 1, ctx);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location to = e.getTo();
        Location from = e.getFrom();
        if (to == null || from == null) return;
        if (from.getWorld() == null || to.getWorld() == null) return;
        long now = System.currentTimeMillis();
        DistanceState state = distanceStates.computeIfAbsent(p.getUniqueId(), k -> new DistanceState());
        if (state.anchor == null || !state.anchor.getWorld().equals(to.getWorld())) {
            state.anchor = to.clone();
            state.lastSampleMs = now;
            return;
        }
        if (from.getWorld() != to.getWorld()) {
            state.anchor = to.clone();
            state.lastSampleMs = now;
            return;
        }
        double sq = from.distanceSquared(to);
        if (sq < 1e-4) return;
        if (sq > 4096) { // teleport guard
            state.anchor = to.clone();
            state.lastSampleMs = now;
            return;
        }
        long elapsed = now - state.lastSampleMs;
        if (elapsed < 50) return;
        double distance = state.anchor.distance(to);
        state.anchor = to.clone();
        state.lastSampleMs = now;
        if (distance <= 0) return;

        String mode = distanceMode(p);
        TrackContext base = TrackContext.of("distance").mode(mode).distance(distance).sampleMs((int) elapsed);
        processDistance(p, base, now);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        clearDistance(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        clearDistance(e.getEntity().getUniqueId());
    }

    public void signalRegionStay(Player p, String regionId) {
        String id = regionId == null ? "" : regionId.toLowerCase(Locale.ROOT);
        for (GoalDef d : plugin.goals().trackedBy("region_stay")) {
            GoalDef.TrackMatcher matcher = d.trackMatchers.get("region_stay");
            if (matcher == null || !matcher.matches(id)) continue;
            plugin.progress().add(p, d, 1);
        }
    }

    private void processDistance(Player player, TrackContext base, long now) {
        List<GoalDef> defs = List.copyOf(plugin.goals().trackedBy("distance"));
        if (defs.isEmpty()) return;
        for (GoalDef def : defs) {
            List<GoalDef.TrackSpec> specs = def.trackSpecs.get("distance");
            if (specs == null || specs.isEmpty()) continue;
            for (GoalDef.TrackSpec spec : specs) {
                if (!matchesDistance(spec, base)) continue;
                DistanceBuffer buffer = distanceBuffer(player.getUniqueId(), bufferKey(def, spec));
                if (spec.distanceSampleMs() > 0 && buffer.lastMs > 0 && now - buffer.lastMs < spec.distanceSampleMs()) {
                    continue;
                }
                double min = spec.distanceMinMeters() > 0 ? spec.distanceMinMeters() : DEFAULT_DISTANCE_MIN;
                if (base.distanceMeters() < min) continue;
                buffer.lastMs = now;
                buffer.pending += base.distanceMeters();
                int gain = (int) Math.floor(buffer.pending);
                if (gain <= 0) continue;
                buffer.pending -= gain;
                TrackContext ctx = TrackContext.of("distance").mode(base.mode()).distance(gain).sampleMs(base.sampleMs());
                plugin.progress().increment(player, def, gain, ctx);
            }
        }
    }

    private boolean matchesDistance(GoalDef.TrackSpec spec, TrackContext ctx) {
        String mode = ctx.mode();
        if (spec.modeFilter() != null && !spec.modeFilter().equals(mode)) return false;
        if (spec.matchesAny()) return true;
        if (mode != null && spec.matchesId(mode)) return true;
        return false;
    }

    private void route(Player player, String kind, String id, long delta, TrackContext ctx) {
        if (player == null) return;
        String normalizedKind = kind.toLowerCase(Locale.ROOT);
        String normalizedId = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (GoalDef def : plugin.goals().trackedBy(normalizedKind)) {
            if (def.type == GoalType.CHECKLIST) {
                handleChecklist(player, def, normalizedKind, normalizedId);
                continue;
            }
            if (normalizedKind.equals("distance")) continue;
            if (normalizedKind.equals("region_stay")) continue;
            if (!handleSpecs(player, def, normalizedKind, normalizedId, delta, ctx)) {
                GoalDef.TrackMatcher matcher = def.trackMatchers.get(normalizedKind);
                if (matcher != null && matcher.matches(normalizedId)) {
                    plugin.progress().increment(player, def, delta, ctx);
                }
            }
        }
    }

    private void handleChecklist(Player player, GoalDef def, String kind, String id) {
        boolean changed = false;
        Map<String, List<GoalDef.ChecklistMatcher>> matchers = def.checklistMatchers;
        if (matchers != null) {
            List<GoalDef.ChecklistMatcher> items = matchers.get(kind);
            if (items != null && !items.isEmpty()) {
                Progress pr = null;
                for (GoalDef.ChecklistMatcher matcher : items) {
                    if (!matcher.matches(id)) continue;
                    if (pr == null) pr = plugin.progress().get(player.getUniqueId(), def);
                    if ((pr.checklistBits & matcher.bit()) != 0) continue;
                    pr.checklistBits |= matcher.bit();
                    pr.value = Long.bitCount(pr.checklistBits);
                    if (!pr.completed && pr.value >= def.target) pr.completed = true;
                    changed = true;
                }
                if (changed) plugin.progress().flushAll();
            }
        }
    }

    private boolean handleSpecs(Player player, GoalDef def, String kind, String id, long delta, TrackContext ctx) {
        List<GoalDef.TrackSpec> specs = def.trackSpecs.get(kind);
        if (specs == null || specs.isEmpty()) return false;
        for (GoalDef.TrackSpec spec : specs) {
            if (!matchesSpec(kind, spec, id, ctx)) continue;
            plugin.progress().increment(player, def, delta, ctx);
            return true;
        }
        return false;
    }

    private boolean matchesSpec(String kind, GoalDef.TrackSpec spec, String id, TrackContext ctx) {
        switch (kind) {
            case "trade":
                return matchesTrade(spec, ctx);
            case "enchant":
                return matchesEnchant(spec, ctx);
            case "brew":
                return matchesBrew(spec, ctx, id);
            case "consume":
                return matchesConsume(spec, ctx, id);
            default:
                return spec.matchesId(id);
        }
    }

    private boolean matchesTrade(GoalDef.TrackSpec spec, TrackContext ctx) {
        if (ctx == null) return false;
        String prof = ctx.tradeProfession();
        String levelName = ctx.tradeLevelName();
        String numeric = ctx.tradeLevel() > 0 ? String.valueOf(ctx.tradeLevel()) : null;
        if (!spec.matchesAny()) {
            boolean hit = false;
            if (prof != null && spec.matchesId(prof)) hit = true;
            if (!hit && levelName != null && spec.matchesId(levelName)) hit = true;
            if (!hit && prof != null && levelName != null && spec.matchesId(prof + ":" + levelName)) hit = true;
            if (!hit && numeric != null && spec.matchesId(numeric)) hit = true;
            if (!hit) return false;
        }
        if (spec.merchantProfession() != null && (prof == null || !spec.merchantProfession().equals(prof))) return false;
        return true;
    }

    private boolean matchesEnchant(GoalDef.TrackSpec spec, TrackContext ctx) {
        if (ctx == null) return false;
        String key = ctx.enchantKey();
        if (!spec.matchesId(key)) return false;
        int level = ctx.enchantLevel();
        if (spec.levelMin() > 0 && level < spec.levelMin()) return false;
        if (spec.levelMax() > 0 && level > spec.levelMax()) return false;
        return true;
    }

    private boolean matchesBrew(GoalDef.TrackSpec spec, TrackContext ctx, String id) {
        if (spec.matchesAny()) return true;
        if (ctx != null) {
            if (ctx.potionKey() != null && spec.matchesId(ctx.potionKey())) return true;
            if (ctx.itemId() != null && spec.matchesId(ctx.itemId())) return true;
        }
        return spec.matchesId(id);
    }

    private boolean matchesConsume(GoalDef.TrackSpec spec, TrackContext ctx, String id) {
        if (spec.matchesAny()) return true;
        if (ctx != null) {
            if (ctx.itemId() != null && spec.matchesId(ctx.itemId())) return true;
            if (ctx.potionKey() != null && spec.matchesId(ctx.potionKey())) return true;
        }
        return spec.matchesId(id);
    }

    private DistanceBuffer distanceBuffer(UUID uuid, String key) {
        Map<String, DistanceBuffer> map = distanceProgress.computeIfAbsent(uuid, k -> new HashMap<>());
        return map.computeIfAbsent(key, k -> new DistanceBuffer());
    }

    private String bufferKey(GoalDef def, GoalDef.TrackSpec spec) {
        return def.key + "#" + spec.index();
    }

    private void clearDistance(UUID uuid) {
        distanceStates.remove(uuid);
        distanceProgress.remove(uuid);
    }

    private String resolvePotionKey(ItemStack stack) {
        if (stack == null) return null;
        Material type = stack.getType();
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) {
            try {
                PotionData data = potionMeta.getBasePotionData();
                if (data != null) {
                    PotionType typeEnum = data.getType();
                    String key = potionTypeKey(typeEnum);
                    if (key != null) return key;
                }
            } catch (Throwable ignored) {}
        }
        return type == null ? null : type.name().toLowerCase(Locale.ROOT);
    }

    private static Method findPotionTypeKey() {
        try {
            return PotionType.class.getMethod("getKey");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private String potionTypeKey(PotionType type) {
        if (type == null) return null;
        if (POTION_TYPE_KEY_METHOD != null) {
            try {
                Object obj = POTION_TYPE_KEY_METHOD.invoke(type);
                if (obj instanceof NamespacedKey key) {
                    return key.toString().toLowerCase(Locale.ROOT);
                }
            } catch (Throwable ignored) {}
        }
        return type.name().toLowerCase(Locale.ROOT);
    }

    private String villagerLevelName(int level) {
        return switch (level) {
            case 1 -> "novice";
            case 2 -> "apprentice";
            case 3 -> "journeyman";
            case 4 -> "expert";
            case 5 -> "master";
            default -> null;
        };
    }

    private String distanceMode(Player player) {
        if (player.isGliding()) return "elytra";
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            String type = vehicle.getType().name().toLowerCase(Locale.ROOT);
            for (String boat : BOAT_TYPES) {
                if (type.contains(boat)) return "boat";
            }
        }
        return "on_foot";
    }

    private static final class DistanceState {
        private Location anchor;
        private long lastSampleMs;
    }

    private static final class DistanceBuffer {
        private double pending;
        private long lastMs;
    }
}
