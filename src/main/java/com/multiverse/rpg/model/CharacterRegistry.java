package com.multiverse.rpg.model;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class CharacterRegistry {
    private final Map<String, CharacterDef> characters = new LinkedHashMap<>();

    public CharacterRegistry() {
        // MAGE
        add(new CharacterDef("zoryn", "Zoryn the Arcane", CharacterDef.CharacterClass.MAGE,
                100, 25, 8, "Meteor Strike", "45 armor-piercing damage, ignores defense",
                45, CharacterDef.SpecialType.ARMOR_PIERCE, 3, "#7928CA", "#FF0080", "\uD83D\uDD2E"));

        add(new CharacterDef("aelindra", "Aelindra Shadowweave", CharacterDef.CharacterClass.MAGE,
                90, 30, 5, "Chain Lightning", "60 damage (65% hit rate), high-risk arc",
                60, CharacterDef.SpecialType.HIGH_RISK, 3, "#4776E6", "#8E54E9", "\u26A1"));

        // WARRIOR
        add(new CharacterDef("thorvald", "Thorvald Ironhide", CharacterDef.CharacterClass.WARRIOR,
                140, 22, 18, "Berserker Charge", "40 direct damage, unstoppable force",
                40, CharacterDef.SpecialType.DIRECT_DAMAGE, 2, "#FF4E50", "#F9D423", "\u2694\uFE0F"));

        add(new CharacterDef("kira", "Kira Steelblade", CharacterDef.CharacterClass.WARRIOR,
                120, 25, 14, "War Cry & Heal", "Deals 25 armor-pierce + heals self 20 HP",
                25, CharacterDef.SpecialType.SELF_HEAL, 3, "#B24592", "#F15F79", "\uD83D\uDEE1\uFE0F"));

        // CYBORG
        add(new CharacterDef("x9", "X-9 Annihilator", CharacterDef.CharacterClass.CYBORG,
                120, 28, 12, "Missile Barrage", "50 direct damage payload, bypasses block",
                50, CharacterDef.SpecialType.DIRECT_DAMAGE, 3, "#0072FF", "#00C6FF", "\uD83E\uDD16"));

        add(new CharacterDef("aria7", "ARIA-7", CharacterDef.CharacterClass.CYBORG,
                110, 22, 16, "System Overload", "Deals 30 damage + heals self 20 HP",
                30, CharacterDef.SpecialType.SELF_HEAL, 3, "#11998E", "#38EF7D", "\uD83D\uDD0B"));

        // RANGER
        add(new CharacterDef("lyra", "Lyra Swiftarrow", CharacterDef.CharacterClass.RANGER,
                95, 27, 10, "Triple Shot", "36 armor-piercing damage across 3 arrows",
                36, CharacterDef.SpecialType.ARMOR_PIERCE, 2, "#1DE9B6", "#1DC4E9", "\uD83C\uDFF9"));

        add(new CharacterDef("dusk", "Dusk Phantom", CharacterDef.CharacterClass.RANGER,
                85, 35, 6, "Shadow Strike", "65 damage (65% hit chance), high risk",
                65, CharacterDef.SpecialType.HIGH_RISK, 3, "#434343", "#9b59b6", "\uD83D\uDDE1\uFE0F"));
    }

    private void add(CharacterDef c) { characters.put(c.getId(), c); }
    public CharacterDef findById(String id) { return characters.get(id); }
    public Collection<CharacterDef> getAll() { return characters.values(); }
}
