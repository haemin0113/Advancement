package org.haemin.advancement.service;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TrackContext {
    private final String source;
    private final Map<String, String> extras = new HashMap<>();
    private String blockId;
    private Material blockType;
    private String entityId;
    private EntityType entityType;
    private String itemId;
    private Material itemType;
    private String enchantKey;
    private Enchantment enchantment;
    private int enchantLevel;
    private String advancementKey;
    private double distanceMeters;
    private String mode;
    private int sampleMs;
    private String tradeProfession;
    private int tradeLevel;
    private String tradeLevelName;
    private String potionKey;

    private TrackContext(String source) {
        this.source = source == null ? "" : source.toLowerCase(Locale.ROOT);
        store("source", this.source);
        store("kind", this.source);
    }

    public static TrackContext of(String source) {
        return new TrackContext(source);
    }

    public TrackContext extra(String key, String value) {
        store(key, value);
        return this;
    }

    public TrackContext block(Material type) {
        this.blockType = type;
        if (type != null) {
            this.blockId = type.name().toLowerCase(Locale.ROOT);
            store("block", this.blockId);
            store("block_type", this.blockId);
        }
        return this;
    }

    public TrackContext blockId(String id) {
        this.blockId = id == null ? null : id.toLowerCase(Locale.ROOT);
        store("block", this.blockId);
        store("block_type", this.blockId);
        return this;
    }

    public TrackContext entity(EntityType type) {
        this.entityType = type;
        if (type != null) {
            this.entityId = type.name().toLowerCase(Locale.ROOT);
            store("entity", this.entityId);
            store("entity_type", this.entityId);
            store("mob", this.entityId);
        }
        return this;
    }

    public TrackContext entityId(String id) {
        this.entityId = id == null ? null : id.toLowerCase(Locale.ROOT);
        store("entity", this.entityId);
        store("entity_type", this.entityId);
        return this;
    }

    public TrackContext item(Material type) {
        this.itemType = type;
        if (type != null) {
            this.itemId = type.name().toLowerCase(Locale.ROOT);
            store("item", this.itemId);
            store("item_type", this.itemId);
        }
        return this;
    }

    public TrackContext itemId(String id) {
        this.itemId = id == null ? null : id.toLowerCase(Locale.ROOT);
        store("item", this.itemId);
        store("item_type", this.itemId);
        return this;
    }

    public TrackContext enchant(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.enchantLevel = level;
        if (enchantment != null) {
            this.enchantKey = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
            store("enchant", this.enchantKey);
            store("enchant_key", this.enchantKey);
        }
        store("enchant_level", level > 0 ? String.valueOf(level) : null);
        return this;
    }

    public TrackContext enchantKey(String key, int level) {
        this.enchantKey = key == null ? null : key.toLowerCase(Locale.ROOT);
        this.enchantLevel = level;
        store("enchant", this.enchantKey);
        store("enchant_key", this.enchantKey);
        store("enchant_level", level > 0 ? String.valueOf(level) : null);
        return this;
    }

    public TrackContext advancement(String key) {
        this.advancementKey = key == null ? null : key.toLowerCase(Locale.ROOT);
        store("advancement", this.advancementKey);
        store("advancement_key", this.advancementKey);
        return this;
    }

    public TrackContext distance(double meters) {
        this.distanceMeters = meters;
        store("distance", meters > 0 ? String.valueOf(meters) : null);
        store("distance_meters", meters > 0 ? String.valueOf(meters) : null);
        return this;
    }

    public TrackContext mode(String mode) {
        this.mode = mode == null ? null : mode.toLowerCase(Locale.ROOT);
        store("mode", this.mode);
        store("distance_mode", this.mode);
        return this;
    }

    public TrackContext sampleMs(int ms) {
        this.sampleMs = ms;
        store("sample_ms", ms > 0 ? String.valueOf(ms) : null);
        store("distance_sample_ms", ms > 0 ? String.valueOf(ms) : null);
        return this;
    }

    public TrackContext trade(String profession, int level, String levelName) {
        this.tradeProfession = profession == null ? null : profession.toLowerCase(Locale.ROOT);
        this.tradeLevel = level;
        this.tradeLevelName = levelName == null ? null : levelName.toLowerCase(Locale.ROOT);
        store("trade_profession", this.tradeProfession);
        store("merchant_profession", this.tradeProfession);
        store("trade_level", level > 0 ? String.valueOf(level) : null);
        store("trade_level_name", this.tradeLevelName);
        store("trade_tier", this.tradeLevelName);
        return this;
    }

    public TrackContext potion(String key) {
        this.potionKey = key == null ? null : key.toLowerCase(Locale.ROOT);
        store("potion", this.potionKey);
        store("potion_key", this.potionKey);
        return this;
    }

    public String source() { return source; }
    public String blockId() { return blockId; }
    public Material blockType() { return blockType; }
    public String entityId() { return entityId; }
    public EntityType entityType() { return entityType; }
    public String itemId() { return itemId; }
    public Material itemType() { return itemType; }
    public String enchantKey() { return enchantKey; }
    public Enchantment enchantment() { return enchantment; }
    public int enchantLevel() { return enchantLevel; }
    public String advancementKey() { return advancementKey; }
    public double distanceMeters() { return distanceMeters; }
    public String mode() { return mode; }
    public int sampleMs() { return sampleMs; }
    public String tradeProfession() { return tradeProfession; }
    public int tradeLevel() { return tradeLevel; }
    public String tradeLevelName() { return tradeLevelName; }
    public String potionKey() { return potionKey; }

    public String valueFor(String key) {
        if (key == null || key.isEmpty()) return null;
        String normalized = key.toLowerCase(Locale.ROOT);
        String direct = extras.get(normalized);
        if (direct != null && !direct.isEmpty()) return direct;
        return switch (normalized) {
            case "block", "block_type" -> blockId;
            case "entity", "entity_type" -> entityId;
            case "item", "item_type" -> itemId;
            case "enchant", "enchant_key" -> enchantKey;
            case "advancement", "advancement_key" -> advancementKey;
            case "mode", "distance_mode" -> mode;
            case "trade_profession", "merchant_profession" -> tradeProfession;
            case "trade_level" -> tradeLevelName != null ? tradeLevelName : (tradeLevel <= 0 ? null : String.valueOf(tradeLevel));
            case "trade_level_name", "trade_tier" -> tradeLevelName;
            case "potion", "potion_key" -> potionKey;
            case "enchant_level" -> enchantLevel <= 0 ? null : String.valueOf(enchantLevel);
            case "distance", "distance_meters" -> distanceMeters <= 0 ? null : String.valueOf(distanceMeters);
            case "sample_ms", "distance_sample_ms" -> sampleMs <= 0 ? null : String.valueOf(sampleMs);
            case "source", "kind" -> source;
            default -> null;
        };
    }

    private void store(String key, String value) {
        if (key == null || key.isEmpty()) return;
        if (value == null || value.isEmpty()) {
            extras.remove(key.toLowerCase(Locale.ROOT));
            return;
        }
        extras.put(key.toLowerCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
    }
}
