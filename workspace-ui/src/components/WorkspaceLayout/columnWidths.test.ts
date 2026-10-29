import { describe, expect, it } from 'vitest';

import { clampColumnWidths } from './columnWidths';

// 1200 px grid, two 9 px splitters, center keeps 480 px: 702 px for both side columns.
const base = {
    total: 1200,
    isLeftVisible: true,
    requested: { left: null, right: null },
    rendered: { left: 292, right: 400 },
};

describe('clampColumnWidths', () => {
    it('keeps the CSS defaults while nothing was requested', () => {
        expect(clampColumnWidths(base)).toEqual({ left: null, right: null, maxLeft: 702 - 400, maxRight: 702 - 292 });
    });

    it('passes a width through that fits', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 250, right: 400 } })).toMatchObject({ left: 250, right: 400 });
    });

    it('rounds to whole pixels', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 250.6, right: null } }).left).toBe(251);
    });

    it('keeps the minimum width of each side column', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 50, right: 50 } })).toMatchObject({ left: 210, right: 320 });
    });

    it('never makes the left column wider than 480 px, however much room there is', () => {
        expect(clampColumnWidths({ ...base, total: 1600, requested: { left: 700, right: null } })).toMatchObject({ left: 480, maxLeft: 480 });
    });

    it('leaves the center column its minimum, counting the other column with its rendered width', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 800, right: null } }).left).toBe(702 - 400);
        expect(clampColumnWidths({ ...base, requested: { left: null, right: 900 } }).right).toBe(702 - 292);
    });

    it('counts the other column with its requested width once there is one', () => {
        expect(clampColumnWidths({ ...base, requested: { left: 800, right: 350 } }).left).toBe(702 - 350);
    });

    it('never goes below the minimum, even when the grid is too narrow', () => {
        expect(clampColumnWidths({ ...base, total: 700, requested: { left: 400, right: 400 } }))
            .toEqual({ left: 210, right: 320, maxLeft: 210, maxRight: 320 });
    });

    it('gives the left column the room of the hidden right column and its splitter', () => {
        expect(clampColumnWidths({ ...base, total: 900, isRightVisible: false, requested: { left: 1000, right: 500 } }))
            .toMatchObject({ left: 900 - 9 - 480, maxLeft: 900 - 9 - 480 });
    });

    it('gives the right column the room of the hidden left column and its splitter', () => {
        expect(clampColumnWidths({ ...base, isLeftVisible: false, requested: { left: 250, right: 1000 } }))
            .toMatchObject({ right: 1200 - 9 - 480, maxRight: 1200 - 9 - 480 });
    });
});
