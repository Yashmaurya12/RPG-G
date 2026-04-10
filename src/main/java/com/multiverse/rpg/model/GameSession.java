package com.multiverse.rpg.model;

import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private String sessionId;
    private String player1Name;
    private String player2Name;
    private CharacterDef character1;
    private CharacterDef character2;
    private boolean player1Selected;
    private boolean player2Selected;
    private int hp1;
    private int hp2;
    private boolean player1Defending;
    private boolean player2Defending;
    private int special1Cooldown;
    private int special2Cooldown;
    private String currentTurnName;
    private GameState state;
    private List<String> combatLog;
    private String winner;

    public enum GameState { CHARACTER_SELECT, BATTLE, FINISHED }

    public GameSession(String sessionId, String p1, String p2) {
        this.sessionId = sessionId;
        this.player1Name = p1;
        this.player2Name = p2;
        this.state = GameState.CHARACTER_SELECT;
        this.combatLog = new ArrayList<>();
    }

    public void startBattle() {
        this.hp1 = character1.getMaxHp();
        this.hp2 = character2.getMaxHp();
        this.currentTurnName = player1Name;
        this.state = GameState.BATTLE;
        combatLog.add("⚔️ Battle begins! " + player1Name + " vs " + player2Name + ". " + player1Name + " goes first!");
    }

    // --- Getters / Setters ---
    public String getSessionId() { return sessionId; }
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }
    public CharacterDef getCharacter1() { return character1; }
    public void setCharacter1(CharacterDef c) { this.character1 = c; }
    public CharacterDef getCharacter2() { return character2; }
    public void setCharacter2(CharacterDef c) { this.character2 = c; }
    public boolean isPlayer1Selected() { return player1Selected; }
    public void setPlayer1Selected(boolean b) { this.player1Selected = b; }
    public boolean isPlayer2Selected() { return player2Selected; }
    public void setPlayer2Selected(boolean b) { this.player2Selected = b; }
    public int getHp1() { return hp1; }
    public void setHp1(int hp1) { this.hp1 = Math.max(0, hp1); }
    public int getHp2() { return hp2; }
    public void setHp2(int hp2) { this.hp2 = Math.max(0, hp2); }
    public boolean isPlayer1Defending() { return player1Defending; }
    public void setPlayer1Defending(boolean b) { this.player1Defending = b; }
    public boolean isPlayer2Defending() { return player2Defending; }
    public void setPlayer2Defending(boolean b) { this.player2Defending = b; }
    public int getSpecial1Cooldown() { return special1Cooldown; }
    public void setSpecial1Cooldown(int n) { this.special1Cooldown = Math.max(0, n); }
    public int getSpecial2Cooldown() { return special2Cooldown; }
    public void setSpecial2Cooldown(int n) { this.special2Cooldown = Math.max(0, n); }
    public String getCurrentTurnName() { return currentTurnName; }
    public void setCurrentTurnName(String n) { this.currentTurnName = n; }
    public GameState getState() { return state; }
    public void setState(GameState s) { this.state = s; }
    public List<String> getCombatLog() { return combatLog; }
    public String getWinner() { return winner; }
    public void setWinner(String w) { this.winner = w; }
}
