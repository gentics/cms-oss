import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Interaction } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useInteractionDraftStore } from '@/store/useInteractionDraftStore';
import { createWrapper } from '@/test/renderWithProviders';

import { focusInteractionCard } from './focusInteractionCard';
import { ConfirmedSettingsCard, InteractionCard } from './InteractionCard';

function interaction(overrides: Partial<Interaction> & Pick<Interaction, 'kind' | 'prompt'>): Interaction {
    return {
        id: 'i-1',
        multi: false,
        other_allowed: false,
        blocking: true,
        status: 'pending',
        requested_at: new Date().toISOString(),
        // The contract's default lifetime: 10 minutes.
        expires_at: new Date(Date.now() + 10 * 60_000).toISOString(),
        ...overrides,
    };
}

function stubAnswer(response: () => Response = () => Response.json({ id: 'i-1', status: 'answered' })) {
    const fetchMock = vi.fn<typeof fetch>(async () => response());

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The `answer` of the one POST the card sent. Reads of the CMS lists a field offers (e.g. `GET /rest/node`
// for the languages) are left out.
async function sentAnswer(fetchMock: ReturnType<typeof stubAnswer>) {
    const posts = () => fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST');

    await waitFor(() => expect(posts()).toHaveLength(1));

    const [url, init] = posts()[0]!;

    expect(url).toBe('/rest/proxy/genaix/sessions/s-1/interactions/i-1');
    expect(init?.method).toBe('POST');

    return (JSON.parse(init?.body as string) as { answer: unknown }).answer;
}

function renderCard(value: Interaction) {
    return render(<InteractionCard interaction={value} sessionId="s-1" />, { wrapper: createWrapper() });
}

describe('InteractionCard', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
        useInteractionDraftStore.setState({ drafts: {}, locks: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.useRealTimers();
    });

    it('answers a settings review with one setting part per proposed and missing key', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({
            kind: 'settings_review',
            prompt: 'Ich habe diese Einstellungen abgeleitet.',
            proposed: [
                { key: 'folder', value: '42', label: 'Richtlinien', source: 'context' },
                { key: 'language', value: 'de', label: 'Deutsch', source: 'text', evidence: 'auf Deutsch' },
            ],
            missing: [{ key: 'template', label: 'Template', other_allowed: false, options: [{ value: '17', label: 'Kampagnen-Landingpage' }, { value: '21', label: 'Themenseite' }] }],
        }));

        // Only the settings; the prompt is not shown.
        expect(screen.queryByText('Ich habe diese Einstellungen abgeleitet.')).not.toBeInTheDocument();
        // Every setting is a value in the look of a field; a proposed one says under it where it came from.
        expect(screen.getByRole('button', { name: 'Folder Richtlinien' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Language Deutsch' })).toBeInTheDocument();
        expect(screen.getByText('From your text · „auf Deutsch“')).toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();

        // The missing template: "Choose…", "Selection required", and Confirm waits for it.
        const template = screen.getByRole('button', { name: 'Template Choose…' });
        const confirm = screen.getByRole('button', { name: 'Confirm' });

        expect(template).toHaveAttribute('data-attention');
        expect(template).toHaveAccessibleDescription('Selection required');
        expect(confirm).toBeDisabled();
        expect(screen.getByText('Still to choose: Template')).toBeInTheDocument();

        // A click opens its list at once.
        await user.click(template);
        await user.click(await screen.findByRole('option', { name: 'Kampagnen-Landingpage' }));

        await waitFor(() => expect(screen.queryByText('Still to choose: Template')).not.toBeInTheDocument());
        await user.click(confirm);

        expect(await sentAnswer(fetchMock)).toEqual({
            settings: [
                { type: 'setting', key: 'folder', value: '42', label: 'Richtlinien' },
                { type: 'setting', key: 'language', value: 'de', label: 'Deutsch' },
                { type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' },
            ],
        });
    });

    it('preselects the recommended folder for an upload, says so only while it is chosen, and lists the others on a click (rc.5 file_folder)', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({
            kind: 'settings_review',
            prompt: 'In welchen Ordner soll das PDF?',
            missing: [{
                key: 'file_folder',
                label: 'Ordner fuer das PDF',
                recommended: '88',
                reason: 'Downloads ist der Standard-Dateiordner des Knotens.',
                other_allowed: true,
                options: [
                    { value: '88', label: 'Downloads', ref: { type: 'folder', id: '88', node_id: 3, label: 'Downloads', path: '/Downloads/' } },
                    { value: '57', label: 'Veranstaltungen', ref: { type: 'folder', id: '57', node_id: 3, label: 'Veranstaltungen', path: '/Veranstaltungen/' } },
                ],
            }],
        }));

        // The value, "Recommended" in its field with the reason as its tooltip, and no button of its own.
        const folder = screen.getByRole('button', { name: 'Ordner fuer das PDF Downloads · /Downloads/' });

        expect(folder).toHaveAccessibleDescription('Recommended');
        expect(screen.getByText('Recommended')).toHaveAttribute('title', 'Downloads ist der Standard-Dateiordner des Knotens.');
        expect(screen.queryByRole('button', { name: 'Other folder…' })).not.toBeInTheDocument();

        // A click lists GenAIx's folders, and "Other folder…" for the folder search.
        await user.click(folder);

        expect(await screen.findByRole('option', { name: 'Other folder…' })).toBeInTheDocument();

        await user.click(screen.getByRole('option', { name: /Veranstaltungen/ }));

        await waitFor(() => expect(screen.queryByText('Recommended')).not.toBeInTheDocument());

        await user.click(screen.getByRole('button', { name: 'Confirm' }));

        expect(await sentAnswer(fetchMock)).toEqual({ settings: [{ type: 'setting', key: 'file_folder', value: '57', label: 'Veranstaltungen' }] });
    });

    it('answers a settings review with a corrected free-text value', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'settings_review', prompt: 'Passt das?', proposed: [{ key: 'filename', value: 'a.html', label: 'a.html', source: 'text' }] }));

        // The value opens its field with a click, which then has the focus.
        await user.click(screen.getByRole('button', { name: 'File name a.html' }));

        const field = screen.getByRole('textbox', { name: 'File name' });

        expect(field).toHaveFocus();
        await user.clear(field);
        await user.type(field, 'terms.html');
        await user.click(screen.getByRole('button', { name: 'Confirm' }));

        expect(await sentAnswer(fetchMock)).toEqual({ settings: [{ type: 'setting', key: 'filename', value: 'terms.html', label: 'terms.html' }] });
    });

    it('opens the folder search with a click on a folder without options, with no button of its own', async () => {
        const user = userEvent.setup();

        stubAnswer();
        renderCard(interaction({ kind: 'settings_review', prompt: 'Passt das?', proposed: [{ key: 'folder', value: '42', label: 'Richtlinien', source: 'context' }] }));

        expect(screen.queryByRole('button', { name: /Choose folder/ })).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Folder Richtlinien' }));

        expect(await screen.findByRole('dialog', { name: 'Choose a folder' })).toBeInTheDocument();
    });

    it('answers a confirm with yes and a comment the user chose to add', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'confirm', prompt: 'Create the construct terms_change_table?' }));

        await user.click(screen.getByRole('button', { name: 'Add a comment' }));

        expect(screen.getByRole('textbox', { name: 'Comment' })).toHaveFocus();
        expect(screen.queryByRole('button', { name: 'Add a comment' })).not.toBeInTheDocument();

        await user.keyboard('Go ahead');
        await user.click(screen.getByRole('button', { name: 'Yes' }));

        expect(await sentAnswer(fetchMock)).toEqual({ approved: true, comment: 'Go ahead' });
    });

    it('answers a confirm with no, offering yes and no without a comment field', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        expect(screen.queryByRole('textbox', { name: 'Comment' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Yes' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'No' }));

        expect(await sentAnswer(fetchMock)).toEqual({ approved: false });
    });

    it('answers a choice with the selected option ids', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({
            kind: 'choice',
            prompt: 'Which pages?',
            multi: true,
            options: [{ id: 'p-1', label: 'Garantiebedingungen' }, { id: 'p-2', label: 'Versand' }],
        }));

        const send = screen.getByRole('button', { name: 'Send' });

        expect(send).toBeDisabled();

        await user.click(screen.getByRole('checkbox', { name: 'Versand' }));
        await user.click(screen.getByRole('checkbox', { name: 'Garantiebedingungen' }));
        await user.click(send);

        expect(await sentAnswer(fetchMock)).toEqual({ selected: ['p-2', 'p-1'] });
    });

    describe('a choice with what GenAIx recommends (rc.5 addition)', () => {
        // S7's run 8: where the FAQ page goes, Service recommended, the user's own answer allowed.
        const faqFolder = interaction({
            kind: 'choice',
            prompt: 'Wo soll die FAQ-Seite liegen?',
            options: [
                { id: 'folder-18', label: 'Aktuelles', reason: 'Dort erscheint die Seite in der Uebersicht der Neuigkeiten.' },
                { id: 'folder-71', label: 'Service', reason: 'Dort liegen die anderen Hilfeseiten.' },
            ],
            recommended: ['folder-71'],
            other_allowed: true,
        });

        it('puts the recommended option first, preselected and badged, each option with its reason', async () => {
            const user = userEvent.setup();
            const fetchMock = stubAnswer();

            renderCard(faqFolder);

            const [first, second] = screen.getAllByRole('radio');

            expect(first).toHaveAccessibleName(/Service/);
            expect(first).toBeChecked();
            expect(second).toHaveAccessibleName(/Aktuelles/);
            expect(screen.getByText('Recommended')).toBeInTheDocument();
            expect(screen.getByText('Dort liegen die anderen Hilfeseiten.')).toBeInTheDocument();
            expect(screen.getByText('Dort erscheint die Seite in der Uebersicht der Neuigkeiten.')).toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: 'Send' }));

            expect(await sentAnswer(fetchMock)).toEqual({ selected: ['folder-71'] });
        });

        it('sends the user\'s own text as `other`, with nothing selected, from "Something else"', async () => {
            const user = userEvent.setup();
            const fetchMock = stubAnswer();

            renderCard(faqFolder);

            await user.click(screen.getByRole('radio', { name: 'Something else' }));

            // Chosen, but nothing written yet.
            expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();

            await user.type(screen.getByRole('textbox', { name: 'Your own answer' }), 'Lieber als News-Meldung');
            await user.click(screen.getByRole('button', { name: 'Send' }));

            expect(await sentAnswer(fetchMock)).toEqual({ selected: [], other: 'Lieber als News-Meldung' });
        });

        it('chooses "Something else" by typing into its field', async () => {
            const user = userEvent.setup();

            stubAnswer();
            renderCard(faqFolder);

            await user.type(screen.getByRole('textbox', { name: 'Your own answer' }), 'Hilfe');

            expect(screen.getByRole('radio', { name: 'Something else' })).toBeChecked();
            expect(screen.getByRole('radio', { name: /Service/ })).not.toBeChecked();
        });

        it('sends the user\'s own text besides the options chosen in a multiple choice', async () => {
            const user = userEvent.setup();
            const fetchMock = stubAnswer();

            renderCard({ ...faqFolder, multi: true, recommended: ['folder-71', 'folder-18'] });

            expect(screen.getByRole('checkbox', { name: /Service/ })).toBeChecked();
            expect(screen.getByRole('checkbox', { name: /Aktuelles/ })).toBeChecked();

            await user.type(screen.getByRole('textbox', { name: 'Your own answer' }), 'und Hilfe');
            await user.click(screen.getByRole('button', { name: 'Send' }));

            expect(await sentAnswer(fetchMock)).toEqual({ selected: ['folder-71', 'folder-18'], other: 'und Hilfe' });
        });

        it('offers no answer of the user\'s own without `other_allowed`, and preselects one option only in a single choice', () => {
            renderCard({ ...faqFolder, other_allowed: false, recommended: ['folder-71', 'folder-18'] });

            expect(screen.queryByRole('radio', { name: 'Something else' })).not.toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
            expect(screen.getByRole('radio', { name: /Service/ })).toBeChecked();
            expect(screen.getByRole('radio', { name: /Aktuelles/ })).not.toBeChecked();
        });
    });

    it('is scrolled to and focused by `focusInteractionCard`', () => {
        renderCard(interaction({ kind: 'ask_user', prompt: 'Which audience?' }));

        expect(focusInteractionCard('i-1')).toBe(true);
        expect(document.activeElement).toHaveAttribute('id', 'interaction-card-i-1');
        expect(document.activeElement).toContainElement(screen.getByText('Which audience?'));
        expect(focusInteractionCard('unknown')).toBe(false);
    });

    it('answers a form with typed values, the required ones first', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({
            kind: 'form',
            prompt: 'Details of the new user',
            schema: {
                type: 'object',
                required: ['login', 'age'],
                properties: {
                    login: { type: 'string', title: 'Login' },
                    age: { type: 'integer', title: 'Age' },
                    admin: { type: 'boolean', title: 'Administrator' },
                    note: { type: 'string', title: 'Note' },
                },
            },
        }));

        const send = screen.getByRole('button', { name: 'Send' });

        await user.type(screen.getByRole('textbox', { name: /Login/ }), 'jdoe');
        expect(send).toBeDisabled();

        await user.type(screen.getByRole('spinbutton', { name: /Age/ }), '42');
        await user.click(screen.getByRole('checkbox', { name: 'Administrator' }));
        await user.click(send);

        expect(await sentAnswer(fetchMock)).toEqual({ values: { login: 'jdoe', age: 42, admin: true } });
    });

    it('answers an ask_user with the text', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'ask_user', prompt: 'Which audience?' }));

        await user.type(screen.getByRole('textbox', { name: 'Your answer' }), 'Existing customers');
        await user.click(screen.getByRole('button', { name: 'Send' }));

        expect(await sentAnswer(fetchMock)).toEqual({ text: 'Existing customers' });
    });

    it('is answered once: locked while the answer is on its way, and marked as answered after', async () => {
        const user = userEvent.setup();
        let accept: (response: Response) => void = () => {};
        const fetchMock = vi.fn<typeof fetch>(() => new Promise((resolve) => {
            accept = resolve;
        }));

        vi.stubGlobal('fetch', fetchMock);
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        await user.click(screen.getByRole('button', { name: 'Yes' }));

        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
        expect(screen.getByRole('button', { name: 'No' })).toBeDisabled();

        accept(Response.json({ id: 'i-1', status: 'answered' }));

        expect(await screen.findByText('Answered')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
        expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('stays locked while the composer sends an answer to it', () => {
        useInteractionDraftStore.getState().lock('i-1', 'sending');
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
        expect(screen.queryByText('Answered')).not.toBeInTheDocument();
    });

    it('shows an error and locks as expired when GenAIx refuses the answer with 410', async () => {
        const user = userEvent.setup();

        stubAnswer(() => Response.json({ type: 't', title: 'Expired', status: 410, genaix_code: 'interaction_expired' }, { status: 410 }));
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        await user.click(screen.getByRole('button', { name: 'Yes' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'chat.interaction.answerFailed', detailKey: 'errors.genaix.interaction_expired' },
        ]));
        expect(screen.getByText('Expired')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
    });

    it('shows an error and locks as answered when GenAIx already has an answer (409)', async () => {
        const user = userEvent.setup();

        stubAnswer(() => Response.json({ type: 't', title: 'Answered', status: 409, genaix_code: 'interaction_already_answered' }, { status: 409 }));
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        await user.click(screen.getByRole('button', { name: 'Yes' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'chat.interaction.answerFailed', detailKey: 'errors.genaix.interaction_already_answered' },
        ]));
        expect(screen.getByText('Answered')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
    });

    it('can be answered again after any other failure', async () => {
        const user = userEvent.setup();

        stubAnswer(() => Response.json({ type: 't', title: 'Invalid', status: 422, genaix_code: 'validation_failed' }, { status: 422 }));
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        await user.click(screen.getByRole('button', { name: 'Yes' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toHaveLength(1));
        expect(screen.getByRole('button', { name: 'Yes' })).toBeEnabled();
        expect(screen.queryByText('Expired')).not.toBeInTheDocument();
    });

    it('cannot be answered once it has expired', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'ask_user', prompt: 'Which audience?', expires_at: new Date(Date.now() - 1000).toISOString() }));

        expect(screen.getByText('Expired')).toBeInTheDocument();
        expect(screen.getByRole('textbox', { name: 'Your answer' })).toBeDisabled();

        await user.type(screen.getByRole('textbox', { name: 'Your answer' }), 'Existing customers{Enter}');

        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('locks at the moment it expires', () => {
        vi.useFakeTimers({ now: new Date('2026-10-07T09:23:16Z') });
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?', expires_at: '2026-10-07T09:33:16Z' }));

        act(() => vi.advanceTimersByTime(10 * 60_000 - 1));

        expect(screen.getByRole('button', { name: 'Yes' })).toBeEnabled();

        act(() => vi.advanceTimersByTime(1));

        expect(screen.getByRole('button', { name: 'Yes' })).toBeDisabled();
        expect(screen.getByText('Expired')).toBeInTheDocument();
    });
});

describe('ConfirmedSettingsCard', () => {
    it('shows the confirmed settings under their names, read-only, marked as confirmed', () => {
        render(
            <ConfirmedSettingsCard
                settings={[
                    { type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' },
                    { type: 'setting', key: 'language', value: 'de', label: '' },
                    { type: 'setting', key: 'audience', value: 'Fachhandel', label: 'Fachhandel' },
                ]}
            />,
            { wrapper: createWrapper() },
        );

        const card = screen.getByRole('region', { name: 'Check settings' });
        const terms = screen.getAllByRole('term').map((term) => term.textContent);
        const values = screen.getAllByRole('definition').map((value) => value.textContent);

        // The setting's name, its raw key where there is none; the wording, its value where there is none.
        expect(terms).toEqual(['Template', 'Language', 'audience']);
        expect(values).toEqual(['Kampagnen-Landingpage', 'de', 'Fachhandel']);
        expect(card).toHaveTextContent('Confirmed');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    });
});
