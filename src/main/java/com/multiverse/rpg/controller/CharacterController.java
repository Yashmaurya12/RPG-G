package com.multiverse.rpg.controller;

import com.multiverse.rpg.model.CharacterDef;
import com.multiverse.rpg.model.CharacterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    @Autowired private CharacterRegistry registry;

    @GetMapping
    public Map<String, List<Map<String, Object>>> getCharacters() {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (CharacterDef.CharacterClass cls : CharacterDef.CharacterClass.values()) {
            grouped.put(cls.name(), new ArrayList<>());
        }
        for (CharacterDef c : registry.getAll()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("characterClass", c.getCharacterClass().name());
            m.put("maxHp", c.getMaxHp());
            m.put("attack", c.getAttack());
            m.put("defense", c.getDefense());
            m.put("specialName", c.getSpecialName());
            m.put("specialDescription", c.getSpecialDescription());
            m.put("specialDamage", c.getSpecialDamage());
            m.put("specialCooldown", c.getSpecialCooldown());
            m.put("color1", c.getColor1());
            m.put("color2", c.getColor2());
            m.put("emoji", c.getEmoji());
            grouped.get(c.getCharacterClass().name()).add(m);
        }
        return grouped;
    }
}
