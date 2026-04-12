package com.multiverse.rpg.model;

public class LootItem {
    private String id;
    private String name;
    private String emoji;
    private String description;
    private EffectType effectType;
    private int value;

    public enum EffectType { DAMAGE, HEAL, BUFF_ATK, BUFF_DEF }

    public LootItem(String id, String name, String emoji, String description, EffectType type, int value) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.description = description;
        this.effectType = type;
        this.value = value;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public EffectType getEffectType() { return effectType; }
    public int getValue() { return value; }
}
