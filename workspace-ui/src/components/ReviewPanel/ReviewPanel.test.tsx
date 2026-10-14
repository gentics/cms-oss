import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import type { Check, PlanState, PlanStatus, Problem, Publication } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { ReviewPanel } from './ReviewPanel';

// The CMS credential has tests of its own (`sessionAuthorization.test.ts`).
vi.mock('@/helper/sessionAuthorization/sessionAuthorization', () => ({
    ensureSessionCmsAuthorization: vi.fn(),
}));

const SESSION = 's-1';
const PLAN_ID = 'pl-1';
const TRANSITIONS = `/rest/proxy/genaix/sessions/${SESSION}/plans/${PLAN_ID}/transitions`;

const altText: Check = {
    id: 'chk-alt-text',
    label: 'Images have alt text',
    severity: 'warn',
    blocking: false,
    message: 'The hero image has no alt text.',
    source: { type: 'gate', label: 'Accessibility' },
    suggested_actions: [{ label: 'Add alt text', prompt: 'Add an alt text to the hero image.' }],
};

const tone: Check = {
    id: 'chk-tone',
    label: 'Formal address',
    severity: 'pass',
    blocking: false,
    message: 'The page addresses readers formally.',
    source: { type: 'guideline', label: 'Tone & Address' },
};

const brokenLink: Check = {
    id: 'chk-links',
    label: 'Links resolve',
    severity: 'fail',
    blocking: true,
    message: 'One link points to a deleted page.',
    source: { type: 'gate', label: 'Links' },
};

const page: Publication = {
    cms_ref: { type: 'page', id: 9142, name: 'Neue Nutzungsbedingungen' },
    status: 'published',
    approval_chain: [{ id: 'ap-1', label: 'Author releases the draft', actor: { type: 'user' }, status: 'done' }],
};

function planState(status: PlanStatus, extra: Partial<PlanState> = {}): PlanState {
    return {
        workflow: 'content_create',
        state: 'done',
        plan: { id: PLAN_ID, workflow: 'content_create', status, version: 2, summary: 'New terms page in Richtlinien.', items: [] },
        steps: [],
        checks: [tone, altText],
        gates: [],
        approval_chain: [
            { id: 'ap-1', label: 'Author releases the draft', actor: { type: 'user' }, status: 'pending' },
            { id: 'ap-2', label: 'Group Marketing has publish permission on Richtlinien', actor: { type: 'group', id: '27', label: 'Marketing' }, status: 'not_required' },
        ],
        cms_objects: [],
        publications: [],
        ...extra,
    };
}

function problem(genaixCode: string, extra: Partial<Problem> = {}) {
    return Response.json({ type: 't', title: 't', status: 409, genaix_code: genaixCode, ...extra }, { status: 409 });
}

/**
 * Stubs the routes the panel calls: the transitions answer with `transition(body)` (default `200`
 * with the plan), `/workflows` with a module that keeps or restores on discard, messages with `202`.
 * Returns the bodies sent to the transitions route.
 */
function stubRoutes({ transition, onDiscard = 'keep' }: { transition?: (body: Record<string, unknown>) => Response; onDiscard?: 'keep' | 'restore' } = {}) {
    const sent: Record<string, unknown>[] = [];
    const messages: unknown[] = [];

    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://localhost').pathname;

        if (url === TRANSITIONS) {
            const body = JSON.parse(String(init?.body)) as Record<string, unknown>;

            sent.push(body);

            return transition?.(body) ?? Response.json(planState('released').plan);
        }

        if (url.endsWith('/workflows')) {
            return Response.json({ items: [{ id: 'content_create', version: '1.2.0', title: 'Create', description: '', roles: [], steps_template: [], plan_policy: { concurrency: 'exclusive', on_discard: onDiscard } }] });
        }

        if (url.endsWith(`/sessions/${SESSION}/messages`)) {
            messages.push(JSON.parse(String(init?.body)));

            return Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: SESSION }, { status: 202 });
        }

        return Response.json({ items: [], next_cursor: null });
    }));

    return { sent, messages };
}

