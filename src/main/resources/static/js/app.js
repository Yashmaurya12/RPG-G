// ===================== SHARED APP STATE =====================
const App = {
  playerName: sessionStorage.getItem('playerName') || null,
  sessionId:  sessionStorage.getItem('sessionId')  || null,
  opponentName: sessionStorage.getItem('opponentName') || null,

  save() {
    if (this.playerName)   sessionStorage.setItem('playerName',   this.playerName);
    if (this.sessionId)    sessionStorage.setItem('sessionId',    this.sessionId);
    if (this.opponentName) sessionStorage.setItem('opponentName', this.opponentName);
  },

  clear() {
    sessionStorage.clear();
    this.playerName = null; this.sessionId = null; this.opponentName = null;
  },

  clearSessionData() {
    sessionStorage.removeItem('sessionId');
    sessionStorage.removeItem('opponentName');
    this.sessionId = null; this.opponentName = null;
  }
};

// ===================== WEBSOCKET CLIENT =====================
let stompClient = null;
let subscriptions = {};

function connectWS(onConnected) {
  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    reconnectDelay: 3000,
    onConnect: () => {
      console.log('WS connected');
      if (onConnected) onConnected(stompClient);
    },
    onDisconnect: () => console.log('WS disconnected'),
    onStompError: (frame) => console.error('STOMP error', frame),
  });
  stompClient.activate();
}

function subscribe(topic, callback) {
  if (!stompClient || !stompClient.connected) return;
  if (subscriptions[topic]) subscriptions[topic].unsubscribe();
  subscriptions[topic] = stompClient.subscribe(topic, msg => {
    let payload = msg.body;
    try { payload = JSON.parse(msg.body); } catch (e) {}
    callback(payload);
  });
}

function send(dest, payload) {
  if (!stompClient || !stompClient.connected) return;
  stompClient.publish({ destination: dest, body: JSON.stringify(payload) });
}

// ===================== REST HELPERS =====================
async function apiPost(url, body) {
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return res.json();
}

async function apiGet(url) {
  const res = await fetch(url);
  return res.json();
}

async function apiDelete(url, body) {
  const res = await fetch(url, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  return res.ok;
}

// ===================== MUSIC =====================
let bgMusic = null;
function initMusic() {
  bgMusic = document.getElementById('bg-music');
  if (!bgMusic) return;
  
  // Set default volume lower (0.15 instead of 0.3)
  const musicOn = localStorage.getItem('musicOn') !== 'false';
  const volume  = parseFloat(localStorage.getItem('musicVolume') || '0.15');
  bgMusic.volume = volume;

  // Support starting from a specific timestamp
  const startTime = parseFloat(bgMusic.dataset.start || '0');
  if (startTime > 0) {
    bgMusic.currentTime = startTime;
  }

  const tryPlay = () => {
    if (localStorage.getItem('musicOn') !== 'false' && bgMusic.paused) {
      bgMusic.play().catch(() => {});
    }
  };

  if (musicOn) bgMusic.play().catch(() => {});
  document.body.addEventListener('click', tryPlay, { once: true });
}
function setMusic(on) {
  if (!bgMusic) return;
  localStorage.setItem('musicOn', on);
  if (on) bgMusic.play().catch(() => {}); else bgMusic.pause();
}
function setVolume(v) {
  if (!bgMusic) return;
  bgMusic.volume = v;
  localStorage.setItem('musicVolume', v);
}

// ===================== UI EFFECTS (Custom WAV) =====================
// Uses the user-provided sci-fi click sound for all interactions
const SFX_FILE = '/mixkit-sci-fi-click-900.wav?v=3';

function playSFX(type) {
  const musicOn = localStorage.getItem('musicOn') !== 'false';
  const volume = parseFloat(localStorage.getItem('musicVolume') || '0.15');
  if (!musicOn || volume <= 0) return;

  try {
    const audio = new Audio(SFX_FILE);
    // Hover is slightly quieter than a click for better balance
    audio.volume = Math.min(1, type === 'hover' ? volume * 0.4 : volume * 1.0);
    audio.play().catch(e => {
        // Skip logging hover blocks to avoid console noise
        if (type !== 'hover') console.warn("[SFX] Play blocked:", e);
    });
  } catch (e) {
    console.warn("[SFX] Audio error:", e);
  }
}

// Global listeners for buttons
document.addEventListener('mouseover', e => {
  const btn = e.target.closest('.btn') || e.target.closest('.action-btn') || e.target.closest('.class-tab') || e.target.closest('.char-card');
  if (btn && !btn.contains(e.relatedTarget)) playSFX('hover');
});

document.addEventListener('click', e => {
  if (e.target.closest('.btn') || e.target.closest('.action-btn') || e.target.closest('.class-tab') || e.target.closest('.char-card')) {
    playSFX('click');
  }
});

// ===================== LOGOUT ON UNLOAD =====================
function setupLogout() {
  // Use sessionStorage to track session across page loads
}
