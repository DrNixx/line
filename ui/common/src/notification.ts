import { storage } from './storage';

let notifications: Array<Notification> = [];
let listening = false;

function listenToFocus() {
  if (!listening) {
    listening = true;
    window.addEventListener('focus', () => {
      notifications.forEach(n => n.close());
      notifications = [];
    });
  }
}

function notify(msg: string | (() => string)) {
  const store = storage.make('just-notified');
  if (document.hasFocus() || Date.now() - parseInt(store.get()!, 10) < 1000) return;
  store.set('' + Date.now());
  if ($.isFunction(msg)) msg = msg();
  const notification = new Notification('live.chess-online.com', {
    icon: site.asset.url('logo/chess-favicon-256.png'),
    body: msg,
  });
  notification.onclick = () => window.focus();
  notifications.push(notification);
  listenToFocus();
}

export default function (msg: string | (() => string)): void {
  if (document.hasFocus() || !('Notification' in window)) return;
  if (Notification.permission === 'granted') {
    // increase chances that the first tab can put a local storage lock
    setTimeout(notify, 10 + Math.random() * 500, msg);
  }
}
