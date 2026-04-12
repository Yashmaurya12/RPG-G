// Battle page logic
let myName, opponentName, sessionId, gameState;

document.addEventListener('DOMContentLoaded', () => {
  initMusic();

  myName       = App.playerName;
  opponentName = App.opponentName;
  sessionId    = App.sessionId;

  if (!myName || !sessionId) { window.location.href = '/lobby.html'; return; }

  connectWS(() => {
    send('/app/join', { playerName: myName });
    subscribe('/topic/game/' + sessionId, handleState);

    apiGet('/api/game/' + sessionId).then(state => {
      if (state && state.sessionId) handleState(state);
    }).catch(() => {});
  });
});

function handleState(state) {
  console.log("RECEIVED STATE:", state);
  gameState = state;
  const p1 = state.player1;
  const p2 = state.player2;

  if (!p1 || !p2) {
    console.error("CRITICAL STATE ERROR: p1 or p2 missing!", state);
    return;
  }

  const myIsP1 = myName === p1.name;
  const me  = myIsP1 ? p1 : p2;
  const opp = myIsP1 ? p2 : p1;

  updateFighter('local', me);
  updateFighter('opponent', opp);

  // Reality Banner
  const rBanner = document.getElementById('reality-banner');
  if (rBanner && state.currentReality) {
    rBanner.textContent = `🌌 REALITY: ${state.currentReality.replace('_', ' ')} (${state.realityTurnsLeft} turns left)`;
    rBanner.className = 'reality-banner ' + state.currentReality;
  }

  // Loot Rift
  const rift = document.getElementById('rift-loot');
  if (rift) {
    if (state.activeLoot) {
      rift.classList.add('visible');
      document.getElementById('loot-emoji').textContent = state.activeLoot.emoji;
      document.getElementById('loot-name').textContent = state.activeLoot.name;
    } else {
      rift.classList.remove('visible');
    }
  }

  // Turn indicator
  const myTurn = state.currentTurn === myName;
  document.getElementById('local-turn-ind').classList.toggle('hidden', !myTurn);

  const banner = document.getElementById('turn-banner');
  if (state.state === 'BATTLE') {
    banner.textContent = myTurn ? '🟢 YOUR TURN — Choose an action' : `⏳ Waiting for ${state.currentTurn}…`;
    banner.style.color = myTurn ? 'var(--cyan)' : 'var(--text-dim)';
    banner.style.borderColor = myTurn ? 'var(--cyan)' : 'var(--border)';
  }

  // Actions
  const canAct = myTurn && state.state === 'BATTLE';
  setActionsEnabled(canAct, me);

  renderLog(state.combatLog || []);
  
  // Animation Detection
  const lastLog = state.combatLog ? state.combatLog[state.combatLog.length - 1] : '';
  if (lastLog) {
    const isMe = lastLog.toLowerCase().includes(myName.toLowerCase());
    const isOpponent = opponentName && lastLog.toLowerCase().includes(opponentName.toLowerCase());
    
    if (lastLog.includes('attacks for') || lastLog.includes('unleashes')) {
      if (isMe) runAnimation('local', 'anim-attack-p1');
      else if (isOpponent) runAnimation('opponent', 'anim-attack-p2');
      
      // Hit reaction on the OTHER player
      setTimeout(() => {
        if (isMe) runAnimation('opponent', 'anim-hit');
        else if (isOpponent) runAnimation('local', 'anim-hit');
      }, 300);
    } else if (lastLog.includes('Special Move:')) {
      if (isMe) runAnimation('local', 'anim-special');
      else if (isOpponent) runAnimation('opponent', 'anim-special');
    } else if (lastLog.includes('scavenged')) {
      if (isMe) runAnimation('local', 'anim-grab');
      else if (isOpponent) runAnimation('opponent', 'anim-grab');
    }
  }

  if (state.state === 'FINISHED') showVictory(state.winner);
}

