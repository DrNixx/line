const searchParams = new URL(self.location.href).searchParams;
const assetBase = new URL(searchParams.get('asset-url')!, self.location.href).href;
const rightOrigin = location.protocol + '//live.chess-online.com';

function assetUrl(path: string): string {
  return `${assetBase}assets/${path}`;
}

self.addEventListener('push', event => {
  if (event.target && (event.target as ServiceWorkerGlobalScope).origin != rightOrigin) {
    return;
  }
  
  const data = event.data!.json();
  return event.waitUntil(self.registration.showNotification(data.title, {
    badge: assetUrl('logo/chess-mono-128.png'),
    icon: assetUrl('logo/chess-favicon-192.png'),
    body: data.body,
    tag: data.tag,
    data: data.payload,
    requireInteraction: true,
  }));
});

async function handleNotificationClick(event: NotificationEvent) {
  const notifications = await self.registration.getNotifications();
  notifications.forEach(notification => notification.close());

  const windowClients = await self.clients.matchAll({
    type: 'window',
    includeUncontrolled: true,
  }) as ReadonlyArray<WindowClient>;

  // determine url
  const data = event.notification.data.userData;
  let url = '/';
  if (data.fullId) url = '/' + data.fullId;
  else if (data.threadId) url = '/inbox/' + data.threadId;
  else if (data.challengeId) url = '/' + data.challengeId;

  // focus open window with same url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if (clientUrl.pathname === url && 'focus' in client) return await client.focus();
  }

  // navigate from open homepage to url
  for (const client of windowClients) {
    const clientUrl = new URL(client.url, self.location.href);
    if ((clientUrl.pathname === '/ru-ru') || 
      (clientUrl.pathname === '/en-us') ||
      clientUrl.pathname === '/es-es') {
        return await client.navigate(url);
      }
  }

  // open new window
  return await self.clients.openWindow(url);
}

self.addEventListener('notificationclick', e => e.waitUntil(handleNotificationClick(e)));
