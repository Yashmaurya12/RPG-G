// Lobby page logic
let pendingChallenger = null;

document.addEventListener('DOMContentLoaded', () => {
  initMusic();
  setupLogout();

  const joinBtn    = document.getElementById('join-btn');
  const nameInput  = document.getElementById('name-input');
  const regError   = document.getElementById('reg-error');
  const logoutBtn  = document.getElementById('logout-btn');
  const toastAccept  = document.getElementById('toast-accept');
  const toastDecline = document.getElementById('toast-decline');

  // If already registered, restore session
  if (App.playerName) showLoggedIn(App.playerName);

  // ── Register ──────────────────────────────────────────────
  joinBtn.addEventListener('click', register);
  nameInput.addEventListener('keydown', e => { if (e.key === 'Enter') register(); });

  async function register() {
    const name = nameInput.value.trim();
    if (!name) { showError('Enter a name first.'); return; }
    joinBtn.disabled = true;
    const res = await apiPost('/api/players/register', { name });
    joinBtn.disabled = false;
    if (!res.success) { showError(res.error); return; }
    App.playerName = res.name;
    App.save();
    showLoggedIn(res.name);
    connectAndJoin(res.name);
  }

  function showError(msg) {
    const el = document.getElementById('reg-error');
    el.textContent = msg;
    setTimeout(() => { el.textContent = ''; }, 4000);
  }

  function showLoggedIn(name) {
    document.getElementById('register-section').style.display = 'none';
    const bar = document.getElementById('logged-in-bar');
    bar.style.display = 'flex';
    document.getElementById('logged-name').textContent = name;
    if (!stompClient || !stompClient.connected) connectAndJoin(name);
  }

  // ── Logout ────────────────────────────────────────────────
  logoutBtn.addEventListener('click', async () => {
    if (App.playerName) await apiPost('/api/players/logout', { name: App.playerName });
    App.clear();
    location.reload();
  });

  // ── WebSocket ─────────────────────────────────────────────
  function connectAndJoin(name) {
    connectWS(() => {
      // Announce join
      send('/app/join', { playerName: name });

      // Subscribe to lobby updates
      subscribe('/topic/lobby', renderPlayers);

      // Subscribe to private channel
      subscribe('/topic/player-' + name, handlePrivateEvent);
    });
  }

  // ── Render Players ────────────────────────────────────────
  function renderPlayers(players) {
    const grid  = document.getElementById('players-grid');
    const count = document.getElementById('player-count');
    count.textContent = players.length;

    grid.innerHTML = '';

    if (players.length === 0) {
      grid.innerHTML = '<div class="empty-lobby">No warriors online yet. Be the first!</div>';
      return;
    }

    players.forEach(p => {
      const isMe  = p.name === App.playerName;
      const avail = p.status === 'LOBBY' && !isMe;

      const card = document.createElement('div');
      card.className = 'player-card';
      card.innerHTML = `
        <div class="player-name">${escHtml(p.name)}${isMe ? ' <span style="color:var(--cyan);font-size:0.65rem">(YOU)</span>' : ''}</div>
        <div class="player-status status-${p.status}">${statusLabel(p.status)}</div>
        ${avail ? `<button class="btn btn-secondary btn-sm" id="challenge-${p.name}" onclick="sendChallenge('${escHtml(p.name)}')">⚔ Challenge</button>` : ''}
      `;
      grid.appendChild(card);
    });
  }

  function statusLabel(s) {
    return { LOBBY: '🟢 Online', IN_GAME: '🔴 In Battle', IN_CHARACTER_SELECT: '🟣 Choosing', CHALLENGED: '🔵 Challenged' }[s] || s;
  }

  // ── Challenge ─────────────────────────────────────────────
  window.sendChallenge = function(target) {
    if (!App.playerName) { alert('Please register first!'); return; }
    send('/app/challenge', { challenger: App.playerName, target });
    // Disable all challenge buttons briefly
    document.querySelectorAll('[id^="challenge-"]').forEach(b => b.disabled = true);
    setTimeout(() => document.querySelectorAll('[id^="challenge-"]').forEach(b => b.disabled = false), 5000);
  };

  // ── Private Events ────────────────────────────────────────
  function handlePrivateEvent(event) {
    if (event.type === 'CHALLENGE') {
      pendingChallenger = event.challenger;
      document.getElementById('toast-body').textContent = `${event.challenger} wants to battle you!`;
      document.getElementById('challenge-toast').classList.remove('hidden');
    } else if (event.type === 'MATCH_ACCEPTED') {
      App.sessionId    = event.sessionId;
      App.opponentName = event.opponent;
      App.save();
      document.getElementById('challenge-toast').classList.add('hidden');
      setTimeout(() => { window.location.href = '/character-select.html'; }, 500);
    } else if (event.type === 'MATCH_DECLINED') {
      showError(`${event.by} declined your challenge.`);
    }
  }

  toastAccept.addEventListener('click', () => {
    if (!pendingChallenger) return;
    send('/app/challenge-response', { challenger: pendingChallenger, target: App.playerName, accepted: 'true' });
    document.getElementById('challenge-toast').classList.add('hidden');
  });

  toastDecline.addEventListener('click', () => {
    if (!pendingChallenger) return;
    send('/app/challenge-response', { challenger: pendingChallenger, target: App.playerName, accepted: 'false' });
    document.getElementById('challenge-toast').classList.add('hidden');
    pendingChallenger = null;
  });

  // ── Utils ─────────────────────────────────────────────────
  function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // Load initial player list
  apiGet('/api/players/online').then(players => renderPlayers(players));
});
