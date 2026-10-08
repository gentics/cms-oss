import { describe, expect, it } from 'vitest';

import type { UserMessagePart } from '@/services/apiService/genaix/types';

import { DEFAULT_WORKFLOW, guessWorkflow } from './workflowGuess';

function text(value: string): UserMessagePart[] {
    return [{ type: 'text', text: value }];
}

describe('guessWorkflow', () => {
    it.each([
        ['Create a landing page about the new terms of service', 'content_create'],
        ['Erstelle eine neue Seite zum Webinar', 'content_create'],
        ['Change the intro of the pricing page', 'content_edit'],
        ['Bitte überarbeite den Teaser', 'content_edit'],
        ['Which pages mention the old shipping threshold?', 'content_research'],
        ['Zeige mir alle Seiten offline', 'content_research'],
        ['Build a construct for a terms change table', 'construct_create'],
        ['Neuer Baustein für eine Tabelle', 'construct_create'],
        ['Add the user Jane Doe to the editors group', 'admin_user'],
        ['Lege einen Benutzer an', 'admin_user'],
    ])('picks the workflow of "%s"', (message, workflow) => {
        expect(guessWorkflow(text(message))).toBe(workflow);
    });

    it('reads the wording of verbatim parts too', () => {
        expect(guessWorkflow([
            { type: 'text', text: 'Please' },
            { type: 'verbatim', text: 'draft a page', source: 'typed' },
        ])).toBe('content_create');
    });

    it('matches at word starts only', () => {
        // "description" contains "script", "unchanged" contains "change": neither is a keyword match.
        expect(guessWorkflow(text('Hello there, the description is unchanged'))).toBe(DEFAULT_WORKFLOW);
    });

    it('falls back to the default workflow', () => {
        expect(guessWorkflow(text('Hello'))).toBe(DEFAULT_WORKFLOW);
        expect(guessWorkflow([])).toBe(DEFAULT_WORKFLOW);
    });
});
