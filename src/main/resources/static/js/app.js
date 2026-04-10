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
    try { callback(JSON.parse(msg.body)); } catch(e) { callback(msg.body); }
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

// ===================== LOGOUT ON UNLOAD =====================
// ===================== LOGOUT ON UNLOAD =====================
function setupLogout() {
  // User requested to keep players registered until server shutdown
}
