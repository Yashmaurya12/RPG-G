package com.multiverse.rpg.service;

import com.multiverse.rpg.model.CharacterDef;
import com.multiverse.rpg.model.CharacterRegistry;
import com.multiverse.rpg.model.GameSession;
import com.multiverse.rpg.model.LootItem;
import com.multiverse.rpg.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    @Autowired private SimpMessagingTemplate messaging;
    @Autowired private CharacterRegistry registry;
    @Autowired private LobbyService lobbyService;

    public GameSession createSession(String p1, String p2) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        GameSession session = new GameSession(id, p1, p2);
        sessions.put(id, session);
        lobbyService.updateStatus(p1, Player.PlayerStatus.IN_CHARACTER_SELECT);
        lobbyService.updateStatus(p2, Player.PlayerStatus.IN_CHARACTER_SELECT);
        return session;
    }

    public GameSession getSession(String id) { return sessions.get(id); }

    public synchronized String selectCharacter(String sessionId, String playerName, String charId) {
        GameSession s = sessions.get(sessionId);
        if (s == null) return "Session not found";
        if (s.getState() != GameSession.GameState.CHARACTER_SELECT) return "Wrong phase";
        CharacterDef c = registry.findById(charId);
        if (c == null) return "Unknown character";

        boolean isP1 = s.getPlayer1Name().equals(playerName);
        if (isP1) { s.setCharacter1(c); s.setPlayer1Selected(true); }
        else      { s.setCharacter2(c); s.setPlayer2Selected(true); }

        broadcastGameState(s);

        if (s.isPlayer1Selected() && s.isPlayer2Selected()) {
            s.startBattle();
            lobbyService.updateStatus(s.getPlayer1Name(), Player.PlayerStatus.IN_GAME);
            lobbyService.updateStatus(s.getPlayer2Name(), Player.PlayerStatus.IN_GAME);
            broadcastGameState(s);
        }
        return null;
    }

    public synchronized String processTurn(String sessionId, String playerName, String action) {
        GameSession s = sessions.get(sessionId);
        if (s == null) return "Session not found";
        if (s.getState() != GameSession.GameState.BATTLE) return "Game not in battle";
        if (!action.equalsIgnoreCase("FORFEIT") && !s.getCurrentTurnName().equals(playerName)) return "Not your turn";

        boolean isP1 = s.getPlayer1Name().equals(playerName);
        CharacterDef atk = isP1 ? s.getCharacter1() : s.getCharacter2();
        CharacterDef def = isP1 ? s.getCharacter2() : s.getCharacter1();
        int defenderHp = isP1 ? s.getHp2() : s.getHp1();
        int attackerHp = isP1 ? s.getHp1() : s.getHp2();
        boolean defIsDefending = isP1 ? s.isPlayer2Defending() : s.isPlayer1Defending();
        int spCd = isP1 ? s.getSpecial1Cooldown() : s.getSpecial2Cooldown();
        LootItem heldLoot = isP1 ? s.getP1Loot() : s.getP2Loot();

        // --- 1. Reality Modifiers ---
        double mod = getRealityModifier(s.getCurrentReality(), atk.getCharacterClass());

        switch (action.toUpperCase()) {
            case "ATTACK" -> {
                int raw = (int)((atk.getAttack() + rng.nextInt(7) - 3) * mod);
                int block = defIsDefending ? def.getDefense() : def.getDefense() / 3;
                int dmg = Math.max(3, raw - block);
                defenderHp -= dmg;
                s.getCombatLog().add("⚔️ " + playerName + " attacks for " + dmg + " damage!" + (defIsDefending ? " (blocked!)" : "") + (mod > 1.1 ? " [REALITY BUFF!]" : mod < 0.9 ? " [REALITY MALFUNCTION!]" : ""));
                if (isP1) s.setPlayer2Defending(false); else s.setPlayer1Defending(false);
            }
            case "DEFEND" -> {
                if (isP1) s.setPlayer1Defending(true); else s.setPlayer2Defending(true);
                s.getCombatLog().add("🛡️ " + playerName + " braces for impact!");
            }
            case "SPECIAL" -> {
                if (spCd > 0) return "Special on cooldown (" + spCd + " turns left)";
                int dmg = 0; int heal = 0; boolean hit = true;
                switch (atk.getSpecialType()) {
                    case DIRECT_DAMAGE -> { dmg = atk.getSpecialDamage(); if (defIsDefending) dmg /= 2; }
                    case ARMOR_PIERCE  -> dmg = atk.getSpecialDamage();
                    case SELF_HEAL     -> { dmg = atk.getSpecialDamage(); heal = 20; }
                    case HIGH_RISK     -> { hit = rng.nextInt(100) < 65; if (hit) dmg = atk.getSpecialDamage(); }
                }
                dmg = (int)(dmg * mod);
                if (hit) {
                    defenderHp -= dmg;
                    attackerHp = Math.min(isP1 ? s.getCharacter1().getMaxHp() : s.getCharacter2().getMaxHp(), attackerHp + heal);
                    if (isP1) { s.setHp1(attackerHp); } else { s.setHp2(attackerHp); }
                    s.getCombatLog().add("✨ " + playerName + " uses " + atk.getSpecialName() + "! " + dmg + " dmg" + (heal > 0 ? " + " + heal + " heal" : "") + "!");
                } else {
                    s.getCombatLog().add("💨 " + playerName + " uses " + atk.getSpecialName() + " but MISSES!");
                }
                if (isP1) { s.setSpecial1Cooldown(atk.getSpecialCooldown()); s.setPlayer2Defending(false); }
                else      { s.setSpecial2Cooldown(atk.getSpecialCooldown()); s.setPlayer1Defending(false); }
            }
            case "GRAB_LOOT" -> {
                if (s.getActiveLoot() == null) return "No loot to grab!";
                LootItem l = s.getActiveLoot();
                if (isP1) s.setP1Loot(l); else s.setP2Loot(l);
                s.setActiveLoot(null);
                s.getCombatLog().add("🎒 " + playerName + " scavenged the [" + l.getName() + "] from the rift!");
            }
            case "USE_LOOT" -> {
                if (heldLoot == null) return "No loot held!";
                int dmg = 0; int h = 0;
                switch(heldLoot.getEffectType()) {
                    case DAMAGE -> dmg = heldLoot.getValue();
                    case HEAL -> h = heldLoot.getValue();
                    case BUFF_ATK -> dmg = (int)(atk.getAttack() * 1.5);
                    case BUFF_DEF -> {
                        if (isP1) s.setPlayer1Defending(true);
                        else s.setPlayer2Defending(true);
                    }
                }
                defenderHp -= dmg;
                attackerHp = Math.min(isP1 ? s.getCharacter1().getMaxHp() : s.getCharacter2().getMaxHp(), attackerHp + h);
                s.getCombatLog().add("💥 " + playerName + " uses the " + heldLoot.getName() + "! " + (dmg > 0 ? dmg + " pure damage!" : "") + (h > 0 ? h + " healing!" : ""));
                if (isP1) { s.setP1Loot(null); s.setHp1(attackerHp); } else { s.setP2Loot(null); s.setHp2(attackerHp); }
            }
            case "FORFEIT" -> {
                if (isP1) s.setHp1(0); else s.setHp2(0);
                s.getCombatLog().add("💨 " + playerName + " ran away and forfeited the battle!");
            }
            default -> { return "Unknown action"; }
        }

        // Apply health updates
        if (isP1) s.setHp2(defenderHp); else s.setHp1(defenderHp);

        // --- 2. Post-Turn State Management ---
        handleTurnEnd(s);

        broadcastGameState(s);
        return null;
    }

    private void handleTurnEnd(GameSession s) {
        if (s.getHp1() <= 0 || s.getHp2() <= 0) {
            String winner = s.getHp1() > 0 ? s.getPlayer1Name() : s.getPlayer2Name();
            s.setWinner(winner);
            s.setState(GameSession.GameState.FINISHED);
            s.getCombatLog().add("🏆 " + winner + " wins the battle!");
            lobbyService.updateStatus(s.getPlayer1Name(), Player.PlayerStatus.LOBBY);
            lobbyService.updateStatus(s.getPlayer2Name(), Player.PlayerStatus.LOBBY);
        } else {
            String next = s.getCurrentTurnName().equals(s.getPlayer1Name()) ? s.getPlayer2Name() : s.getPlayer1Name();
            s.setCurrentTurnName(next);
            
            // Cooldowns
            if (next.equals(s.getPlayer1Name()) && s.getSpecial1Cooldown() > 0) s.setSpecial1Cooldown(s.getSpecial1Cooldown() - 1);
            else if (next.equals(s.getPlayer2Name()) && s.getSpecial2Cooldown() > 0) s.setSpecial2Cooldown(s.getSpecial2Cooldown() - 1);

            // Reality Shift
            s.setRealityTurnsLeft(s.getRealityTurnsLeft() - 1);
            if (s.getRealityTurnsLeft() <= 0) {
                shiftReality(s);
            }

            // Loot Spawn (15% chance if field is empty)
            if (s.getActiveLoot() == null && rng.nextInt(100) < 15) {
                spawnLoot(s);
            }
        }
    }

    private void shiftReality(GameSession s) {
        GameSession.RealityPhase current = s.getCurrentReality();
        GameSession.RealityPhase next = switch(current) {
            case NEUTRAL -> GameSession.RealityPhase.HIGH_MAGIC;
            case HIGH_MAGIC -> GameSession.RealityPhase.SCI_FI;
            case SCI_FI -> GameSession.RealityPhase.NEUTRAL;
        };
        s.setCurrentReality(next);
        s.setRealityTurnsLeft(3);
        String msg = switch(next) {
            case HIGH_MAGIC -> "🔮 Rift open! High Magic Reality detected. [Magic/Warrior BUFFED, Tech NERFED]";
            case SCI_FI -> "🛰️ Neural Pulse! Sci-Fi Reality detected. [Tech BUFFED, Magic NERFED]";
            default -> "🌀 The rift stabilizes. Reality is Neutral.";
        };
        s.getCombatLog().add(msg);
    }

    private void spawnLoot(GameSession s) {
        String[] names = {"Holy Avenger", "Plasma Turret", "Cursed Idol", "Quantum Medic", "Gravity Core"};
        String[] emojis = {"🗡️", "🔫", "🧿", "🧪", "⚙️"};
        int i = rng.nextInt(names.length);
        LootItem.EffectType type = (i == 3) ? LootItem.EffectType.HEAL : LootItem.EffectType.DAMAGE;
        int val = (type == LootItem.EffectType.HEAL) ? 35 : 40 + rng.nextInt(20);
        
        LootItem loot = new LootItem(UUID.randomUUID().toString().substring(0,4), names[i], emojis[i], "Powerful artifact", type, val);
        s.setActiveLoot(loot);
        s.getCombatLog().add("🌌 A dimensional rift dropped a [" + loot.getEmoji() + " " + loot.getName() + "] onto the field!");
    }

    private double getRealityModifier(GameSession.RealityPhase r, CharacterDef.CharacterClass cls) {
        if (r == GameSession.RealityPhase.HIGH_MAGIC) {
            if (cls == CharacterDef.CharacterClass.MAGE || cls == CharacterDef.CharacterClass.WARRIOR) return 1.25;
            if (cls == CharacterDef.CharacterClass.CYBORG || cls == CharacterDef.CharacterClass.RANGER) return 0.75;
        } else if (r == GameSession.RealityPhase.SCI_FI) {
            if (cls == CharacterDef.CharacterClass.CYBORG || cls == CharacterDef.CharacterClass.RANGER) return 1.25;
            if (cls == CharacterDef.CharacterClass.MAGE || cls == CharacterDef.CharacterClass.WARRIOR) return 0.75;
        }
        return 1.0;
    }

    public void broadcastGameState(GameSession s) {
        messaging.convertAndSend("/topic/game/" + s.getSessionId(), getStateMap(s));
    }

    public Map<String, Object> getStateMap(GameSession s) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", s.getSessionId());
        m.put("state", s.getState().name());
        m.put("currentTurn", s.getCurrentTurnName());
        m.put("winner", s.getWinner());
        m.put("combatLog", s.getCombatLog());
        m.put("currentReality", s.getCurrentReality().name());
        m.put("realityTurnsLeft", s.getRealityTurnsLeft());
        m.put("activeLoot", s.getActiveLoot());

        m.put("player1", buildPlayerState(s.getPlayer1Name(), s.getCharacter1(), s.getHp1(),
                s.isPlayer1Defending(), s.getSpecial1Cooldown(), s.isPlayer1Selected(), s.getP1Loot()));
        m.put("player2", buildPlayerState(s.getPlayer2Name(), s.getCharacter2(), s.getHp2(),
                s.isPlayer2Defending(), s.getSpecial2Cooldown(), s.isPlayer2Selected(), s.getP2Loot()));
        return m;
    }

    private Map<String, Object> buildPlayerState(String name, CharacterDef c, int hp,
                                                  boolean defending, int spCd, boolean selected, LootItem loot) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("selected", selected);
        p.put("hp", hp);
        p.put("defending", defending);
        p.put("specialCooldown", spCd);
        p.put("heldLoot", loot);
        if (c != null) {
            p.put("characterId", c.getId());
            p.put("characterName", c.getName());
            p.put("maxHp", c.getMaxHp());
            p.put("emoji", c.getEmoji());
            p.put("color1", c.getColor1());
            p.put("color2", c.getColor2());
            p.put("specialName", c.getSpecialName());
        }
        return p;
    }
}
