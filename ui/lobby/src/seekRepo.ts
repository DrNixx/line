import LobbyController from './ctrl';

function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

export function sort(ctrl: LobbyController) {
  ctrl.data.seeks.sort(order);
}

export function initAll(ctrl: LobbyController) {
  ctrl.data.seeks.forEach(function(seek) {
    seek.action = (ctrl.data.me && seek.id === ctrl.data.me.id) ? 'cancelSeek' : 'joinSeek';
  });
  sort(ctrl);
}

export function find(ctrl: LobbyController, id: string) {
  return ctrl.data.seeks.find(s => s.id === id);
}
