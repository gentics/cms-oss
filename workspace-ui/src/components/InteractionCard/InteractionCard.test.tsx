import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Interaction } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { createWrapper } from '@/test/renderWithProviders';

import { ConfirmedSettingsCard, InteractionCard } from './InteractionCard';

function interaction(overrides: Partial<Interaction> & Pick<Interaction, 'kind' | 'prompt'>): Interaction {
    return {
        id: 'i-1',
        multi: false,
        blocking: true,
        status: 'pending',
        requested_at: '2026-10-07T09:23:16Z',
        expires_at: '2026-10-07T09:33:16Z',
        ...overrides,
    };
}

function stubAnswer(response: () => Response = () => Response.json({ id: 'i-1', status: 'answered' })) {
    const fetchMock = vi.fn<typeof fetch>(async () => response());

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The `answer` of the one POST the card sent.
async function sentAnswer(fetchMock: ReturnType<typeof stubAnswer>) {
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));

    const [url, init] = fetchMock.mock.calls[0]!;

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
    });

    afterEach(() => {
        vi.unstubAllGlobals();
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
            missing: [{ key: 'template', label: 'Template', options: [{ value: '17', label: 'Kampagnen-Landingpage' }, { value: '21', label: 'Themenseite' }] }],
        }));

        expect(screen.getByText('Ich habe diese Einstellungen abgeleitet.')).toBeInTheDocument();
        expect(screen.getByText(/From your text · „auf Deutsch“/)).toBeInTheDocument();

        const confirm = screen.getByRole('button', { name: 'Confirm' });

        // The missing template has no value yet.
        expect(confirm).toBeDisabled();

        await user.click(screen.getByRole('combobox', { name: 'Template' }));
        await user.click(await screen.findByRole('option', { name: 'Kampagnen-Landingpage' }));
        await user.click(confirm);

        expect(await sentAnswer(fetchMock)).toEqual({
            settings: [
                { type: 'setting', key: 'folder', value: '42', label: 'Richtlinien' },
                { type: 'setting', key: 'language', value: 'de', label: 'Deutsch' },
                { type: 'setting', key: 'template', value: '17', label: 'Kampagnen-Landingpage' },
            ],
        });
    });

    it('answers a settings review with a corrected free-text value', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'settings_review', prompt: 'Passt das?', proposed: [{ key: 'filename', value: 'a.html', label: 'a.html', source: 'text' }] }));

        const field = screen.getByRole('textbox', { name: 'File name' });

        await user.clear(field);
        await user.type(field, 'terms.html');
        await user.click(screen.getByRole('button', { name: 'Confirm' }));

        expect(await sentAnswer(fetchMock)).toEqual({ settings: [{ type: 'setting', key: 'filename', value: 'terms.html', label: 'terms.html' }] });
    });

    it('answers a confirm with yes and the comment', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'confirm', prompt: 'Create the construct terms_change_table?' }));

        await user.type(screen.getByRole('textbox', { name: 'Comment' }), 'Go ahead');
        await user.click(screen.getByRole('button', { name: 'Yes' }));

        expect(await sentAnswer(fetchMock)).toEqual({ approved: true, comment: 'Go ahead' });
    });

    it('answers a confirm with no', async () => {
        const user = userEvent.setup();
        const fetchMock = stubAnswer();

        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

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

    it('shows an error when the answer is refused, for example an expired question', async () => {
        const user = userEvent.setup();

        stubAnswer(() => Response.json({ type: 't', title: 'Expired', status: 410, genaix_code: 'interaction_expired' }, { status: 410 }));
        renderCard(interaction({ kind: 'confirm', prompt: 'Publish now?' }));

        await user.click(screen.getByRole('button', { name: 'Yes' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'chat.interaction.answerFailed', detailKey: 'errors.genaix.interaction_expired' },
        ]));
        expect(screen.getByRole('button', { name: 'Yes' })).toBeEnabled();
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
