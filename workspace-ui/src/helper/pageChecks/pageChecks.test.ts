import { describe, expect, it } from 'vitest';

import type { Check, CmsObjectRef } from '@/services/apiService/genaix/types';

import { isCheckForPage, isCheckOnPage, isFailingCheck } from './pageChecks';

const PAGE: CmsObjectRef = { type: 'page', id: 8644, node_id: 3 };

function check(severity: Check['severity'], target?: Check['target']): Check {
    return { id: 'chk-1', label: 'Check', severity, blocking: false, message: '', source: { type: 'gate' }, ...(target && { target }) };
}

describe('pageChecks', () => {
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
