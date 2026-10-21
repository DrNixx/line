import { describe, expect, it } from 'vitest';
import { realTimeTabLabel } from '../src/view/tabs';

describe('realTimeTabLabel', () => {
  it('shows the current count', () => {
    expect(realTimeTabLabel('Lobby', 27)).toBe('Lobby (27)');
  });

  it('shows 99+ above 100', () => {
    expect(realTimeTabLabel('Lobby', 101)).toBe('Lobby (99+)');
  });

  it('keeps 100 as is', () => {
    expect(realTimeTabLabel('Lobby', 100)).toBe('Lobby (100)');
  });
});
