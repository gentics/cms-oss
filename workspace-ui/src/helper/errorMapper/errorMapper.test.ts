import { describe, expect, it } from 'vitest';

import en from '@/i18n/locales/en/common.json';
import { GenaixApiError } from '@/services/apiService/apiService';
import type { Problem } from '@/services/apiService/genaix/types';
import { HttpError } from '@/services/httpService/httpService';

import { errorMessageKey, problemMessageKey } from './errorMapper';

function genaixError(status: number, genaixCode: string): GenaixApiError {
    const problem = { type: 'https://genaix.gentics.com/problems/x', title: 'x', status, genaix_code: genaixCode } as Problem;

    return new GenaixApiError('/genaix/api/v1/x', status, problem, new Headers());
}

function cmsError(status: number, responseCode: string): HttpError {
    return new HttpError('/rest/admin/token', status, { messages: [], responseInfo: { responseCode } }, new Headers());
}

// The value at a dotted i18n key in the English resources, or undefined.
function translation(key: string): unknown {
    return key.split('.').reduce<unknown>((node, part) => (node as Record<string, unknown> | undefined)?.[part], en);
}

describe('errorMessageKey', () => {
    it('maps a GenAIx error by its genaix_code', () => {
        expect(errorMessageKey(genaixError(409, 'run_already_active'))).toBe('errors.genaix.run_already_active');
    });

    it('falls back to the status for a genaix_code it does not know', () => {
        expect(errorMessageKey(genaixError(409, 'added_in_a_later_version'))).toBe('errors.http.failed');
        expect(errorMessageKey(genaixError(503, 'added_in_a_later_version'))).toBe('errors.http.server');
    });

    it('maps a CMS error by responseInfo.responseCode', () => {
        expect(errorMessageKey(cmsError(403, 'PERMISSION'))).toBe('errors.cms.PERMISSION');
        expect(errorMessageKey(cmsError(503, 'MAINTENANCEMODE'))).toBe('errors.cms.MAINTENANCEMODE');
    });

    it('falls back to the status for a responseCode it does not know, or a body without one', () => {
        expect(errorMessageKey(cmsError(403, 'SOMETHING_NEW'))).toBe('errors.http.forbidden');
        expect(errorMessageKey(new HttpError('/x', 404, 'Not Found'))).toBe('errors.http.notFound');
        expect(errorMessageKey(new HttpError('/x', 401))).toBe('errors.http.unauthorized');
    });

    it('maps an HttpError by its status', () => {
        expect(errorMessageKey(new HttpError('/x', 429))).toBe('errors.http.tooManyRequests');
        expect(errorMessageKey(new HttpError('/x', 500))).toBe('errors.http.server');
        expect(errorMessageKey(new HttpError('/x', 502))).toBe('errors.http.server');
        expect(errorMessageKey(new HttpError('/x', 400))).toBe('errors.http.failed');
    });

    it('maps a network error, an unreadable body and anything else', () => {
        expect(errorMessageKey(new TypeError('Failed to fetch'))).toBe('errors.network');
        expect(errorMessageKey(new SyntaxError('Unexpected token <'))).toBe('errors.invalidResponse');
        expect(errorMessageKey(new Error('other'))).toBe('errors.unknown');
        expect(errorMessageKey('not an error')).toBe('errors.unknown');
    });

    it('returns only keys that have an English text', () => {
        const keys = [
            errorMessageKey(genaixError(503, 'service_unavailable')),
            errorMessageKey(cmsError(500, 'FAILURE')),
            errorMessageKey(new HttpError('/x', 400)),
            errorMessageKey(new TypeError('x')),
            errorMessageKey(new SyntaxError('x')),
            errorMessageKey(null),
        ];

        keys.forEach((key) => expect(typeof translation(key), key).toBe('string'));
    });
});

describe('problemMessageKey', () => {
    const problem = (status: number, genaixCode: string) => ({ type: 't', title: 'x', status, genaix_code: genaixCode }) as Problem;

    it('maps a Problem from the event stream by its genaix_code', () => {
        expect(problemMessageKey(problem(424, 'mcp_authorization_required'))).toBe('errors.genaix.mcp_authorization_required');
    });

    it('falls back to the status for a genaix_code it does not know', () => {
        expect(problemMessageKey(problem(502, 'added_in_a_later_version'))).toBe('errors.http.server');
        expect(typeof translation(problemMessageKey(problem(423, 'cms_object_locked')))).toBe('string');
    });
});
