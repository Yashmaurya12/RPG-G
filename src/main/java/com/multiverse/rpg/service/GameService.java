package com.multiverse.rpg.service;

import com.multiverse.rpg.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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
        boolean isP2 = s.getPlayer2Name().equals(playerName);
        if (!isP1 && !isP2) return "Player not in session";

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

        switch (action.toUpperCase()) {
            case "ATTACK" -> {
                int raw = atk.getAttack() + rng.nextInt(7) - 3;
                int block = defIsDefending ? def.getDefense() : def.getDefense() / 3;
                int dmg = Math.max(3, raw - block);
                defenderHp -= dmg;
                s.getCombatLog().add("⚔️ " + playerName + " attacks for " + dmg + " damage!" + (defIsDefending ? " (blocked!)" : ""));
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
            case "FORFEIT" -> {
                if (isP1) s.setHp1(0); else s.setHp2(0);
                s.getCombatLog().add("💨 " + playerName + " ran away and forfeited the battle!");
            }
            default -> { return "Unknown action"; }
        }

        if (!action.equalsIgnoreCase("DEFEND") && !action.equalsIgnoreCase("SPECIAL")) {
            if (isP1) s.setHp2(defenderHp); else s.setHp1(defenderHp);
        } else if (action.equalsIgnoreCase("SPECIAL")) {
            if (isP1) s.setHp2(defenderHp); else s.setHp1(defenderHp);
        }

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
            if (next.equals(s.getPlayer1Name()) && s.getSpecial1Cooldown() > 0) s.setSpecial1Cooldown(s.getSpecial1Cooldown() - 1);
            else if (next.equals(s.getPlayer2Name()) && s.getSpecial2Cooldown() > 0) s.setSpecial2Cooldown(s.getSpecial2Cooldown() - 1);
        }

        broadcastGameState(s);
        return null;
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

        m.put("player1", buildPlayerState(s.getPlayer1Name(), s.getCharacter1(), s.getHp1(),
                s.isPlayer1Defending(), s.getSpecial1Cooldown(), s.isPlayer1Selected()));
        m.put("player2", buildPlayerState(s.getPlayer2Name(), s.getCharacter2(), s.getHp2(),
                s.isPlayer2Defending(), s.getSpecial2Cooldown(), s.isPlayer2Selected()));
        return m;
    }

    private Map<String, Object> buildPlayerState(String name, CharacterDef c, int hp,
                                                  boolean defending, int spCd, boolean selected) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("selected", selected);
        p.put("hp", hp);
        p.put("defending", defending);
        p.put("specialCooldown", spCd);
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
