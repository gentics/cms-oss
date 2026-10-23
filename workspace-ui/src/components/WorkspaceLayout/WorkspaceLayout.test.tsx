import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { WorkspaceLayout } from './WorkspaceLayout';

import '@/i18n';

// jsdom has no layout. The stubs below play the browser: the grid is `gridWidth` px wide and each side
// track is as wide as its CSS variable says, or its default (292 px / 46 %). As in the browser, a
// column's box is 8 px wider than its track (`margin-inline: -4px` reaches under the splitters), and
// the grid's computed `gridTemplateColumns` lists the resolved tracks. `measure()` stands in for the
// ResizeObserver firing.
let gridWidth = 1600;
let resizeCallbacks: (() => void)[] = [];

class ResizeObserverStub {
    constructor(callback: () => void) {
        resizeCallbacks.push(callback);
    }

    observe() {}

    disconnect() {}
}

function rect(left: number, width: number) {
    return { x: left, y: 0, left, top: 0, right: left + width, bottom: 0, width, height: 0, toJSON: () => ({}) } as DOMRect;
}

function columnWidth(grid: HTMLElement, variable: string, fallback: number) {
    const value = grid.style.getPropertyValue(variable);

    return value ? Number.parseFloat(value) : fallback;
}

/** Overlap of a column under the splitters on both sides together, in px. */
const COLUMN_OVERLAP = 8;

function tracks(layout: HTMLElement) {
    const left = screen.getByTestId('left').parentElement!.hidden ? 0 : columnWidth(layout, '--w-left', 292);
    const right = columnWidth(layout, '--w-right', gridWidth * 0.46);
    const leftSplitter = left === 0 ? 0 : 9;

    return { left, leftSplitter, center: gridWidth - left - leftSplitter - 9 - right, right };
}

function stubLayout() {
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
        const layout = grid();

        if (this === layout) {
            return rect(0, gridWidth);
        }
        if (this.hidden) {
            return rect(0, 0);
        }
        if (this === screen.getByTestId('left').parentElement) {
            return rect(0, tracks(layout).left + COLUMN_OVERLAP);
        }
        if (this === screen.getByTestId('right').parentElement) {
            return rect(0, tracks(layout).right + COLUMN_OVERLAP);
        }

        return rect(0, 0);
    });

    const getComputedStyle = window.getComputedStyle.bind(window);

    // Only the grid's `gridTemplateColumns` is faked; every other element keeps jsdom's computed style.
    vi.spyOn(window, 'getComputedStyle').mockImplementation((element, pseudoElement) => {
        if (element !== grid()) {
            return getComputedStyle(element, pseudoElement);
        }

        const { left, leftSplitter, center, right } = tracks(grid());

        return { gridTemplateColumns: `${left}px ${leftSplitter}px ${center}px 9px ${right}px` } as CSSStyleDeclaration;
    });
}

function measure() {
    act(() => {
        resizeCallbacks.forEach((callback) => callback());
    });
}

function renderLayout(isLeftColumnVisible = true, isRightColumnVisible = true) {
    const result = render(
        <WorkspaceLayout
            left={<p data-testid="left">Sessions</p>}
            center={<p data-testid="center">Chat</p>}
            right={<p data-testid="right">Preview</p>}
            isLeftColumnVisible={isLeftColumnVisible}
            isRightColumnVisible={isRightColumnVisible}
        />,
    );

    measure();

    return result;
}

const grid = () => screen.getByTestId('center').parentElement!.parentElement!;
const leftSplitter = () => screen.getByRole('separator', { name: 'Width of the left column' });
const rightSplitter = () => screen.getByRole('separator', { name: 'Width of the right column' });

