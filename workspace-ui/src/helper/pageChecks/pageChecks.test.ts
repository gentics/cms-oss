import { describe, expect, it } from 'vitest';

import type { Check, CmsObjectRef } from '@/services/apiService/genaix/types';

import { checkSummary, isCheckForPage, isCheckOnPage, isFailingCheck, isGateCheck } from './pageChecks';

const PAGE: CmsObjectRef = { type: 'page', id: 8644, node_id: 3 };

function check(severity: Check['severity'], target?: Check['target']): Check {
    return { id: 'chk-1', label: 'Check', severity, blocking: false, message: '', source: { type: 'gate' }, ...(target && { target }) };
}

describe('pageChecks', () => {
    it('tells a gate\'s check from the agent\'s own finding by its gate id', () => {
        expect(isGateCheck({ ...check('warn'), gate_id: 'references_exist', kind: 'script' })).toBe(true);
        // A gate naming a guideline keeps its guideline source.
        expect(isGateCheck({ ...check('warn'), source: { type: 'guideline', id: 'accessibility-basics' }, gate_id: 'alt_text', kind: 'script' })).toBe(true);
        expect(isGateCheck({ ...check('info'), source: { type: 'gate' }, gate_id: 'release_self_check', kind: 'prompt' })).toBe(true);
        expect(isGateCheck({ ...check('warn'), source: { type: 'system' }, kind: 'prompt' })).toBe(false);
        expect(isGateCheck({ ...check('info'), source: { type: 'requirement' } })).toBe(false);
    });

    it('sums a check up by its gate\'s verdict summary, else by its message', () => {
        const found = { ...check('warn'), message: 'Placeholder alt text.\nwarn: The alt text "test" is not descriptive.' };

        expect(checkSummary({ ...found, evidence: { summary: 'Placeholder alt text.', verdict: 'warn' } })).toBe('Placeholder alt text.');
        expect(checkSummary(found)).toBe(found.message);
        expect(checkSummary({ ...found, evidence: { summary: '' } })).toBe(found.message);
        expect(checkSummary({ ...found, evidence: { summary: 3 } })).toBe(found.message);
    });

    it('counts warnings and failures as failing', () => {
        expect(isFailingCheck(check('warn'))).toBe(true);
        expect(isFailingCheck(check('fail'))).toBe(true);
        expect(isFailingCheck(check('pass'))).toBe(false);
        expect(isFailingCheck(check('info'))).toBe(false);
    });

    it('puts a check on the page it targets', () => {
        expect(isCheckOnPage(check('fail', { type: 'page', id: '8644' }), PAGE)).toBe(true);
        expect(isCheckOnPage(check('fail', { type: 'page', id: '9011' }), PAGE)).toBe(false);
        expect(isCheckOnPage(check('fail'), PAGE)).toBe(false);
    });

    it('shows a check on its page, and one without a page on every page', () => {
        expect(isCheckForPage(check('fail', { type: 'page', id: '8644' }), PAGE)).toBe(true);
        expect(isCheckForPage(check('fail', { type: 'page', id: '9011' }), PAGE)).toBe(false);
        expect(isCheckForPage(check('fail'), PAGE)).toBe(true);
        expect(isCheckForPage(check('fail', { type: 'image', id: '77' }), PAGE)).toBe(true);
    });
});