function updateFighter(prefix, player) {
  const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
  set(`${prefix}-name`, player.name);
  set(`${prefix}-char`, player.characterName || '');

  // Sprite Rendering
  const img = document.getElementById(`${prefix}-img`);
  if (img && player.characterId) {
    const isLocal = (prefix === 'local');
    img.src = `/img/characters/${player.characterId}${isLocal ? '_back' : ''}.png`;
    img.style.display = 'block';
    
    // Position/Scale is now handled by CSS .player-sprite-pos and .enemy-sprite-pos
    img.style.transform = ''; 
  }

  const hp = Math.max(0, player.hp || 0);
  const maxHp = player.maxHp || 100;
  const pct = Math.round((hp / maxHp) * 100);
  const bar = document.getElementById(`${prefix}-hp-bar`);
  const hpText = document.getElementById(`${prefix}-hp-text`);
  if (bar) {
    bar.style.width = pct + '%';
    bar.className = 'hp-fill ' + (pct > 60 ? 'hp-high' : pct > 30 ? 'hp-med' : 'hp-low');
  }
  if (hpText) hpText.textContent = `${hp} / ${maxHp}`;

  const badge = document.getElementById(`${prefix}-defend-badge`);
  const container = document.getElementById(`${prefix}-container`);
  if (badge) badge.textContent = player.defending ? '🛡 Defending' : '';
  
  // Toggle defense animation
  if (container) {
    if (player.defending) container.classList.add('anim-defend');
    else container.classList.remove('anim-defend');

    // Idle Animation Assignment
    container.classList.remove('model-idle-breath', 'model-idle-float');
    if (player.characterClass === 'CYBORG') {
      container.classList.add('model-idle-float');
    } else {
      container.classList.add('model-idle-breath');
    }
  }
  
  const card = document.getElementById(`${prefix}-card`);
  if (card && player.color1) {
    card.style.borderColor = player.color1;
    card.style.background = `linear-gradient(135deg, rgba(5, 10, 30, 0.95), ${player.color1}22)`;
  }
}

function runAnimation(prefix, animClass) {
  const el = document.getElementById(`${prefix}-container`);
  if (!el) return;
  el.classList.remove('anim-attack-p1', 'anim-attack-p2', 'anim-hit', 'anim-special', 'anim-grab');
  void el.offsetWidth; // Trigger reflow
  el.classList.add(animClass);
}

function setActionsEnabled(enabled, me) {
  const btnAtk  = document.getElementById('btn-attack');
  const btnSpec = document.getElementById('btn-special');
  const btnDef  = document.getElementById('btn-defend');
  const btnRun  = document.getElementById('btn-run');
  const btnGrab = document.getElementById('btn-grab');
  const cdText  = document.getElementById('special-cd-text');
  const spLabel = document.getElementById('special-label');
  const grabLbl = document.getElementById('grab-label');

  btnAtk.disabled = !enabled;
  btnDef.disabled = !enabled;
  btnRun.disabled = false;

  const cd = me ? me.specialCooldown : 0;
  const onCooldown = cd > 0;
  btnSpec.disabled = !enabled || onCooldown;
  if (me && me.specialName) spLabel.textContent = me.specialName;
  cdText.textContent = onCooldown ? `(${cd} turn${cd > 1 ? 's' : ''})` : '';

  // Loot Action Button
  if (me && me.heldLoot) {
    btnGrab.classList.remove('hidden');
    btnGrab.disabled = !enabled;
    grabLbl.textContent = `USE ${me.heldLoot.name.toUpperCase()}`;
    btnGrab.onclick = () => doAction('USE_LOOT');
  } else if (gameState && gameState.activeLoot) {
    btnGrab.classList.remove('hidden');
    btnGrab.disabled = !enabled;
    grabLbl.textContent = 'GRAB LOOT';
    btnGrab.onclick = () => doAction('GRAB_LOOT');
  } else {
    btnGrab.classList.add('hidden');
  }
}

window.doAction = function(action) {
  if (!gameState || gameState.state !== 'BATTLE') return;
  if (action !== 'FORFEIT' && gameState.currentTurn !== myName) return;

  ['btn-attack','btn-special','btn-defend','btn-run','btn-grab'].forEach(id => {
    const el = document.getElementById(id);
    if(el) el.disabled = true;
  });

  send('/app/game-action', { sessionId, playerName: myName, action });
};

function renderLog(lines) {
  const log = document.getElementById('combat-log');
  log.innerHTML = '';
  lines.forEach(line => {
    const div = document.createElement('div');
    div.className = 'log-line';
    div.textContent = line;
    log.appendChild(div);
  });
  log.scrollTop = log.scrollHeight;
}

function showVictory(winner) {
  const isWinner = winner === myName;
  const overlay = document.getElementById('victory-overlay');
  overlay.classList.remove('hidden');
  document.getElementById('victory-emoji').textContent = isWinner ? '🏆' : '💀';
  document.getElementById('victory-title').textContent = isWinner ? 'VICTORY!' : 'DEFEATED!';
  document.getElementById('victory-sub').textContent = isWinner
    ? 'You crushed your opponent in battle!'
    : `${winner} has vanquished you this time.`;

  // Clear data and handle navigation
  const resetHandler = async (e) => {
    e.preventDefault();
    const targetHref = e.currentTarget.href;
    
    if (myName) {
      try {
        await fetch('/api/players/logout', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({ name: myName })
        });
      } catch (err) { console.error(err); }
    }
    App.clear();
    window.location.href = targetHref || '/lobby.html';
  };
  
  document.getElementById('btn-play-again').onclick = resetHandler;
  document.getElementById('btn-home').onclick = resetHandler;
}