describe('WorkspaceLayout', () => {
    beforeEach(() => {
        gridWidth = 1600;
        resizeCallbacks = [];
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
        HTMLElement.prototype.setPointerCapture = vi.fn();
        stubLayout();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it('renders the content of all three columns', () => {
        renderLayout();

        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        expect(screen.getByText('Preview')).toBeVisible();
    });

    it('describes each splitter with the width of its column and the limits', () => {
        renderLayout();

        // 1600 − 2 × 9 splitter − 480 center = 1102 for both side columns; the right one is 46 % = 736.
        expect(leftSplitter()).toHaveAttribute('aria-orientation', 'vertical');
        expect(leftSplitter()).toHaveAttribute('aria-valuenow', '292');
        expect(leftSplitter()).toHaveAttribute('aria-valuemin', '210');
        expect(leftSplitter()).toHaveAttribute('aria-valuemax', String(1102 - 736));
        expect(rightSplitter()).toHaveAttribute('aria-valuenow', '736');
        expect(rightSplitter()).toHaveAttribute('aria-valuemin', '320');
        expect(rightSplitter()).toHaveAttribute('aria-valuemax', String(1102 - 292));
    });

    it('keeps the CSS default widths until a column is resized', () => {
        renderLayout();

        expect(grid().style.getPropertyValue('--w-left')).toBe('');
        expect(grid().style.getPropertyValue('--w-right')).toBe('');
    });

    it('moves a splitter with the arrow keys, further with Shift', async () => {
        const user = userEvent.setup();

        renderLayout();

        leftSplitter().focus();
        await user.keyboard('{ArrowLeft}');

        expect(grid().style.getPropertyValue('--w-left')).toBe('260px');

        measure();
        await user.keyboard('{ArrowRight}');

        expect(grid().style.getPropertyValue('--w-left')).toBe('292px');

        rightSplitter().focus();
        await user.keyboard('{Shift>}{ArrowRight}{/Shift}');

        expect(grid().style.getPropertyValue('--w-right')).toBe(`${736 - 96}px`);
    });

    it('keeps the center column its minimum and each side column its minimum', async () => {
        const user = userEvent.setup();

        renderLayout();

        leftSplitter().focus();
        await user.keyboard('{Shift>}{ArrowRight}{ArrowRight}{/Shift}');

        expect(grid().style.getPropertyValue('--w-left')).toBe(`${1102 - 736}px`);

        rightSplitter().focus();

        for (let press = 0; press < 5; press++) {
            await user.keyboard('{Shift>}{ArrowRight}{/Shift}');
            measure();
        }

        expect(grid().style.getPropertyValue('--w-right')).toBe('320px');
    });

    it('resets a column to its default width with Home and with a double-click', async () => {
        const user = userEvent.setup();

        renderLayout();

        leftSplitter().focus();
        await user.keyboard('{ArrowRight}');
        await user.keyboard('{Home}');

        expect(grid().style.getPropertyValue('--w-left')).toBe('');

        rightSplitter().focus();
        await user.keyboard('{ArrowLeft}');

        expect(grid().style.getPropertyValue('--w-right')).not.toBe('');

        await user.dblClick(rightSplitter());

        expect(grid().style.getPropertyValue('--w-right')).toBe('');
    });

    it('resizes a column by dragging its splitter, and stops when the pointer is released', () => {
        renderLayout();

        fireEvent.pointerDown(leftSplitter(), { pointerId: 1, clientX: 296 });
        fireEvent.pointerMove(leftSplitter(), { pointerId: 1, clientX: 254.5 });

        // Pointer position minus half a splitter.
        expect(grid().style.getPropertyValue('--w-left')).toBe('250px');

        fireEvent.pointerMove(rightSplitter(), { pointerId: 1, clientX: 700 });

        expect(grid().style.getPropertyValue('--w-right')).toBe('');

        fireEvent.pointerUp(leftSplitter(), { pointerId: 1 });
        fireEvent.pointerMove(leftSplitter(), { pointerId: 1, clientX: 400 });

        expect(grid().style.getPropertyValue('--w-left')).toBe('250px');

        fireEvent.pointerDown(rightSplitter(), { pointerId: 2, clientX: 640 });
        fireEvent.pointerMove(rightSplitter(), { pointerId: 2, clientX: 1104.5 });

        expect(grid().style.getPropertyValue('--w-right')).toBe(`${1600 - 1104.5 - 4.5}px`);
    });

    it('stops a splitter at its limit instead of resizing the other side column', () => {
        renderLayout();

        fireEvent.pointerDown(rightSplitter(), { pointerId: 1, clientX: 900 });
        fireEvent.pointerMove(rightSplitter(), { pointerId: 1, clientX: 1004.5 });
        fireEvent.pointerUp(rightSplitter(), { pointerId: 1 });
        measure();

        expect(grid().style.getPropertyValue('--w-right')).toBe('591px');

        // Far past the limit: 1102 − 591 = 511 is left, of which the left column takes its 480 px at most.
        fireEvent.pointerDown(leftSplitter(), { pointerId: 2, clientX: 296 });
        fireEvent.pointerMove(leftSplitter(), { pointerId: 2, clientX: 1200 });
        measure();

        expect(grid().style.getPropertyValue('--w-left')).toBe('480px');
        expect(grid().style.getPropertyValue('--w-right')).toBe('591px');

        // Back from past the limit, the splitter follows the pointer at once.
        fireEvent.pointerMove(leftSplitter(), { pointerId: 2, clientX: 404.5 });

        expect(grid().style.getPropertyValue('--w-left')).toBe('400px');
    });

    it('clamps the widths again when the workspace gets narrower, so they can still be adjusted', async () => {
        const user = userEvent.setup();

        renderLayout();

        leftSplitter().focus();
        await user.keyboard('{ArrowLeft}');

        expect(grid().style.getPropertyValue('--w-left')).toBe('260px');

        gridWidth = 1000;
        measure();

        // 1000 − 18 − 480 − 460 (46 % right column) = 42, below the minimum of 210.
        expect(grid().style.getPropertyValue('--w-left')).toBe('210px');
        expect(leftSplitter()).toHaveAttribute('aria-valuemax', '210');

        // Wider again: the requested width comes back.
        gridWidth = 1800;
        measure();

        expect(grid().style.getPropertyValue('--w-left')).toBe('260px');
    });

    it('measures the grid tracks, not the column boxes that reach under the splitters', async () => {
        const user = userEvent.setup();

        renderLayout();

        expect(leftSplitter()).toHaveAttribute('aria-valuenow', '292');
        expect(rightSplitter()).toHaveAttribute('aria-valuenow', '736');

        leftSplitter().focus();
        await user.keyboard('{ArrowLeft}');

        // One step from the 292 px track, not from the 300 px column box.
        expect(grid().style.getPropertyValue('--w-left')).toBe(`${292 - 32}px`);
    });

    it('hides the left column and its splitter but keeps the content mounted', () => {
        renderLayout(false);

        expect(screen.getByTestId('left')).not.toBeVisible();
        expect(screen.queryByRole('separator', { name: 'Width of the left column' })).not.toBeInTheDocument();
        expect(rightSplitter()).toHaveAttribute('aria-valuemax', String(1600 - 9 - 480));
    });

    it('hides the right column and its splitter while there is no preview, but keeps the content mounted', () => {
        renderLayout(true, false);

        expect(screen.getByTestId('right')).not.toBeVisible();
        expect(screen.queryByRole('separator', { name: 'Width of the right column' })).not.toBeInTheDocument();
        expect(screen.getByText('Sessions')).toBeVisible();
        expect(screen.getByText('Chat')).toBeVisible();
        // The left column may take the room of the hidden right column and its splitter, up to 480 px.
        expect(leftSplitter()).toHaveAttribute('aria-valuemax', '480');
    });

    it('can hide both side columns, leaving only the center', () => {
        renderLayout(false, false);

        expect(screen.getByTestId('left')).not.toBeVisible();
        expect(screen.getByTestId('right')).not.toBeVisible();
        expect(screen.queryAllByRole('separator')).toHaveLength(0);
        expect(screen.getByText('Chat')).toBeVisible();
    });
});
