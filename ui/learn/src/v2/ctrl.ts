import { LearnProgress, SnabbdomLearnOpts } from '../learn';
import { Stage } from '../stage/list';
import * as stages from '../stage/list';
import * as scoring from '../score';
import { SnabbdomSideCtrl } from './sideCtrl';
import { clearTimeouts } from '../timeouts';

export class LearnCtrl {
  data: LearnProgress = this.opts.storage.data;
  trans: Trans = site.trans(this.opts.i18n);

  sideCtrl: SnabbdomSideCtrl;

  constructor(
    readonly opts: SnabbdomLearnOpts,
    readonly redraw: () => void,
  ) {
    clearTimeouts();

    this.sideCtrl = new SnabbdomSideCtrl(this, opts);
  }

  isStageIdComplete = (stageId: number) => {
    const stage = stages.byId[stageId];
    if (!stage) return true;
    const result = this.data.stages[stage.key];
    if (!result) return false;
    return result.scores.filter(scoring.gtz).length >= stage.levels.length;
  };

  stageProgress = (stage: Stage) => {
    const result = this.data.stages[stage.key];
    const complete = result ? result.scores.filter(scoring.gtz).length : 0;
    return [complete, stage.levels.length];
  };
}
