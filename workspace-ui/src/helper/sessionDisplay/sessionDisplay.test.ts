import { describe, expect, it } from 'vitest';

import i18n from '@/i18n';

import { formatSessionDate, sessionTitle } from './sessionDisplay';

describe('sessionTitle', () => {
    it('prefers the title, then the intent, then "Untitled session"', () => {
        expect(sessionTitle({ title: 'Landing page', intent: 'Create a page' }, i18n.t)).toBe('Landing page');
        expect(sessionTitle({ title: ' ', intent: 'Create a page' }, i18n.t)).toBe('Create a page');
        expect(sessionTitle({}, i18n.t)).toBe('Untitled session');
    });
});

describe('formatSessionDate', () => {
    const now = new Date('2026-10-02T12:00:00Z');

    // Month abbreviations depend on the ICU version, so only the day and the year are asserted.
    it('shows the day without the year for this year', () => {
        const formatted = formatSessionDate('2026-09-23T10:00:00Z', 'en', now);

        expect(formatted).toContain('23');
        expect(formatted).not.toContain('2026');
    });

    it('adds the year for an earlier year', () => {
        expect(formatSessionDate('2025-09-23T10:00:00Z', 'en', now)).toContain('2025');
    });
});
