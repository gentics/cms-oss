import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import type { Check, CmsObjectRef, GenaixEvent, PlanState, PlanStatus, Problem, Publication } from '@/services/apiService/genaix/types';
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
        workflows: [{ id: 'content_create', version: '1.0.0' }],
        state: 'done',
        plan: { id: PLAN_ID, workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 2, summary: 'New terms page in Richtlinien.', items: [] },
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

// A page the plan wrote, as `PlanState.cms_objects` lists it.
function writtenPage(id: number, operation: CmsObjectRef['operation'] = 'created'): CmsObjectRef {
    return { type: 'page', id, node_id: 3, operation };
}

/**
 * Stubs the routes the panel calls: the transitions answer with `transition(body)` (default `200`
 * with the plan), `/workflows` with a module that keeps or restores on discard, messages with `202`,
 * the CMS publish permission of a page with `permissions[page id]`. Returns the bodies sent to the
 * transitions route and the CMS permission URLs asked.
 */
function stubRoutes({ transition, onDiscard = 'keep', permissions = {} }: {
    transition?: (body: Record<string, unknown>) => Response;
    onDiscard?: 'keep' | 'restore';
    permissions?: Record<number, boolean>;
} = {}) {
    const sent: Record<string, unknown>[] = [];
    const messages: unknown[] = [];
    const permissionUrls: string[] = [];

    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), 'http://localhost').pathname;
        const permission = /^\/rest\/perm\/publish\/page\/(\d+)$/.exec(url);

        if (permission) {
            permissionUrls.push(String(input));

            return Response.json({ granted: permissions[Number(permission[1])] ?? false });
        }

        if (url === TRANSITIONS) {
            const body = JSON.parse(String(init?.body)) as Record<string, unknown>;

            sent.push(body);

            return transition?.(body) ?? Response.json(planState('released').plan);
        }

        if (url.endsWith('/workflows')) {
            return Response.json({
                items: [
                    { id: 'content_create', version: '1.2.0', title: 'Create', description: '', roles: [], steps_template: [], plan_policy: { on_discard: onDiscard } },
                    { id: 'content_edit', version: '1.0.0', title: 'Edit', description: '', roles: [], steps_template: [], plan_policy: { on_discard: 'restore' } },
                ],
            });
        }

        if (url.endsWith(`/sessions/${SESSION}/messages`)) {
            messages.push(JSON.parse(String(init?.body)));

            return Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: SESSION }, { status: 202 });
        }

        return Response.json({ items: [], next_cursor: null });
    }));

    return { sent, messages, permissionUrls };
}

async function renderPanel(state: PlanState) {
    const result = renderWithProviders(<ReviewPanel sessionId={SESSION} state={state} />, { path: `/sessions/${SESSION}/review` });

    await screen.findByRole('heading', { name: 'Review' });

    return result;
}

/**
 * Renders the panel with `initial`; the returned `setState` replaces the plan's state, as the review page
 * does when it reads the plan again after a plan event.
 */
async function renderLivePanel(initial: PlanState) {
    let setState: (state: PlanState) => void = () => {};

    function LivePanel() {
        const [state, set] = useState(initial);

        setState = set;

        return <ReviewPanel sessionId={SESSION} state={state} />;
    }

    renderWithProviders(<LivePanel />, { path: `/sessions/${SESSION}/review` });
    await screen.findByRole('heading', { name: 'Review' });

    return { setState: (state: PlanState) => act(() => setState(state)) };
}

// A plan event of the session's stream, `seq` above the ones applied before.
function applyEvent(seq: number, type: string, payload: Record<string, unknown>) {
    const event = { seq, ts: '2026-10-14T09:00:00Z', session_id: SESSION, type, ...payload } as GenaixEvent;

    act(() => useWorkspaceEventStore.getState().applyEvent(SESSION, event));
}