async function renderPanel(state: PlanState) {
    const result = renderWithProviders(<ReviewPanel sessionId={SESSION} state={state} />, { path: `/sessions/${SESSION}/review` });

    await screen.findByRole('heading', { name: 'Review' });

    return result;
}

function actionNames() {
    return within(screen.getByRole('contentinfo')).queryAllByRole('button').map((button) => button.textContent);
}

describe('ReviewPanel', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useErrorNotificationStore.setState({ errors: [] });
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('shows the checklist grouped by source, each check with its state in words', async () => {
        stubRoutes();
        await renderPanel(planState('done'));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        expect(within(checklist).getByRole('group', { name: 'Tone & Address' })).toHaveTextContent('Formal addressPassed');
        expect(within(checklist).getByRole('group', { name: 'Accessibility' })).toHaveTextContent('Images have alt textWarning');
        expect(within(checklist).getByText('The hero image has no alt text.')).toBeInTheDocument();
    });

    it('sends a suggested fix to the agent and goes back to the chat', async () => {
        const user = userEvent.setup();
        const { messages } = stubRoutes();
        const { router } = await renderPanel(planState('done'));

        await user.click(screen.getByRole('button', { name: 'Add alt text' }));

        await waitFor(() => expect(messages).toEqual([{ parts: [{ type: 'text', text: 'Add an alt text to the hero image.' }] }]));
        expect(router.state.location.pathname).toBe(`/sessions/${SESSION}`);
    });

    it.each<[PlanStatus, string[]]>([
        ['accepted', ['Release']],
        ['running', ['Release']],
        ['done', ['Discard', 'Release']],
        ['released', ['Discard', 'Reopen', 'Request review', 'Publish']],
        ['published', ['Reopen']],
        ['review_requested', ['Reopen']],
        ['discarded', []],
        ['superseded', []],
    ])('offers in status %s: %j', async (status, actions) => {
        stubRoutes();
        await renderPanel(planState(status));

        expect(actionNames()).toEqual(actions);
    });

    it('releases a done plan, then publishes the released one: two steps', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes();
        const { unmount } = await renderPanel(planState('done'));

        await user.click(screen.getByRole('button', { name: 'Release' }));
        await waitFor(() => expect(sent).toEqual([{ action: 'release' }]));
        unmount();

        await renderPanel(planState('released'));
        await user.click(screen.getByRole('button', { name: 'Publish' }));
        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'publish' }]));
    });

    it('requests a review of a released plan', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes();

        await renderPanel(planState('released'));
        await user.click(screen.getByRole('button', { name: 'Request review' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'request_review' }]));
    });

    it('shows the labels of the approval chain', async () => {
        stubRoutes();
        await renderPanel(planState('released'));

        const approval = screen.getByRole('region', { name: 'Approval' });

        expect(approval).toHaveTextContent('Author releases the draftPending');
        expect(approval).toHaveTextContent('Group Marketing has publish permission on RichtlinienNot required');
    });

    it('shows the outcome per page, with its approval labels and the error of a failed one', async () => {
        stubRoutes();
        await renderPanel(planState('released', {
            publications: [
                page,
                {
                    cms_ref: { type: 'page', id: 8871, name: 'Garantiebedingungen' },
                    status: 'failed',
                    approval_chain: [],
                    error: { type: 't', title: 'Locked', status: 423, genaix_code: 'cms_object_locked', detail: 'The page is locked by A. Kern.' },
                },
            ],
        }));

        // One entry per page: the direct items of the list (their approval steps are a list of their own).
        const [published, failed] = [...within(screen.getByRole('region', { name: 'Pages' })).getAllByRole('list')[0]!.children];

        expect(published).toHaveTextContent('Neue NutzungsbedingungenPublished');
        expect(published).toHaveTextContent('Author releases the draftDone');
        expect(failed).toHaveTextContent('GarantiebedingungenFailed');
        expect(failed).toHaveTextContent('The page is locked by A. Kern.');
        // A failed page is published again with the same action.
        expect(screen.getByRole('button', { name: 'Publish' })).toBeInTheDocument();
    });

    it('asks before discarding, and says that kept pages stay as drafts', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes();

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Discard' }));

        const dialog = await screen.findByRole('dialog', { name: 'Discard this plan?' });

        await waitFor(() => expect(dialog).toHaveTextContent('Pages it already changed stay as drafts in the CMS.'));
        expect(sent).toEqual([]);

        await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
        expect(sent).toEqual([]);

        await user.click(screen.getByRole('button', { name: 'Discard' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Discard' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'discard' }]));
    });

    it('says that a module which restores takes the pages back', async () => {
        const user = userEvent.setup();

        stubRoutes({ onDiscard: 'restore' });
        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Discard' }));

        expect(await screen.findByText('The plan is closed and the pages it changed go back to their version from before it.')).toBeInTheDocument();
    });

    it('reopens a published plan', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes();

        await renderPanel(planState('published', { publications: [page] }));
        expect(screen.getByRole('contentinfo')).toHaveTextContent('Published');
        await user.click(screen.getByRole('button', { name: 'Reopen' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'reopen' }]));
    });

    it('shows a discarded plan read-only: its status, no actions and no fixes', async () => {
        stubRoutes();
        await renderPanel(planState('discarded'));

        expect(screen.getByRole('contentinfo')).toHaveTextContent('Discarded');
        expect(actionNames()).toEqual([]);
        expect(screen.queryByRole('button', { name: 'Add alt text' })).not.toBeInTheDocument();
    });

    it('lists what a refused release failed on, and releases anyway with the warnings acknowledged', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({
            transition: (body) => (body.acknowledge_checks
                ? Response.json(planState('released').plan)
                : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] })),
        });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        // In the panel itself, not in a dialog.
        const refusal = await within(screen.getByRole('region', { name: 'Review' })).findByRole('region', { name: 'Checks failed' });

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(refusal).toHaveTextContent('Images have alt textWarning');
        await user.click(within(refusal).getByRole('button', { name: 'Release anyway' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }]));
        await waitFor(() => expect(screen.queryByRole('region', { name: 'Checks failed' })).not.toBeInTheDocument());
    });

    it('offers no way past a blocking check, only to close', async () => {
        const user = userEvent.setup();

        stubRoutes({ transition: () => problem('quality_gate_failed', { failing_checks: [brokenLink, altText], failing_gates: [{ gate_id: 'links', label: 'Links', kind: 'script', blocking: true, when: 'before_release', status: 'failed', attempt: 1, check_ids: ['chk-links'] }], acknowledgeable_checks: ['chk-alt-text'] }) });
        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        const refusal = await within(screen.getByRole('region', { name: 'Review' })).findByRole('region', { name: 'Checks failed' });

        expect(refusal).toHaveTextContent('Links resolveBlocking');
        expect(within(refusal).queryByRole('button', { name: 'Release anyway' })).not.toBeInTheDocument();
        expect(within(refusal).getAllByRole('button', { name: 'Close' }).length).toBeGreaterThan(0);

        await user.click(within(refusal).getByRole('button', { name: 'Close' }));

        expect(screen.queryByRole('region', { name: 'Checks failed' })).not.toBeInTheDocument();
    });

    it('reports any other refusal as an error, e.g. a closed plan', async () => {
        const user = userEvent.setup();

        stubRoutes({ transition: () => problem('plan_closed') });
        await renderPanel(planState('released'));
        await user.click(screen.getByRole('button', { name: 'Publish' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'planBar.transitionFailed', detailKey: 'errors.genaix.plan_closed' },
        ]));
    });
});
