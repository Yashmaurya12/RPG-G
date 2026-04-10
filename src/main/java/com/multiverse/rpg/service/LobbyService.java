package com.multiverse.rpg.service;

import com.multiverse.rpg.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {

    private final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messaging;

    /** Register player. Returns null on success, or error string. */
    public synchronized String registerPlayer(String name, String wsSessionId) {
        if (name == null || name.trim().isEmpty()) return "Name cannot be empty";
        name = name.trim();
        if (name.length() < 2 || name.length() > 20) return "Name must be 2–20 characters";
        if (players.containsKey(name.toLowerCase())) return "Name already taken — choose another";
        players.put(name.toLowerCase(), new Player(name, wsSessionId));
        broadcastLobby();
        return null;
    }

    public void removePlayerBySession(String wsSessionId) {
        players.entrySet().removeIf(e -> wsSessionId.equals(e.getValue().getWsSessionId()));
        broadcastLobby();
    }

    public void removePlayer(String name) {
        if (name != null) { players.remove(name.toLowerCase()); broadcastLobby(); }
    }

    public Player getPlayer(String name) {
        return name == null ? null : players.get(name.toLowerCase());
    }

    public void updateStatus(String name, Player.PlayerStatus status) {
        Player p = getPlayer(name);
        if (p != null) { p.setStatus(status); broadcastLobby(); }
    }

    public void broadcastLobby() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Player p : players.values()) {
            Map<String, String> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("status", p.getStatus().name());
            list.add(m);
        }
        messaging.convertAndSend("/topic/lobby", list);
    }

    public Collection<Player> getOnlinePlayers() { return players.values(); }
}
