package org.haemin.advancement.service;

import io.papermc.paper.event.player.PlayerAnvilRepairEvent;
import io.papermc.paper.event.player.PlayerSmithItemEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Merchant;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.haemin.advancement.AdvancementPlugin;
import org.haemin.advancement.model.GoalDef;
import org.haemin.advancement.model.GoalType;
import org.haemin.advancement.model.Progress;
import org.haemin.advancement.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventRouter implements Listener {
    private static final double DEFAULT_DISTANCE_MIN = 0.2D;
    private static final long ANVIL_GRACE_MS = 200L;
    private static final long SMITH_GRACE_MS = 200L;

    private final AdvancementPlugin plugin;
    private final Map<UUID, DistanceState> distanceStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> recentAnvil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> recentSmith = new ConcurrentHashMap<>();

    public EventRouter(AdvancementPlugin plugin) {
        this.plugin = plugin;
        List<String> hooks = List.of(
                "break", "kill", "fish", "craft", "pickup",
                "harvest", "shear", "breed", "tame", "trade",
                "enchant", "anvil", "smithing", "brew", "consume",
                "distance", "advancement"
        );
        for (String hook : hooks) Log.info("Hooked preset listener: " + hook);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        String id = e.getBlock().getType().name().toLowerCase(Locale.ROOT);
        EventContext breakCtx = new EventContext();
        breakCtx.block = id;
        handle(player, "block_break", List.of(id), 1, breakCtx);
        if (isMatureCrop(e.getBlock())) {
            EventContext harvestCtx = new EventContext();
            harvestCtx.block = id;
            handle(player, "harvest", List.of(id), 1, harvestCtx);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        String id = e.getEntityType().name().toLowerCase(Locale.ROOT);
        handle(killer, "mob_kill", List.of(id), 1, null);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        Player player = e.getPlayer();
        handle(player, "fish", List.of("any"), 1, null);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack result = e.getRecipe() == null ? null : e.getRecipe().getResult();
        String id = result == null ? "any" : result.getType().name().toLowerCase(Locale.ROOT);
        int delta = result == null ? 1 : Math.max(1, result.getAmount());
        handle(player, "craft", List.of(id), delta, null);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player player)) return;
        ItemStack stack = e.getItem().getItemStack();
        String id = stack.getType().name().toLowerCase(Locale.ROOT);
        int delta = Math.max(1, stack.getAmount());
        handle(player, "pickup", List.of(id), delta, null);
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        if (e.isCancelled()) return;
        Player player = e.getPlayer();
        String id = e.getEntity().getType().name().toLowerCase(Locale.ROOT);
        EventContext ctx = new EventContext();
        ctx.entity = id;
        handle(player, "shear", List.of(id), 1, ctx);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player player)) return;
        String id = e.getEntityType().name().toLowerCase(Locale.ROOT);
        EventContext ctx = new EventContext();
        ctx.entity = id;
        handle(player, "breed", List.of(id), 1, ctx);
    }

    @EventHandler
    public void onTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;
        String id = e.getEntity().getType().name().toLowerCase(Locale.ROOT);
        EventContext ctx = new EventContext();
        ctx.entity = id;
        handle(player, "tame", List.of(id), 1, ctx);
    }

    @EventHandler
    public void onTrade(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        Inventory inventory = e.getInventory();
        if (!(inventory instanceof MerchantInventory merchantInventory)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        Merchant merchant = merchantInventory.getMerchant();
        String profession = merchantProfession(merchant);
        int level = merchantLevel(merchant);

        List<String> ids = new ArrayList<>();
        if (!profession.isEmpty()) {
            if (level > 0) ids.add(profession + ":" + level);
            ids.add(profession);
        }
        ids.add("any");

        EventContext ctx = new EventContext();
        ctx.item = itemKey(current);
        ctx.merchantProfession = profession;
        ctx.merchantLevel = level;
        handle(player, "trade", ids, 1, ctx);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        Player player = e.getEnchanter();
        Map<Enchantment, Integer> enchants = e.getEnchantsToAdd();
        if (enchants == null || enchants.isEmpty()) return;
        ItemStack item = e.getItem();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            NamespacedKey key = enchantment.getKey();
            String namespaced = key.toString().toLowerCase(Locale.ROOT);
            String simple = key.getKey().toLowerCase(Locale.ROOT);
            Set<String> ids = new HashSet<>();
            ids.add(namespaced);
            ids.add(simple);
            ids.add("any");
            EventContext ctx = new EventContext();
            ctx.enchantKey = simple;
            ctx.enchantLevel = level;
            ctx.item = itemKey(item);
            handle(player, "enchant", ids, 1, ctx);
        }
    }

    @EventHandler
    public void onAnvilRepair(PlayerAnvilRepairEvent e) {
        Player player = e.getPlayer();
        ItemStack result = e.getResult();
        if (result == null) return;
        handleAnvil(player, result);
        recentAnvil.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onAnvilClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory() instanceof AnvilInventory)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        if (recentHandled(recentAnvil, player.getUniqueId(), ANVIL_GRACE_MS)) return;
        handleAnvil(player, current);
    }

    @EventHandler
    public void onSmith(PlayerSmithItemEvent e) {
        Player player = e.getPlayer();
        ItemStack result = e.getResult();
        if (result == null) return;
        handleSmith(player, result);
        recentSmith.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onSmithClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory() instanceof SmithingInventory)) return;
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        ItemStack current = e.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;
        if (recentHandled(recentSmith, player.getUniqueId(), SMITH_GRACE_MS)) return;
        handleSmith(player, current);
    }

    @EventHandler
    public void onBrew(BrewEvent e) {
        BrewerInventory inventory = e.getContents();
        if (inventory == null) return;
        Player viewer = inventory.getViewers().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .findFirst()
                .orElse(null);
        if (viewer == null) return;
        for (int slot = 0; slot < 3; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            int amount = Math.max(1, item.getAmount());
            List<String> ids = new ArrayList<>(potionIds(item));
            ids.add(itemKey(item));
            ids.add("any");
            EventContext ctx = new EventContext();
            ctx.item = itemKey(item);
            handle(viewer, "brew", ids, amount, ctx);
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null) return;
        List<String> ids = itemIds(item);
        EventContext ctx = new EventContext();
        ctx.item = itemKey(item);
        handle(player, "consume", ids, 1, ctx);
    }

    @EventHandler
    public void onDistance(PlayerMoveEvent e) {
        Collection<GoalDef> goals = plugin.goals().trackedBy("distance");
        if (goals.isEmpty()) return;
        Player player = e.getPlayer();
        Location to = e.getTo();
        if (to == null) return;
        DistanceState state = distanceStates.computeIfAbsent(player.getUniqueId(), id -> new DistanceState());
        long now = System.currentTimeMillis();
        long interval = distanceSampleInterval(goals);
        if (state.lastLocation == null || !sameWorld(state.lastLocation, to)) {
            state.reset(to, now);
            return;
        }
        double segment = state.lastLocation.distance(to);
        state.pendingDistance += segment;
        state.lastLocation = to.clone();
        long elapsed = now - state.lastSample;
        if (elapsed < 0) elapsed = 0;
        state.lastSample = now;
        state.totalElapsed += elapsed;
        if (state.totalElapsed < interval) return;
        double distance = state.pendingDistance;
        if (distance < DEFAULT_DISTANCE_MIN) return;
        double total = state.remainder + distance;
        long meters = (long) Math.floor(total);
        state.remainder = total - meters;
        state.pendingDistance = 0;
        if (meters <= 0) return;
        String mode = detectMode(player);
        List<String> ids = List.of(mode, "any");
        EventContext ctx = new EventContext();
        ctx.distanceMode = mode;
        ctx.distanceMeters = distance;
        ctx.elapsedMillis = state.totalElapsed;
        ctx.distanceElapsedByGoal = distanceElapsed(goals, state, now);
        Set<String> progressed = handle(player, "distance", ids, meters, ctx);
        if (!progressed.isEmpty()) {
            for (String key : progressed) state.goalProgressTime.put(key, now);
            state.totalElapsed = 0;
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        Player player = e.getPlayer();
        String key = e.getAdvancement().getKey().toString().toLowerCase(Locale.ROOT);
        List<String> ids = advancementIds(key);
        EventContext ctx = new EventContext();
        ctx.advancementKey = key;
        handle(player, "advancement", ids, 1, ctx);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        distanceStates.remove(e.getPlayer().getUniqueId());
        recentAnvil.remove(e.getPlayer().getUniqueId());
        recentSmith.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        distanceStates.remove(e.getEntity().getUniqueId());
    }

    public void signalRegionStay(Player player, String regionId) {
        String id = regionId == null ? "" : regionId.toLowerCase(Locale.ROOT);
        for (GoalDef def : plugin.goals().trackedBy("region_stay")) {
            GoalDef.TrackMatcher matcher = def.trackMatchers.get("region_stay");
            if (matcher == null || !matcher.matches(id)) continue;
            plugin.progress().add(player, def, 1);
        }
    }

    private Set<String> handle(Player player, String kind, Collection<String> ids, long delta, EventContext ctx) {
        if (player == null) return Collections.emptySet();
        String normalizedKind = kind.toLowerCase(Locale.ROOT);
        List<String> normalizedIds = normalize(ids);
        Set<String> progressed = new HashSet<>();
        for (GoalDef def : plugin.goals().trackedBy(normalizedKind)) {
            if (!matchesOptions(def, ctx)) continue;
            if (def.type == GoalType.CHECKLIST) {
                Map<String, List<GoalDef.ChecklistMatcher>> matchers = def.checklistMatchers;
                if (matchers == null) continue;
                List<GoalDef.ChecklistMatcher> items = matchers.get(normalizedKind);
                if (items == null || items.isEmpty()) continue;
                boolean changed = false;
                Progress progress = null;
                for (GoalDef.ChecklistMatcher matcher : items) {
                    if (!matchesChecklist(matcher, normalizedIds)) continue;
                    if (progress == null) progress = plugin.progress().get(player.getUniqueId(), def);
                    if ((progress.checklistBits & matcher.bit()) != 0) continue;
                    progress.checklistBits |= matcher.bit();
                    progress.value = Long.bitCount(progress.checklistBits);
                    if (!progress.completed && progress.value >= def.target) progress.completed = true;
                    changed = true;
                }
                if (changed) {
                    plugin.progress().flushAll();
                    progressed.add(def.key);
                }
                continue;
            }

            GoalDef.TrackMatcher matcher = def.trackMatchers.get(normalizedKind);
            if (matcher == null || !matchesMatcher(matcher, normalizedIds)) continue;

            if (def.type == GoalType.UNIQUE) {
                Progress progress = plugin.progress().get(player.getUniqueId(), def);
                progress.value += delta;
                if (!progress.completed && progress.value >= def.target) progress.completed = true;
                plugin.progress().flushAll();
                progressed.add(def.key);
            } else {
                plugin.progress().add(player, def, delta);
                progressed.add(def.key);
            }
        }
        return progressed.isEmpty() ? Collections.emptySet() : progressed;
    }

    private List<String> normalize(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of("");
        Set<String> set = new HashSet<>();
        for (String id : ids) {
            if (id == null) continue;
            String normalized = id.toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) continue;
            set.add(normalized);
        }
        return set.isEmpty() ? List.of("") : List.copyOf(set);
    }

    private boolean matchesMatcher(GoalDef.TrackMatcher matcher, List<String> ids) {
        if (matcher.matchesAny()) return true;
        for (String id : ids) {
            if (matcher.matches(id)) return true;
        }
        return false;
    }

    private boolean matchesChecklist(GoalDef.ChecklistMatcher matcher, List<String> ids) {
        for (String id : ids) {
            if (matcher.matches(id)) return true;
        }
        return false;
    }

    private boolean matchesOptions(GoalDef def, EventContext ctx) {
        if (def.presetOptions == null || def.presetOptions.isEmpty()) return true;
        Map<String, Object> options = def.presetOptions;
        if (options.containsKey("merchant_profession")) {
            String expected = String.valueOf(options.get("merchant_profession"));
            if (ctx == null || ctx.merchantProfession == null || !Objects.equals(expected, ctx.merchantProfession)) return false;
        }
        if (options.containsKey("level_min")) {
            int min = ((Number) options.get("level_min")).intValue();
            if (ctx == null || ctx.enchantLevel < min) return false;
        }
        if (options.containsKey("level_max")) {
            int max = ((Number) options.get("level_max")).intValue();
            if (ctx == null || ctx.enchantLevel > max) return false;
        }
        if (options.containsKey("mode")) {
            String mode = String.valueOf(options.get("mode"));
            if (ctx == null || ctx.distanceMode == null || !ctx.distanceMode.equals(mode)) return false;
        }
        if (options.containsKey("distance_min_m")) {
            double min = ((Number) options.get("distance_min_m")).doubleValue();
            if (ctx == null || ctx.distanceMeters < min) return false;
        }
        if (options.containsKey("distance_sample_ms")) {
            long required = ((Number) options.get("distance_sample_ms")).longValue();
            long elapsed = ctx == null ? 0L : ctx.distanceElapsed(def.key, ctx.elapsedMillis);
            if (elapsed < required) return false;
        }
        return true;
    }

    private Map<String, Long> distanceElapsed(Collection<GoalDef> goals, DistanceState state, long now) {
        if (goals.isEmpty()) return Collections.emptyMap();
        Map<String, Long> out = new HashMap<>();
        for (GoalDef def : goals) {
            long anchor = state.goalProgressTime.computeIfAbsent(def.key, k -> Math.max(0L, now - state.totalElapsed));
            long elapsed = Math.max(0L, now - anchor);
            out.put(def.key, elapsed);
        }
        return out;
    }

    private void handleAnvil(Player player, ItemStack result) {
        String id = itemKey(result);
        EventContext ctx = new EventContext();
        ctx.item = id;
        handle(player, "anvil", List.of(id, "any"), 1, ctx);
    }

    private void handleSmith(Player player, ItemStack result) {
        String id = itemKey(result);
        EventContext ctx = new EventContext();
        ctx.item = id;
        handle(player, "smithing", List.of(id, "any"), 1, ctx);
    }

    private boolean isMatureCrop(Block block) {
        if (block == null) return false;
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private boolean recentHandled(Map<UUID, Long> recent, UUID uuid, long grace) {
        Long ts = recent.get(uuid);
        if (ts == null) return false;
        if (System.currentTimeMillis() - ts <= grace) return true;
        recent.remove(uuid);
        return false;
    }

    private String merchantProfession(Merchant merchant) {
        if (merchant instanceof Villager villager) return villager.getProfession().name().toLowerCase(Locale.ROOT);
        if (merchant instanceof AbstractVillager) return merchant.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return "";
    }

    private int merchantLevel(Merchant merchant) {
        if (merchant instanceof Villager villager) return villager.getVillagerLevel();
        return 0;
    }

    private String itemKey(ItemStack item) {
        if (item == null) return "any";
        return item.getType().name().toLowerCase(Locale.ROOT);
    }

    private List<String> itemIds(ItemStack item) {
        List<String> ids = new ArrayList<>();
        ids.add(itemKey(item));
        if (isPotion(item)) ids.addAll(potionIds(item));
        ids.add("any");
        return ids;
    }

    private boolean isPotion(ItemStack item) {
        return item != null && item.getItemMeta() instanceof PotionMeta;
    }

    private List<String> potionIds(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta meta)) return List.of();
        PotionData data = meta.getBasePotionData();
        PotionType type = data.getType();
        String base = type == null ? "unknown" : type.name().toLowerCase(Locale.ROOT);
        StringBuilder key = new StringBuilder(base);
        if (data.isUpgraded()) key.insert(0, "strong_");
        if (data.isExtended()) key.insert(0, "long_");
        List<String> ids = new ArrayList<>();
        ids.add(key.toString());
        ids.add("potion:" + key);
        return ids;
    }

    private List<String> advancementIds(String key) {
        List<String> ids = new ArrayList<>();
        ids.add(key);
        int idx = key.lastIndexOf('/');
        while (idx > 0) {
            ids.add(key.substring(0, idx) + "/*");
            idx = key.lastIndexOf('/', idx - 1);
        }
        int colon = key.indexOf(':');
        if (colon > 0) ids.add(key.substring(0, colon) + ":*");
        ids.add("*");
        return ids;
    }

    private String detectMode(Player player) {
        if (player.isGliding()) return "elytra";
        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getType() == EntityType.BOAT) return "boat";
        return "on_foot";
    }

    private long distanceSampleInterval(Collection<GoalDef> goals) {
        long interval = 250L;
        for (GoalDef def : goals) {
            if (def.presetOptions == null) continue;
            Object value = def.presetOptions.get("distance_sample_ms");
            if (value instanceof Number number) {
                long candidate = number.longValue();
                if (candidate > 0) interval = Math.min(interval, candidate);
            }
        }
        return Math.max(50L, interval);
    }

    private boolean sameWorld(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld());
    }

    private static final class EventContext {
        String entity;
        String item;
        String enchantKey;
        int enchantLevel;
        String merchantProfession;
        int merchantLevel;
        String distanceMode;
        double distanceMeters;
        long elapsedMillis;
        String advancementKey;
        String block;
        Map<String, Long> distanceElapsedByGoal;

        long distanceElapsed(String goalKey, long fallback) {
            if (distanceElapsedByGoal == null || distanceElapsedByGoal.isEmpty()) return fallback;
            return distanceElapsedByGoal.getOrDefault(goalKey, fallback);
        }
    }

    private static final class DistanceState {
        Location lastLocation;
        long lastSample;
        double remainder;
        double pendingDistance;
        long totalElapsed;
        Map<String, Long> goalProgressTime = new HashMap<>();

        void reset(Location location, long now) {
            this.lastLocation = location == null ? null : location.clone();
            this.lastSample = now;
            this.totalElapsed = 0L;
            this.pendingDistance = 0D;
            this.remainder = 0D;
            this.goalProgressTime.clear();
        }
    }
}
