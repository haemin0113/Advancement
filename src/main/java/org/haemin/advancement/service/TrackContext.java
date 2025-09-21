package org.haemin.advancement.service;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.enchantments.Enchantment;

import java.util.Locale;

public class TrackContext {
    private final String source;
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
    }

    public static TrackContext of(String source) {
        return new TrackContext(source);
    }

    public TrackContext block(Material type) {
        this.blockType = type;
        if (type != null) this.blockId = type.name().toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext blockId(String id) {
        this.blockId = id == null ? null : id.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext entity(EntityType type) {
        this.entityType = type;
        if (type != null) this.entityId = type.name().toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext entityId(String id) {
        this.entityId = id == null ? null : id.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext item(Material type) {
        this.itemType = type;
        if (type != null) this.itemId = type.name().toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext itemId(String id) {
        this.itemId = id == null ? null : id.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext enchant(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.enchantLevel = level;
        if (enchantment != null) this.enchantKey = enchantment.getKey().getKey().toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext enchantKey(String key, int level) {
        this.enchantKey = key == null ? null : key.toLowerCase(Locale.ROOT);
        this.enchantLevel = level;
        return this;
    }

    public TrackContext advancement(String key) {
        this.advancementKey = key == null ? null : key.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext distance(double meters) {
        this.distanceMeters = meters;
        return this;
    }

    public TrackContext mode(String mode) {
        this.mode = mode == null ? null : mode.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext sampleMs(int ms) {
        this.sampleMs = ms;
        return this;
    }

    public TrackContext trade(String profession, int level, String levelName) {
        this.tradeProfession = profession == null ? null : profession.toLowerCase(Locale.ROOT);
        this.tradeLevel = level;
        this.tradeLevelName = levelName == null ? null : levelName.toLowerCase(Locale.ROOT);
        return this;
    }

    public TrackContext potion(String key) {
        this.potionKey = key == null ? null : key.toLowerCase(Locale.ROOT);
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
        return switch (normalized) {
            case "block", "block_type" -> blockId;
            case "entity", "entity_type" -> entityId;
            case "item", "item_type" -> itemId;
            case "enchant", "enchant_key" -> enchantKey;
            case "advancement", "advancement_key" -> advancementKey;
            case "mode" -> mode;
            case "trade_profession" -> tradeProfession;
            case "trade_level" -> tradeLevelName != null ? tradeLevelName : (tradeLevel <= 0 ? null : String.valueOf(tradeLevel));
            default -> null;
        };
    }
}
