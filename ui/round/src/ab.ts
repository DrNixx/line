import { MoveMetadata, RoundController, RoundData } from './interfaces';
import { Pubsub } from 'common/pubsub';

// Вспомогательные функции и переменные
var $t = [];
var ne = 8 + Math.round(Math.random() * 13);
var se = 6;
var St = !1;
var C = { hold: false, holdAcc: 0, ick: false };
var Pt = !1;
var j = 0;

type mouseMoveData = {
  buttonPressed: boolean;
  x: number;
  y: number;
};

var mouseMoveObserver = (() => {
  const mousemove = 'mousemove';
  let isListening = false;
  let mouseEvents: mouseMoveData[] = [];
  let i = 0;

  function handleMouseMove(ev: MouseEvent) {
    mouseEvents.push({
      buttonPressed: ev.buttons != 0,
      x: ev.clientX,
      y: ev.clientY,
    });

    if (mouseEvents.length > 4) {
      mouseEvents.shift();
      checkForHits();
    }
  }

  function calculateDistance(point1: mouseMoveData, point2: mouseMoveData) {
    return Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2);
  }

  function checkForHits() {
    let events = mouseEvents;
    if (
      !events[0].buttonPressed &&
      events[1].buttonPressed &&
      events[2].buttonPressed &&
      !events[3].buttonPressed &&
      calculateDistance(events[0], events[1]) > 900 &&
      calculateDistance(events[1], events[2]) === 0 &&
      calculateDistance(events[2], events[3]) === 0
    ) {
      i++;
    }
  }

  return {
    start() {
      if (!isListening) {
        isListening = true;
        document.addEventListener(mousemove, handleMouseMove);
      }
    },
    stop() {
      if (isListening) {
        isListening = false;
        document.removeEventListener(mousemove, handleMouseMove);
      }
    },
    hits: () => i,
  };
})();

var P: number[] = [];

// Функция ho
function handleMove(round: RoundController, meta: MoveMetadata) {
  if (meta.premove && round.ply > 1) {
    Pt = true;
  }

  if (Pt || !meta.holdTime || round.ply > 30) {
    mouseMoveObserver.stop();
    return;
  }

  P.push(meta.holdTime);
  if (P.length <= se) {
    return;
  }

  let isHold = false;
  P.shift();

  let variance: number = 0;
  let meanHoldTime = P.reduce((total, time) => total + time) / se;
  if (meanHoldTime > 2 && meanHoldTime < 140) {
    variance =
      P.map(time => Math.pow(time - meanHoldTime, 2)).reduce((total, time) => total + time) / (se - 1);
    isHold = variance < 256;
  }

  if (isHold || St) {
    $('.manipulable .cg-board').toggleClass('bh1', isHold && mouseMoveObserver.hits() > 2);
  }

  if (isHold) {
    if (!C.hold) {
      C.holdAcc++;
      if (C.holdAcc > 5) {
        round.socket.send('hold', { mean: Math.round(meanHoldTime), sd: Math.round(Math.sqrt(variance)) });
        C.hold = true;
      }
    }
    mouseMoveObserver.start();
    if (mouseMoveObserver.hits() > 2 && !C.ick) {
      round.socket.send('bye2');
      sendLog(round.data, 'ick2');
      C.ick = true;
    }
  } else {
    C.holdAcc = 0;
  }

  St = isHold;
}

// Функция M
function sendLog(data: RoundData, msg: string) {
  return fetchJSLog(data.game.id + data.player.id, msg);
}

// Функция sendJSLog
function fetchJSLog(msgId: string, msgStr: string) {
  return fetch('/jslog/' + msgId + '?n=' + msgStr, { method: 'post' });
}

function wsEmpty() {
  return !Object.keys(window.WebSocket).length;
}

// Функция bo
function bo() {
  let o = () => {
    let e = 'chess-master-autoclick',
      t = window.localStorage.getItem(e) !== null;
    return t && window.localStorage.removeItem(e), t;
  };

  // a[href*="chess.orgfree.com"]:not([href*="lichess.org"])
  return hasSig('YVtocmVmKj0iY2hlc3Mub3JnZnJlZS5jb20iXTpub3QoW2hyZWYqPSJsaWNoZXNzLm9yZyJdKQ==') || o()
    ? 'cma20'
    : ($('body>div[id]>div[class]>div[style]>span[id][style]').attr('style') || '').includes('color:blue')
      ? 'cma22'
      : !1;
}

