package com.multiverse.rpg.controller;

import com.multiverse.rpg.model.GameSession;
import com.multiverse.rpg.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired private GameService gameService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getGameState(@PathVariable String sessionId) {
        GameSession s = gameService.getSession(sessionId);
        if (s == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(gameService.getStateMap(s));
    }
}
