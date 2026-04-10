package com.multiverse.rpg.controller;

import com.multiverse.rpg.model.Player;
import com.multiverse.rpg.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired private LobbyService lobbyService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String sessionId = body.getOrDefault("sessionId", "web-" + UUID.randomUUID());
        String error = lobbyService.registerPlayer(name, sessionId);
        Map<String, Object> resp = new HashMap<>();
        if (error != null) {
            resp.put("success", false);
            resp.put("error", error);
            return ResponseEntity.badRequest().body(resp);
        }
        resp.put("success", true);
        resp.put("name", name.trim());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/online")
    public ResponseEntity<List<Map<String, String>>> online() {
        List<Map<String, String>> list = new ArrayList<>();
        for (Player p : lobbyService.getOnlinePlayers()) {
            Map<String, String> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("status", p.getStatus().name());
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        lobbyService.removePlayer(body.get("name"));
        return ResponseEntity.ok().build();
    }
}
