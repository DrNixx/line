import { h } from 'snabbdom';
import { bind, type MaybeVNodes } from 'common/snabbdom';
import type LobbyController from '../ctrl';
import type { Tab } from '../interfaces';

export const realTimeTabLabel = (label: string, nb: number): string => `${label} (${nb > 100 ? '99+' : nb})`;

function tab(ctrl: LobbyController, key: Tab, active: Tab, content: MaybeVNodes) {
  return h(
    'span',
    {
      attrs: { role: 'tab' },
      class: { active: key === active, glowing: key !== active && key === 'pools' && !!ctrl.poolMember },
      hook: bind('mousedown', _ => ctrl.setTab(key), ctrl.redraw),
    },
    content,
  );
}

function linkTab(key: Tab, active: Tab, href: string, content: MaybeVNodes) {
  return h(
    'a.tab-link',
    {
      attrs: { href, role: 'tab' },
      class: { active: key === active },
    },
    content,
  );
}

export default function (ctrl: LobbyController) {
  const nbPlaying = ctrl.data.nbNowPlaying,
    myTurnPovsNb = ctrl.data.nowPlaying.filter(p => p.isMyTurn).length,
    active = ctrl.tab,
    isBot = ctrl.me?.isBot,
    realTimeLabel =
      ctrl.mergeMode && ctrl.tab !== 'real_time' && ctrl.realTimeCount !== undefined
        ? realTimeTabLabel(i18n.site.lobby, ctrl.realTimeCount)
        : i18n.site.lobby;
  return [
    isBot ? undefined : tab(ctrl, 'pools', active, [i18n.site.quickPairing]),
    isBot ? undefined : tab(ctrl, 'real_time', active, [realTimeLabel]),
    isBot ? undefined : linkTab('seeks', active, 'https://www.chess-online.com/', [i18n.site.correspondence]),
    active === 'now_playing' || nbPlaying || isBot
      ? tab(ctrl, 'now_playing', active, [
          ...i18n.site.nbGamesInPlay.asArray(nbPlaying, nbPlaying >= 100 ? '100+' : nbPlaying.toString()),
          myTurnPovsNb > 0 ? h('i.unread', myTurnPovsNb >= 9 ? '9+' : myTurnPovsNb) : null,
        ])
      : null,
  ];
}
