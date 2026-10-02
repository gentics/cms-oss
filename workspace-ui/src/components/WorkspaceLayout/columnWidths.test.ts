import { describe, expect, it } from 'vitest';

import { clampColumnWidths } from './columnWidths';

// 1200 px grid, two 9 px splitters, center keeps 320 px: 862 px for both side columns.
const base = {
    total: 1200,
    isLeftVisible: true,
    requested: { left: null, right: null },
    rendered: { left: 292, right: 552 },
};

describe('clampColumnWidths', () => {
    it('keeps the CSS defaults while nothing was requested', () => {
        expect(clampColumnWidths(base)).toEqual({ left: null, right: null, maxLeft: 862 - 552, maxRight: 862 - 292 });
    });

    it('passes a width through that fits', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 250, right: 500 } })).toMatchObject({ left: 250, right: 500 });
    });

    it('rounds to whole pixels', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 250.6, right: null } }).left).toBe(251);
    });

    it('keeps the minimum width of each side column', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 50, right: 50 } })).toMatchObject({ left: 210, right: 320 });
    });

    it('leaves the center column its minimum, counting the other column with its rendered width', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 800, right: null } }).left).toBe(862 - 552);
        expect(clampColumnWidths({ ...base, requested: { left: null, right: 900 } }).right).toBe(862 - 292);
    });

    it('counts the other column with its requested width once there is one', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 800, right: 400 } }).left).toBe(862 - 400);
    });

    it('never goes below the minimum, even when the grid is too narrow', () => {
        expect(clampColumnWidths({ ...base, total: 700, requested: { left: 400, right: 400 } }))
            .toEqual({ left: 210, right: 320, maxLeft: 210, maxRight: 320 });
    });

    it('gives the right column the room of the hidden left column and its splitter', () => {
        expect(clampColumnWidths({ ...base, isLeftVisible: false, requested: { left: 250, right: 1000 } }))
            .toMatchObject({ right: 1200 - 9 - 320, maxRight: 1200 - 9 - 320 });
    });
});
