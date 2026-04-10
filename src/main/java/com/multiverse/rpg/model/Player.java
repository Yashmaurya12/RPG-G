package com.multiverse.rpg.model;

public class Player {
    private String name;
    private PlayerStatus status;
    private String wsSessionId;

    public enum PlayerStatus { LOBBY, CHALLENGED, IN_CHARACTER_SELECT, IN_GAME }

    public Player() {}
    public Player(String name, String wsSessionId) {
        this.name = name;
        this.status = PlayerStatus.LOBBY;
        this.wsSessionId = wsSessionId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlayerStatus getStatus() { return status; }
    public void setStatus(PlayerStatus status) { this.status = status; }
    public String getWsSessionId() { return wsSessionId; }
    public void setWsSessionId(String wsSessionId) { this.wsSessionId = wsSessionId; }
}
