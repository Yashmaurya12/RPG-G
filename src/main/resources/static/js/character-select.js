// Character Select logic
let allCharacters = {};      // { MAGE: [...], WARRIOR: [...], ... }
let selectedCharId = null;
let currentClass = 'MAGE';

document.addEventListener('DOMContentLoaded', async () => {
  initMusic();

  if (!App.playerName || !App.sessionId) {
    window.location.href = '/lobby.html'; return;
  }

  document.getElementById('opponent-label').textContent = App.opponentName || '???';

  // Load characters from server
  allCharacters = await apiGet('/api/characters');
  renderClass(currentClass);

  // Connect WebSocket
  connectWS(() => {
    send('/app/join', { playerName: App.playerName });
    subscribe('/topic/game/' + App.sessionId, handleGameState);
    subscribe('/topic/player-' + App.playerName, handlePrivate);

    // Fetch current state in case we missed the broadcast
    apiGet('/api/game/' + App.sessionId).then(state => {
      if (state && state.sessionId) handleGameState(state);
    }).catch(() => {});
  });

  // Ready button
  document.getElementById('ready-btn').addEventListener('click', () => {
    if (!selectedCharId) return;
    send('/app/select-character', {
      sessionId: App.sessionId,
      playerName: App.playerName,
      characterId: selectedCharId
    });
    document.getElementById('ready-btn').disabled = true;
    document.getElementById('ready-btn').textContent = '⏳ Waiting for opponent…';
  });
});

// ── Class Tab Switch ─────────────────────────────────────────
window.switchClass = function(cls) {
  currentClass = cls;
  document.querySelectorAll('.class-tab').forEach(t => t.classList.remove('active'));
  document.getElementById('tab-' + cls).classList.add('active');
  renderClass(cls);
};

function renderClass(cls) {
  const chars = allCharacters[cls] || [];
  const grid = document.getElementById('chars-grid');
  grid.innerHTML = '';
  chars.forEach(c => grid.appendChild(buildCharCard(c)));
}

function buildCharCard(c) {
  const div = document.createElement('div');
  div.className = 'char-card' + (selectedCharId === c.id ? ' selected' : '');
  div.id = 'char-' + c.id;
  div.style.background = `linear-gradient(135deg, ${c.color1}22, ${c.color2}11)`;
  div.style.borderColor = selectedCharId === c.id ? c.color1 : 'transparent';

  const hpPct  = (c.maxHp  / 150 * 100).toFixed(0);
  const atkPct = (c.attack / 40  * 100).toFixed(0);
  const defPct = (c.defense / 20 * 100).toFixed(0);

  div.innerHTML = `
    <div class="char-card-selected-badge">✓ SELECTED</div>
    <div class="char-card-inner">
      <div class="char-img-container">
        <img src="/img/characters/${c.id}.png" class="char-img" alt="${c.name}">
      </div>
      <div class="char-name">${c.name}</div>
      <div class="char-class-badge">${c.characterClass}</div>
      <div class="stat-bar">
        <div class="stat-label"><span>HP</span><span>${c.maxHp}</span></div>
        <div class="stat-track"><div class="stat-fill" style="width:${hpPct}%;background:linear-gradient(90deg,#44ff88,#00cc66)"></div></div>
      </div>
      <div class="stat-bar">
        <div class="stat-label"><span>ATK</span><span>${c.attack}</span></div>
        <div class="stat-track"><div class="stat-fill" style="width:${atkPct}%;background:linear-gradient(90deg,${c.color1},${c.color2})"></div></div>
      </div>
      <div class="stat-bar">
        <div class="stat-label"><span>DEF</span><span>${c.defense}</span></div>
        <div class="stat-track"><div class="stat-fill" style="width:${defPct}%;background:linear-gradient(90deg,#00a8ff,#0044ff)"></div></div>
      </div>
      <div class="special-box">
        <span class="special-name">⚡ ${c.specialName}</span>
        ${c.specialDescription} (${c.specialCooldown}-turn CD)
      </div>
    </div>
  `;

  div.addEventListener('click', () => selectChar(c.id, c.color1));
  return div;
}

function selectChar(id, color) {
  selectedCharId = id;
  // Re-render to update selection state
  renderClass(currentClass);
  const btn = document.getElementById('ready-btn');
  btn.disabled = false;
  btn.textContent = '✅ READY — Lock In!';
  btn.style.background = `linear-gradient(135deg, ${color}, var(--violet))`;
}

// ── Game State Update ────────────────────────────────────────
function handleGameState(state) {
  updateSelectionStatus(state);
  if (state.state === 'BATTLE') {
    window.location.href = '/battle.html';
  }
}

function updateSelectionStatus(state) {
  const p1 = state.player1; const p2 = state.player2;
  const myIsP1 = App.playerName === p1.name;

  document.getElementById('p1-name-label').textContent = p1.name;
  document.getElementById('p2-name-label').textContent = p2.name;

  const p1Dot = document.getElementById('p1-dot');
  const p2Dot = document.getElementById('p2-dot');
  const p1Lbl = document.getElementById('p1-sel-label');
  const p2Lbl = document.getElementById('p2-sel-label');

  if (p1.selected) { p1Dot.classList.add('ready'); p1Lbl.textContent = '✓ ' + (p1.characterName || 'Ready'); }
  else { p1Lbl.textContent = 'Choosing…'; }
  if (p2.selected) { p2Dot.classList.add('ready'); p2Lbl.textContent = '✓ ' + (p2.characterName || 'Ready'); }
  else { p2Lbl.textContent = 'Choosing…'; }
}

function handlePrivate(event) {
  if (event.type === 'ERROR') {
    alert('Error: ' + event.message);
    document.getElementById('ready-btn').disabled = false;
    document.getElementById('ready-btn').textContent = '✅ READY — Lock In!';
  }
}
