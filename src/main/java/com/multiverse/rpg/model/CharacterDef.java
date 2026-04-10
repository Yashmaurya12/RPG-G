package com.multiverse.rpg.model;

public class CharacterDef {
    private String id;
    private String name;
    private CharacterClass characterClass;
    private int maxHp;
    private int attack;
    private int defense;
    private String specialName;
    private String specialDescription;
    private int specialDamage;
    private SpecialType specialType;
    private int specialCooldown;
    private String color1;
    private String color2;
    private String emoji;

    public enum CharacterClass { MAGE, WARRIOR, CYBORG, RANGER }
    public enum SpecialType { DIRECT_DAMAGE, ARMOR_PIERCE, SELF_HEAL, HIGH_RISK }

    public CharacterDef() {}
    public CharacterDef(String id, String name, CharacterClass cls,
                        int maxHp, int attack, int defense,
                        String specialName, String specialDesc,
                        int specialDamage, SpecialType specialType, int specialCooldown,
                        String color1, String color2, String emoji) {
        this.id = id; this.name = name; this.characterClass = cls;
        this.maxHp = maxHp; this.attack = attack; this.defense = defense;
        this.specialName = specialName; this.specialDescription = specialDesc;
        this.specialDamage = specialDamage; this.specialType = specialType;
        this.specialCooldown = specialCooldown;
        this.color1 = color1; this.color2 = color2; this.emoji = emoji;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CharacterClass getCharacterClass() { return characterClass; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public String getSpecialName() { return specialName; }
    public String getSpecialDescription() { return specialDescription; }
    public int getSpecialDamage() { return specialDamage; }
    public SpecialType getSpecialType() { return specialType; }
    public int getSpecialCooldown() { return specialCooldown; }
    public String getColor1() { return color1; }
    public String getColor2() { return color2; }
    public String getEmoji() { return emoji; }
}