// Функция ko
function ko() {
  // a[href="http://thapawngun.live"]
  if (hasSig('YVtocmVmPSJodHRwOi8vdGhhcGF3bmd1bi5saXZlIl0=')) {
    return 'lga1';
  } else if (
    hasSig('Lm1haW4tYm9hcmQgY2FudmFz') ||
    hasSig('aW1nW3NyYz0iaHR0cHM6Ly9saWNoZXNzLmdhLzEyOC5wbmciXQ==')
  ) {
    return 'lga3';
  } else if (hasSig('I2JhckxvZ29bc3R5bGVd')) {
    return 'lga4';
  } else {
    let lastChild = $('.main-board cg-board > *:last-child')[0];
    if (lastChild && lastChild.tagName.includes('-')) {
      return 'lga14';
    } else if (vo()) {
      return 'lga16';
    } else {
      return false;
    }
  }
}

// Функция To
function To() {
  if (hasSig('Ym9keSA+IGRpdiA+IGRpdi5nbWM=')) {
    // body > div > div.gmc
    return 'sqn1';
  } else if (hasSig('c3ZnW2RhdGEtaWNvbj0ibWludXMiXQ==')) {
    // svg[data-icon="minus"]
    return 'sqn2';
  } else if (hasSig('Ym9keT5zdmc+bWFya2Vy')) {
    // body>svg>marker
    return 'sqn3';
  } else {
    return false;
  }
}

// Функция So
function So() {
  // STOCKFISH
  const stkey: string = atob('U1RPQ0tGSVNI');
  return (window as { [key: string]: any })[stkey] ? 'kjf1' : false;
}

// Функция Po
function Po() {
  // #asdO
  return hasSig('I2FzZE8=') ? 'kbf1' : false;
}

// Функция xo
function xo() {
  // body>svg>polygon
  return hasSig('Ym9keT5zdmc+cG9seWdvbg==') ? 'aca1' : !1;
}

// Функция Do
function Do() {
  // #Kpawnu
  return hasSig('I0twYXdudQ==') ? 'kbg1' : !1;
}

// Функция Ro
function Ro() {
  return $t.length > 0 ? 'wpl1' : !1;
}

// Функция wo
function wo() {
  // #authBar #move_suggest_box
  return hasSig('I2F1dGhCYXI=') || hasSig('I21vdmVfc3VnZ2VzdF9ib3g=');
}

// Функция go
function go() {
  let o = Object.keys(window.WebSocket);
  return o[0] == 'prototype' && !o[1];
}

// Функция yo
function yo() {
  return !window.WebSocket.prototype.send.toString().includes('[native code]');
}

// Функция Mo
function Mo() {
  // cg-board>div[style]
  return hasSig('Y2ctYm9hcmQ+ZGl2W3N0eWxlXQ==') == 2;
}

// Функция vo
function vo() {
  if (j || !uo()) return !1;
  j = 1;
  try {
    document.querySelectorAll('.game__meta div').forEach(o => {
      o.attachShadow({ mode: 'open' }).innerHTML = '<slot>';
    });
  } catch (o) {
    j = 2;
  }
  return j == 2;
}

// Функция uo
function uo() {
  return navigator.userAgent.includes('Chrome/');
}

// Функция g
function hasSig(o: string): number {
  return $(atob(o)).length;
}

export function init(round: RoundController): void {
  window.setTimeout(() => {
    wsEmpty() && sendLog(round.data, 'ih1');
  }, 1000);
}

export function move(round: RoundController, meta: MoveMetadata, emit: Pubsub['emit']): void {
  handleMove(round, meta);
  if (round.ply <= ne + 2 && round.ply > ne) {
    try {
      if (round.opts.userId || Math.random() < 0.2) {
        let sendDelayedLog = (msg: string, weight: number) => {
          if (Math.random() < weight) {
            window.setTimeout(() => {
              if (Math.random() < 0.5) {
                sendLog(round.data, msg);
              } else {
                emit('ab.rep', msg);
              }
            }, ne * 1500);
          }
        };

        let i: string | false;
        if ((i = bo()) || (i = ko())) {
          sendDelayedLog(i, 0.5);
        } else if ((i = To())) {
          sendDelayedLog(i, 0.2);
        } else if ((i = So())) {
          sendDelayedLog(i, 0.3);
        } else if ((i = Po()) || (i = xo())) {
          sendDelayedLog(i, 0.5);
        } else if ((i = Do()) || (i = Ro())) {
          sendDelayedLog(i, 0.3);
        }
      }
      if (wo()) {
        sendLog(round.data, 'los');
      }
      if (round.opts.userId && go()) {
        sendLog(round.data, 'wst1');
      }
      if (yo()) {
        sendLog(round.data, 'wst2');
      }
      if (Mo()) {
        sendLog(round.data, 'wrp');
      }
    } catch (error) {
      console.error(error);
      sendLog(round.data, 'err ' + ('' + error).toString().slice(0, 120));
    }
  }
}
