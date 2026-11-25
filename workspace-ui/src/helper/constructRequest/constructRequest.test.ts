import { describe, expect, it } from 'vitest';

import i18n from '@/i18n';
import type { ConstructDraftPart, Interaction } from '@/services/apiService/genaix/types';

import { constructInstruction, constructRequestTask, isConstructRequest, latestConstructDraft, requestsConstruct } from './constructRequest';

const t = i18n.getFixedT('en');

// The draft of the S3 transcript (`examples/s3-construct-create.sse`), shortened.
const draft: ConstructDraftPart = {
    type: 'construct_draft',
    keyword: 'terms_change_table',
    name: { de: 'Aenderungstabelle Nutzungsbedingungen', en: 'Terms change table' },
    description: { en: 'Compares the old and the new legal situation row by row.' },
    node_ids: [3],
    parts: [
        { keyword: 'heading', name: { en: 'Heading' }, type_id: 1, editable: true, mandatory: true, hidden: false },
        { keyword: 'label_old', name: { en: 'Column title old' }, type_id: 1, editable: true, mandatory: false, hidden: false, default: 'Old terms' },
    ],
    template: '<table>{{cms.tag.parts.heading}}</table>',
};

function request(overrides: Partial<Interaction> = {}): Interaction {
    return {
        id: 'i-7',
        kind: 'choice',
        prompt: 'No construct fits, and you may not create one. How should I go on?',
        options: [
            { id: 'request_construct', label: 'Request construct', ref: { type: 'page', id: '9142', node_id: 3, label: 'Terms of use' } },
            { id: 'continue_without', label: 'Go on without a new construct' },
        ],
        multi: false,
        other_allowed: false,
        blocking: true,
        status: 'pending',
        requested_at: '2026-10-07T11:04:20Z',
        expires_at: '2026-10-07T11:14:20Z',
        run_id: 'r-1',
        ...overrides,
    };
}

describe('constructRequest', () => {
    it('recognises a choice with the reserved option, and nothing else', () => {
        expect(isConstructRequest(request())).toBe(true);
        expect(isConstructRequest(request({ options: [{ id: 'request', label: 'Request construct' }] }))).toBe(false);
        expect(isConstructRequest(request({ kind: 'confirm' }))).toBe(false);
        expect(requestsConstruct({ selected: ['request_construct'] })).toBe(true);
        expect(requestsConstruct({ selected: ['continue_without'] })).toBe(false);
        expect(requestsConstruct(undefined)).toBe(false);
    });

    it('takes the last draft of the request\'s run that was not created', () => {
        const other = { ...draft, keyword: 'other_run' };
        const created = { ...draft, keyword: 'created', created_ref: { type: 'construct' as const, id: 88 } };
        const earlier = { ...draft, keyword: 'earlier' };
        const entries = [
            { runId: 'r-0', parts: [other] },
            { runId: 'r-1', parts: [earlier, undefined] },
            { runId: 'r-1', parts: [draft, created] },
        ];

        expect(latestConstructDraft(entries, 'r-1')?.keyword).toBe('terms_change_table');
        expect(latestConstructDraft(entries, 'r-2')).toBeUndefined();
        expect(latestConstructDraft(entries, undefined)).toBeUndefined();
    });

    it('writes the draft as an instruction GenAIx can follow on its own', () => {
        expect(constructInstruction(draft, t, 'en')).toBe([
            'Create the construct "Terms change table" (terms_change_table) in the CMS. It was requested because the requester may not create constructs.',
            'Names: de: Aenderungstabelle Nutzungsbedingungen, en: Terms change table',
            'Description: Compares the old and the new legal situation row by row.',
            'Nodes: 3',
            'Parts:',
            '- heading (Heading): type 1, mandatory',
            '- label_old (Column title old): type 1, default: Old terms',
            'Template:',
            '<table>{{cms.tag.parts.heading}}</table>',
        ].join('\n'));
    });

    it('builds the task for the group, with the page the request names and the interaction as its key', () => {
        expect(constructRequestTask(draft, request(), 9, t, 'en')).toEqual({
            type: 'requestConstruct',
            title: 'Create construct: Terms change table',
            description: 'Compares the old and the new legal situation row by row.',
            assignee: { type: 'group', id: 9 },
            objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
            externalKey: 'i-7',
            genaix: { instruction: constructInstruction(draft, t, 'en') },
        });
    });

    it('leaves the objects out when the request names none', () => {
        const withoutRef = request({ options: [{ id: 'request_construct', label: 'Request construct' }, { id: 'continue_without', label: 'Go on' }] });

        expect(constructRequestTask(draft, withoutRef, 9, t, 'en')).not.toHaveProperty('objects');
    });
});
