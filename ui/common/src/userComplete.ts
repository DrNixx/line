import { complete } from './complete';
import * as xhr from './xhr';
import debounce from 'debounce-promise';

export interface UserCompleteResult {
  result: LightUserOnline[];
}

export interface UserCompleteOpts {
  input: HTMLInputElement;
  tag?: 'a' | 'span';
  minLength?: number;
  populate?: (result: LightUserOnline) => string;
  onSelect?: (result: LightUserOnline) => void;
  focus?: boolean;
  friend?: boolean;
  tour?: string;
  swiss?: string;
  team?: string;
}

export function userComplete(opts: UserCompleteOpts): void {
  const debounced = debounce(
    (term: string) =>
      xhr
        .json(
          xhr.url('/api/player/autocomplete', {
            term,
            friend: opts.friend ? 1 : 0,
            tour: opts.tour,
            swiss: opts.swiss,
            team: opts.team,
            object: 1,
          }),
        )
        .then((r: UserCompleteResult) => ({ term, ...r })),
    150,
  );

  complete<LightUserOnline>({
    input: opts.input,
    fetch: t =>
      debounced(t).then(({ term, result }) => (t === term ? result : Promise.reject('Debounced ' + t))),
    render(o: LightUserOnline) {
      const tag = opts.tag || 'a';
      return (
        '<' +
        tag +
        ' class="complete-result ulpt user-link' +
        (o.online ? ' online' : '') +
        '" ' +
        (tag === 'a' ? '' : 'data-') +
        'href="/@/' +
        o.id +
        '">' +
        '<i class="line' +
        (o.patron ? ' patron' : '') +
        '"></i>' +
        (o.title
          ? '<span class="utitle"' +
            (o.title === 'BOT' ? ' data-bot="data-bot" ' : '') +
            '>' +
            o.title +
            '</span>&nbsp;'
          : '') +
        o.name +
        (o.flair ? '<img class="uflair" src="' + site.asset.flairSrc(o.flair) + '"/>' : '') +
        '</' +
        tag +
        '>'
      );
    },
    populate: opts.populate || (r => r.name),
    onSelect: opts.onSelect,
    regex: /^([a-z][\w-]|[\d]){2,29}$/i,
  });
  if (opts.focus) setTimeout(() => opts.input.focus());
}