// `publications.updated` of the plan: the publish outcome of each of its pages.
function announce(seq: number, publications: Publication[]) {
    applyEvent(seq, 'publications.updated', { plan_id: PLAN_ID, publications });
}

// The `202` of a transition that a run carries out (`publish`, `request_review`).
function accepted(runId: string) {
    return Response.json({ run_id: runId, plan: planState('released').plan, session: { id: SESSION } }, { status: 202 });
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
        // rc.5 removed `reopen`: a published plan is changed with a message (contract §4.4).
        ['released', ['Discard', 'Request review', 'Publish']],
        ['published', []],
        ['review_requested', []],
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
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));
        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'publish' }]));
    });

    describe('publish permission in the CMS', () => {
        it('offers no Publish when the user may not publish the plan\'s page, only a review request', async () => {
            const { permissionUrls } = stubRoutes({ permissions: { 9142: false } });

            await renderPanel(planState('released', { cms_objects: [writtenPage(9142)] }));

            await waitFor(() => expect(permissionUrls).toEqual(['/rest/perm/publish/page/9142?nodeId=3']));
            expect(actionNames()).toEqual(['Discard', 'Request review']);
        });

        it('offers no Publish when the user may publish only some of the plan\'s pages', async () => {
            const { permissionUrls } = stubRoutes({ permissions: { 9142: true, 9143: false } });

            await renderPanel(planState('released', { cms_objects: [writtenPage(9142), writtenPage(9143, 'updated')] }));

            await waitFor(() => expect(permissionUrls).toHaveLength(2));
            expect(actionNames()).toEqual(['Discard', 'Request review']);
        });

        it('offers Publish when the user may publish every page the plan wrote, and does not ask for pages it only read', async () => {
            const { permissionUrls } = stubRoutes({ permissions: { 9142: true, 9143: true } });

            await renderPanel(planState('released', { cms_objects: [writtenPage(9142), writtenPage(9143, 'published'), writtenPage(77, 'read')] }));

            expect(await screen.findByRole('button', { name: 'Publish' })).toBeInTheDocument();
            expect(permissionUrls).toEqual(['/rest/perm/publish/page/9142?nodeId=3', '/rest/perm/publish/page/9143?nodeId=3']);
        });
    });

    describe('publishing, confirmed in a dialog', () => {
        const pending: Publication = { cms_ref: { type: 'page', id: 9142, name: 'Neue Nutzungsbedingungen' }, status: 'pending', approval_chain: [] };

        it('asks first and sends nothing until confirmed; cancelling closes the dialog', async () => {
            const user = userEvent.setup();
            const { sent } = stubRoutes();

            await renderPanel(planState('released', { publications: [pending] }));
            announce(1, [pending]);
            await user.click(screen.getByRole('button', { name: 'Publish' }));

            const dialog = await screen.findByRole('dialog', { name: 'Publish this plan?' });

            expect(dialog).toHaveTextContent('1 page goes online.');
            expect(sent).toEqual([]);

            await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(sent).toEqual([]);
        });

        it('closes with Escape before it is confirmed', async () => {
            const user = userEvent.setup();

            stubRoutes();
            await renderPanel(planState('released'));
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await screen.findByRole('dialog', { name: 'Publish this plan?' });

            await user.keyboard('{Escape}');

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        });

        it('publishes once confirmed, shows that it is under way and cannot be closed meanwhile, then says what went out', async () => {
            const user = userEvent.setup();
            const { sent } = stubRoutes({ transition: () => accepted('r-9') });
            const { setState } = await renderLivePanel(planState('released', { publications: [pending] }));

            announce(1, [pending]);
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }]));
            const dialog = screen.getByRole('dialog');

            expect(within(dialog).getByRole('status')).toHaveTextContent('Publishing');
            expect(within(dialog).queryByRole('button')).not.toBeInTheDocument();

            await user.keyboard('{Escape}');
            expect(screen.getByRole('dialog')).toBeInTheDocument();

            announce(2, [{ ...pending, status: 'published' }]);
            await setState(planState('published', { publications: [{ ...pending, status: 'published' }] }));

            expect(await within(dialog).findByText('Published')).toBeInTheDocument();
            expect(dialog).toHaveTextContent('1 published');
            await user.click(within(dialog).getByRole('button', { name: 'Close' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        });

        it('counts pages sent to review as done, and says so', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => accepted('r-9') });
            const second: Publication = { ...pending, cms_ref: { type: 'page', id: 9143, name: 'Garantiebedingungen' } };
            const { setState } = await renderLivePanel(planState('released', { publications: [pending, second] }));

            announce(1, [pending, second]);
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));
            await screen.findByRole('status');

            announce(2, [{ ...pending, status: 'published' }, { ...second, status: 'review_requested' }]);
            await setState(planState('review_requested', { publications: [{ ...pending, status: 'published' }, { ...second, status: 'review_requested' }] }));

            const dialog = screen.getByRole('dialog');

            expect(await within(dialog).findByText('Done')).toBeInTheDocument();
            expect(dialog).toHaveTextContent('1 published, 1 in review');
        });

        it('shows why publishing failed when its run fails, and can then be closed', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => accepted('r-9') });
            await renderPanel(planState('released', { publications: [pending] }));
            announce(1, [pending]);

            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));
            await screen.findByRole('status');

            const error = { type: 't', title: 'Locked', status: 423, genaix_code: 'cms_object_locked' as const, detail: 'The page is locked by A. Kern.' };

            // The plan stays released: only the failed page and the failed run tell (contract).
            announce(2, [{ ...pending, status: 'failed', error }]);
            applyEvent(3, 'run.failed', { run_id: 'r-9', run: { id: 'r-9', status: 'failed' }, error });

            const dialog = screen.getByRole('dialog');

            expect(await within(dialog).findByText('Publishing failed')).toBeInTheDocument();
            expect(dialog).toHaveTextContent('Neue Nutzungsbedingungen');
            expect(dialog).toHaveTextContent('The page is locked by A. Kern.');
            await user.click(within(dialog).getByRole('button', { name: 'Close' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        });

        it('shows a refused request in the dialog, beside the notification every failed transition gets', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => problem('plan_closed') });
            await renderPanel(planState('released'));
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));

            const dialog = screen.getByRole('dialog');

            expect(await within(dialog).findByText('Publishing failed')).toBeInTheDocument();
            expect(dialog).toHaveTextContent('This plan is closed');
            expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'planBar.transitionFailed', detailKey: 'errors.genaix.plan_closed' },
            ]);
        });

        // Refuses `publish` with a warning until it is acknowledged; `release` is refused the same way.
        function refuseUntilAcknowledged() {
            return stubRoutes({
                transition: (body) => (body.acknowledge_checks
                    ? accepted('r-9')
                    : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] })),
            });
        }

        // Publish, confirmed, and refused by the gates: the dialog closes and the plan head says so.
        async function publishRefused(user: ReturnType<typeof userEvent.setup>) {
            await renderPanel(planState('released'));
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(await screen.findByText('Some checks have warnings. Confirm them to go on.')).toBeInTheDocument();
        }

        it('leaves a refusal by the quality gates to the panel, and asks again before publishing anyway', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await publishRefused(user);
            await user.click(await screen.findByRole('button', { name: 'Publish anyway' }));

            const dialog = await screen.findByRole('dialog', { name: 'Publish this plan?' });

            expect(sent).toEqual([{ action: 'publish' }]);
            await user.click(within(dialog).getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }, { action: 'publish', acknowledge_checks: ['chk-alt-text'] }]));
            expect(within(screen.getByRole('dialog')).getByRole('status')).toHaveTextContent('Publishing');
        });

        it('asks again before "Check again" after a refused publish, which publishes if the checks pass now', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await publishRefused(user);
            await user.click(screen.getByRole('button', { name: 'Check again' }));

            const dialog = await screen.findByRole('dialog', { name: 'Publish this plan?' });

            expect(sent).toEqual([{ action: 'publish' }]);
            await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(sent).toEqual([{ action: 'publish' }]);

            await user.click(screen.getByRole('button', { name: 'Check again' }));
            await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }, { action: 'publish' }]));
        });

        it('does not ask before "Check again" after a refused release, which puts nothing online', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await renderPanel(planState('done'));
            await user.click(screen.getByRole('button', { name: 'Release' }));
            await user.click(await screen.findByRole('button', { name: 'Check again' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release' }]));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        });
    });

    it('requests a review of a released plan', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes();

        await renderPanel(planState('released'));
        await user.click(screen.getByRole('button', { name: 'Request review' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'request_review' }]));
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

    it('takes the strictest discard policy of the plan\'s modules (rc.5)', async () => {
        const user = userEvent.setup();
        const state = planState('done');

        stubRoutes();
        await renderPanel({ ...state, plan: { ...state.plan, workflows: [{ id: 'content_create', version: '1.2.0' }, { id: 'content_edit', version: '1.0.0' }] } });
        await user.click(screen.getByRole('button', { name: 'Discard' }));

        expect(await screen.findByText('The plan is closed and the pages it changed go back to their version from before it.')).toBeInTheDocument();
    });

    it('offers no reopen on a published plan (rc.5: a revision replaces it, from the chat)', async () => {
        stubRoutes();

        await renderPanel(planState('published', { publications: [page] }));
        expect(screen.getByRole('contentinfo')).toHaveTextContent('Published');
        expect(screen.queryByRole('button', { name: 'Reopen' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Ask for a change' })).not.toBeInTheDocument();
    });

    it('shows a discarded plan read-only: its status, no actions and no fixes', async () => {
        stubRoutes();
        await renderPanel(planState('discarded'));

        expect(screen.getByRole('contentinfo')).toHaveTextContent('Discarded');
        expect(actionNames()).toEqual([]);
        expect(screen.queryByRole('button', { name: 'Add alt text' })).not.toBeInTheDocument();
    });

    it('offers to check again or release anyway when the gates refuse a release with warnings only', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({
            transition: (body) => (body.acknowledge_checks
                ? Response.json(planState('released').plan)
                : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] })),
        });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        // In the panel's foot, not in a dialog; the failing checks are in the checklist above.
        expect(await screen.findByText('Some checks have warnings. Confirm them to go on.')).toBeInTheDocument();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(actionNames()).toEqual(['Discard', 'Check again', 'Release anyway']);

        await user.click(screen.getByRole('button', { name: 'Release anyway' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }]));
        await waitFor(() => expect(screen.queryByText('Some checks have warnings. Confirm them to go on.')).not.toBeInTheDocument());
    });

    it('offers no way past a blocking check, only to check again', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({ transition: () => problem('quality_gate_failed', { failing_checks: [brokenLink, altText], failing_gates: [{ gate_id: 'links', label: 'Links', kind: 'script', blocking: true, when: 'before_release', status: 'failed', attempt: 1, check_ids: ['chk-links'] }], acknowledgeable_checks: ['chk-alt-text'] }) });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        expect(await screen.findByText('Fix the blocking checks first, or ask the agent to. Then check again.')).toBeInTheDocument();
        expect(actionNames()).toEqual(['Discard', 'Check again']);

        await user.click(screen.getByRole('button', { name: 'Check again' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release' }]));
    });

    it('reports any other refusal as an error, e.g. a closed plan', async () => {
        const user = userEvent.setup();

        stubRoutes({ transition: () => problem('plan_closed') });
        await renderPanel(planState('released'));
        // Not Publish, whose refusals its dialog shows ('publishing, confirmed in a dialog').
        await user.click(screen.getByRole('button', { name: 'Request review' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'planBar.transitionFailed', detailKey: 'errors.genaix.plan_closed' },
        ]));
    });
});
