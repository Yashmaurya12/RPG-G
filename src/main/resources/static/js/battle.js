// Battle page logic
let myName, sessionId, gameState;

document.addEventListener('DOMContentLoaded', () => {
  initMusic();

  myName    = App.playerName;
  sessionId = App.sessionId;

  if (!myName || !sessionId) { window.location.href = '/lobby.html'; return; }

  connectWS(() => {
    send('/app/join', { playerName: myName });
    subscribe('/topic/game/' + sessionId, handleState);

    // Fetch current state via REST to avoid race condition
    // (the state may have been broadcast before we subscribed)
    apiGet('/api/game/' + sessionId).then(state => {
      if (state && state.sessionId) handleState(state);
    }).catch(() => {});
  });
});

// ── Handle Game State ────────────────────────────────────────
function handleState(state) {
  gameState = state;
  const p1 = state.player1;
  const p2 = state.player2;

  // Determine which panel is "me"
  const myIsP1 = myName === p1.name;
  const me  = myIsP1 ? p1 : p2;
  const opp = myIsP1 ? p2 : p1;

  // Update panels
  updateFighter('local', me);
  updateFighter('opponent', opp);

  // Turn indicator
  const myTurn = state.currentTurn === myName;
  document.getElementById('local-turn-ind').classList.toggle('hidden', !myTurn);

  const banner = document.getElementById('turn-banner');
  if (state.state === 'BATTLE') {
    banner.textContent = myTurn ? '🟢 YOUR TURN — Choose an action' : `⏳ Waiting for ${state.currentTurn}…`;
    banner.style.color = myTurn ? 'var(--cyan)' : 'var(--text-dim)';
    banner.style.borderColor = myTurn ? 'var(--cyan)' : 'var(--border)';
  }

  // Enable / disable action buttons
  const canAct = myTurn && state.state === 'BATTLE';
  setActionsEnabled(canAct, me);

  // Combat log
  renderLog(state.combatLog || []);

  // Check win/loss
  if (state.state === 'FINISHED') showVictory(state.winner);
}

function updateFighter(prefix, player) {
  const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
  const setHtml = (id, val) => { const el = document.getElementById(id); if (el) el.innerHTML = val; };

  set(`${prefix}-name`, player.name);
  set(`${prefix}-emoji`, player.emoji || '⚔️');
  set(`${prefix}-char`, player.characterName || '');

  // HP bar
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

  // Defending badge
  const badge = document.getElementById(`${prefix}-defend-badge`);
  if (badge) badge.textContent = player.defending ? '🛡 Defending' : '';

  // Fighter card gradient / border
  const card = document.getElementById(`${prefix}-card`);
  if (card && player.color1) {
    card.style.borderColor = player.color1;
    card.style.background = `linear-gradient(135deg, rgba(5, 10, 30, 0.95), ${player.color1}22)`;
  }
}

// ── Action Buttons ───────────────────────────────────────────
function setActionsEnabled(enabled, me) {
  const btnAtk  = document.getElementById('btn-attack');
  const btnSpec = document.getElementById('btn-special');
  const btnDef  = document.getElementById('btn-defend');
  const btnRun  = document.getElementById('btn-run');
  const cdText  = document.getElementById('special-cd-text');
  const spLabel = document.getElementById('special-label');

  btnAtk.disabled = !enabled;
  btnDef.disabled = !enabled;
  btnRun.disabled = false; // Always enabled unless game finishes

  const cd = me ? me.specialCooldown : 0;
  const onCooldown = cd > 0;
  btnSpec.disabled = !enabled || onCooldown;
  btnSpec.classList.toggle('on-cooldown', onCooldown);
  cdText.textContent = onCooldown ? `(${cd} turn${cd > 1 ? 's' : ''})` : '';
  if (me && me.specialName) spLabel.textContent = me.specialName;
}

window.doAction = function(action) {
  if (!gameState || gameState.state !== 'BATTLE') return;
  if (action !== 'FORFEIT' && gameState.currentTurn !== myName) return;

  // Disable buttons immediately
  ['btn-attack','btn-special','btn-defend','btn-run'].forEach(id => {
    const el = document.getElementById(id);
    if(el) el.disabled = true;
  });

  send('/app/game-action', { sessionId, playerName: myName, action });
};

// ── Combat Log ────────────────────────────────────────────────
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

// ── Victory ───────────────────────────────────────────────────
function showVictory(winner) {
  const isWinner = winner === myName;
  const overlay = document.getElementById('victory-overlay');
  document.getElementById('victory-emoji').textContent = isWinner ? '🏆' : '💀';
  document.getElementById('victory-title').textContent = isWinner ? 'VICTORY!' : 'DEFEATED!';
  document.getElementById('victory-sub').textContent = isWinner
    ? 'You crushed your opponent in battle!'
    : `${winner} has vanquished you this time.`;
  overlay.classList.remove('hidden');
  App.clearSessionData();
}
