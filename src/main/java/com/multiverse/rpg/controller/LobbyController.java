package com.multiverse.rpg.controller;

import com.multiverse.rpg.model.GameSession;
import com.multiverse.rpg.service.GameService;
import com.multiverse.rpg.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LobbyController {

    @Autowired private LobbyService lobbyService;
    @Autowired private GameService gameService;
    @Autowired private SimpMessagingTemplate messaging;

    /** Player announces they are connected */
    @MessageMapping("/join")
    public void join(@Payload Map<String, String> msg, StompHeaderAccessor accessor) {
        String name = msg.get("playerName");
        String wsId = accessor.getSessionId();
        // Update session ID if player name already registered (e.g., page reload)
        var player = lobbyService.getPlayer(name);
        if (player != null) player.setWsSessionId(wsId);
        lobbyService.broadcastLobby();
    }

    /** Player A challenges Player B */
    @MessageMapping("/challenge")
    public void challenge(@Payload Map<String, String> msg) {
        String challenger = msg.get("challenger");
        String target = msg.get("target");
        Map<String, String> event = new HashMap<>();
        event.put("type", "CHALLENGE");
        event.put("challenger", challenger);
        messaging.convertAndSend("/topic/player-" + target, event);
    }

    /** Player B responds to challenge */
    @MessageMapping("/challenge-response")
    public void challengeResponse(@Payload Map<String, String> msg) {
        String challenger = msg.get("challenger");
        String target = msg.get("target");
        boolean accepted = Boolean.parseBoolean(msg.get("accepted"));

        if (accepted) {
            GameSession session = gameService.createSession(challenger, target);
            Map<String, String> event = new HashMap<>();
            event.put("type", "MATCH_ACCEPTED");
            event.put("sessionId", session.getSessionId());
            event.put("opponent", target);
            messaging.convertAndSend("/topic/player-" + challenger, event);

            Map<String, String> event2 = new HashMap<>();
            event2.put("type", "MATCH_ACCEPTED");
            event2.put("sessionId", session.getSessionId());
            event2.put("opponent", challenger);
            messaging.convertAndSend("/topic/player-" + target, event2);
        } else {
            Map<String, String> event = new HashMap<>();
            event.put("type", "MATCH_DECLINED");
            event.put("by", target);
            messaging.convertAndSend("/topic/player-" + challenger, event);
        }
    }

    /** Player selects their character */
    @MessageMapping("/select-character")
    public void selectCharacter(@Payload Map<String, String> msg) {
        String error = gameService.selectCharacter(msg.get("sessionId"), msg.get("playerName"), msg.get("characterId"));
        if (error != null) {
            Map<String, String> e = new HashMap<>();
            e.put("type", "ERROR"); e.put("message", error);
            messaging.convertAndSend("/topic/player-" + msg.get("playerName"), e);
        }
    }

    /** Player takes a combat action */
    @MessageMapping("/game-action")
    public void gameAction(@Payload Map<String, String> msg) {
        String error = gameService.processTurn(msg.get("sessionId"), msg.get("playerName"), msg.get("action"));
        if (error != null) {
            Map<String, String> e = new HashMap<>();
            e.put("type", "ERROR"); e.put("message", error);
            messaging.convertAndSend("/topic/player-" + msg.get("playerName"), e);
        }
    }

    /** Clean up on WebSocket disconnect */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String wsSessionId = event.getSessionId();
        if (wsSessionId != null) {
            // Remove the player from the lobby to completely erase their data on disconnect
            lobbyService.removePlayerBySession(wsSessionId);
        }
    }
}
