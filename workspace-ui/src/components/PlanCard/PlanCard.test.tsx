import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import type { Plan } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { createWrapper } from '@/test/renderWithProviders';

import { PlanCard } from './PlanCard';

// The CMS credential has tests of its own (`sessionAuthorization.test.ts`).
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    ensureSessionCmsAuthorization: vi.fn(),
}));

const PAGE = { type: 'page' as const, id: '9142', node_id: 3, label: 'Neue Nutzungsbedingungen ab Q3' };

const PLAN: Plan = {
    id: '5b0e7c2a-91d4-4f6b-8a3e-2c7d9f1e4a60',
    workflow: 'content_create',
    status: 'proposed',
    version: 2,
    summary: 'Create the page in Richtlinien.',
    items: [
        { id: 'p1', label: 'Create the page', kind: 'create', target: PAGE, status: 'pending' },
        { id: 'p2', label: 'Write the intro', kind: 'create', status: 'pending' },
    ],
};

// Answers every request with an accepted message and returns the mock, to read what was posted.
function stubFetch() {
    const fetchMock = vi.fn<typeof fetch>(async () => Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 }));

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

async function postedBody(fetchMock: ReturnType<typeof stubFetch>): Promise<unknown> {
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());

    const [input, init] = fetchMock.mock.calls[0]!;

    expect(String(input)).toBe('/rest/proxy/genaix/sessions/s-1/messages');

    return JSON.parse(String(init?.body));
}

describe('PlanCard', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('accepts the plan as proposed with a plan part naming the plan and its version', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={PLAN} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{ type: 'plan', plan: { plan_id: PLAN.id, based_on_version: 2 } }],
            options: { mode: 'execute' },
        });
    });

    it('accepts the adjusted plan with its steps in order, kept steps by their id, a new one without', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<PlanCard plan={PLAN} sessionId="s-1" canAnswer />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.clear(screen.getByRole('textbox', { name: 'Step 2' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 2' }), 'Write a short intro');
        await user.click(screen.getByRole('button', { name: 'Add a step' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 3' }), 'Add an FAQ block');
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{
                type: 'plan',
                plan: {
                    plan_id: PLAN.id,
                    based_on_version: 2,
                    items: [
                        { id: 'p1', label: 'Create the page', target: PAGE },
                        { id: 'p2', label: 'Write a short intro' },
                        { label: 'Add an FAQ block' },
                    ],
                },
            }],
            options: { mode: 'execute' },
        });
    });
});
