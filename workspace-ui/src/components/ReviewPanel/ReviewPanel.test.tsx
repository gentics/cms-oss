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
    gate_id: 'alt_text',
    kind: 'script',
    suggested_actions: [{ label: 'Add alt text', prompt: 'Add an alt text to the hero image.' }],
};

const tone: Check = {
    id: 'chk-tone',
    label: 'Formal address',
    severity: 'pass',
    blocking: false,
    message: 'The page addresses readers formally.',
    source: { type: 'guideline', label: 'Tone & Address' },
    gate_id: 'guideline_compliance',
    kind: 'subagent',
};

// The agent's own finding (`report_check`), no gate's: it never holds up a transition.
const selfReport: Check = {
    id: 'content_create/chk-cross-link-online-page',
    label: 'Cross-link to an online page',
    severity: 'warn',
    blocking: false,
    message: 'The new page links to a page that is online.',
    source: { type: 'system', label: 'Agent' },
    kind: 'prompt',
};

const brokenLink: Check = {
    id: 'chk-links',
    label: 'Links resolve',
    severity: 'fail',
    blocking: true,
    message: 'One link points to a deleted page.',
    source: { type: 'gate', label: 'Links' },
    gate_id: 'references_exist',
    kind: 'script',
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
    transition?: (body: Record<string, unknown>) => Response | Promise<Response>;
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

    const { router } = renderWithProviders(<LivePanel />, { path: `/sessions/${SESSION}/review` });

    await screen.findByRole('heading', { name: 'Review' });

    return { router, setState: (state: PlanState) => act(() => setState(state)) };
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
        const user = userEvent.setup();

        stubRoutes();
        await renderPanel(planState('done'));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        await user.click(within(checklist).getByRole('button', { name: '1 check passed' }));
        expect(within(checklist).getByRole('group', { name: 'Tone & Address' })).toHaveTextContent('Formal addressPassed');
        expect(within(checklist).getByRole('group', { name: 'Accessibility' })).toHaveTextContent('Images have alt textWarning');
        expect(within(checklist).getByText('The hero image has no alt text.')).toBeInTheDocument();
    });

    it('says what a failing check found in one sentence, its gate\'s summary, not every finding', async () => {
        const selfCheck: Check = {
            ...altText,
            id: 'content_create/chk-release-self-check',
            label: 'Agent self-check before release',
            message: 'Page 42 is an offline draft. The alt text "test" is a placeholder.\ninfo: I read page 42 back from the CMS.\nwarn: The alt text \'test\' is not descriptive.',
            source: { type: 'gate', id: 'release_self_check', label: 'Agent self-check before release' },
            gate_id: 'release_self_check',
            kind: 'prompt',
            evidence: { gate_id: 'release_self_check', kind: 'prompt', verdict: 'warn', summary: 'Page 42 is an offline draft. The alt text "test" is a placeholder.', finding_count: 2 },
            suggested_actions: undefined,
        };

        stubRoutes();
        await renderPanel(planState('done', { checks: [selfCheck] }));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        expect(within(checklist).getByText('Page 42 is an offline draft. The alt text "test" is a placeholder.')).toBeInTheDocument();
        expect(within(checklist).queryByText(/I read page 42 back/)).not.toBeInTheDocument();
    });

    it('folds the checks that passed into one line, closed until opened', async () => {
        const user = userEvent.setup();
        const short: Check = { ...tone, id: 'chk-short', label: 'Short sentences' };

        stubRoutes();
        await renderPanel(planState('done', { checks: [tone, altText, short] }));

        const checklist = screen.getByRole('region', { name: 'Checklist' });
        const toggle = within(checklist).getByRole('button', { name: '2 checks passed' });

        // The warning stays in view; the passed ones are folded.
        expect(toggle).toHaveAttribute('aria-expanded', 'false');
        expect(within(checklist).getByText('Images have alt text')).toBeInTheDocument();
        expect(within(checklist).queryByText('Formal address')).not.toBeInTheDocument();

        await user.click(toggle);

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect(within(checklist).getByRole('group', { name: 'Tone & Address' })).toHaveTextContent('Formal addressPassedShort sentencesPassed');

        await user.click(toggle);

        expect(within(checklist).queryByText('Formal address')).not.toBeInTheDocument();
    });

    it('has no line for passed checks when none passed', async () => {
        stubRoutes();
        await renderPanel(planState('done', { checks: [altText] }));

        expect(within(screen.getByRole('region', { name: 'Checklist' })).queryByRole('button', { name: /passed/ })).not.toBeInTheDocument();
    });

    it('leaves the agent\'s own findings out of the checklist, showing only the gates\' checks', async () => {
        stubRoutes();
        await renderPanel(planState('done', { checks: [tone, altText, selfReport] }));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        expect(within(checklist).queryByRole('group', { name: 'Agent' })).not.toBeInTheDocument();
        expect(within(checklist).queryByText('Cross-link to an online page')).not.toBeInTheDocument();
        expect(within(checklist).getByText('Images have alt text')).toBeInTheDocument();
    });

    it('sends a suggested fix to the agent as a revision of this plan and goes back to the chat', async () => {
        const user = userEvent.setup();
        const { messages } = stubRoutes();
        const { router } = await renderPanel(planState('released'));

        await user.click(screen.getByRole('button', { name: 'Add alt text' }));

        await waitFor(() => expect(messages).toEqual([{
            parts: [{ type: 'text', text: 'Add an alt text to the hero image.' }],
            options: { mode: 'revise', plan_id: PLAN_ID },
        }]));
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

    describe('while the quality gates check the plan', () => {
        // Holds the transitions until `answer` is called with the response.
        function holdTransitions() {
            let resolveTransition: (response: Response) => void = () => {};

            stubRoutes({ transition: () => new Promise<Response>((resolve) => {
                resolveTransition = resolve;
            }) });

            return {
                answer: (response: Response) => {
                    act(() => resolveTransition(response));
                },
            };
        }

        it('blurs the checklist and shows the gates at work until they decided, then that all passed', async () => {
            const user = userEvent.setup();
            const { answer } = holdTransitions();
            const { setState } = await renderLivePanel(planState('done', { checks: [tone] }));

            await user.click(screen.getByRole('button', { name: 'Release' }));

            expect(await screen.findByText('The quality gates are checking the plan…')).toBeInTheDocument();
            expect(screen.getByRole('region', { name: 'Checklist' }).closest('[aria-busy="true"]')).not.toBeNull();

            answer(Response.json(planState('released').plan));
            setState(planState('released', { checks: [tone] }));

            await waitFor(() => expect(screen.queryByText('The quality gates are checking the plan…')).not.toBeInTheDocument());
            expect(screen.getByRole('region', { name: 'Checklist' }).closest('[aria-busy="true"]')).toBeNull();
            expect(within(screen.getByRole('region', { name: 'Checklist' })).getByRole('status')).toHaveTextContent('All checks passed. The plan is released.');
            expect(actionNames()).toEqual(['Discard', 'Request review', 'Publish']);
        });

        it('shows the gates at work on "Publish anyway" too: the release first, then publishing over the whole page', async () => {
            const user = userEvent.setup();
            let isHeld = false;
            let answer: (response: Response) => void = () => {};
            const { sent } = stubRoutes({
                transition: (body) => {
                    if (body.action === 'publish') {
                        return accepted('r-9');
                    }

                    if (!body.acknowledge_checks) {
                        return problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] });
                    }

                    isHeld = true;

                    return new Promise<Response>((resolve) => {
                        answer = resolve;
                    });
                },
            });

            await renderPanel(planState('done'));
            await user.click(screen.getByRole('button', { name: 'Release' }));
            await user.click(await screen.findByRole('button', { name: 'Publish anyway' }));

            await waitFor(() => expect(isHeld).toBe(true));
            expect(screen.getByText('The quality gates are checking the plan…')).toBeInTheDocument();
            act(() => answer(Response.json(planState('released').plan)));

            await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }, { action: 'publish' }]));
            expect(await screen.findByText('Publishing…')).toBeInTheDocument();
        });
    });

    it('says a released plan passed every check', async () => {
        stubRoutes();
        await renderPanel(planState('released', { checks: [tone] }));

        expect(within(screen.getByRole('region', { name: 'Checklist' })).getByRole('status')).toHaveTextContent('All checks passed. The plan is released.');
    });

    it('says a released plan was released with the warnings the user acknowledged', async () => {
        stubRoutes();
        await renderPanel(planState('released', { checks: [tone, { ...altText, acknowledged_by: 'sub-1', acknowledged_at: '2026-10-14T09:00:00Z' }] }));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        expect(within(checklist).getByRole('status')).toHaveTextContent('Released with 1 warning.');
        expect(within(checklist).getByRole('group', { name: 'Accessibility' })).toHaveTextContent('Images have alt textWarning · acknowledged');
    });

    it('says nothing about the gates before they ran', async () => {
        stubRoutes();
        await renderPanel(planState('done'));

        expect(within(screen.getByRole('region', { name: 'Checklist' })).queryByRole('status')).not.toBeInTheDocument();
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

    describe('publishing, over the whole page', () => {
        const pending: Publication = { cms_ref: { type: 'page', id: 9142, name: 'Neue Nutzungsbedingungen' }, status: 'pending', approval_chain: [] };

        it('publishes at once, covers the page while it is under way, then opens the page that says what went out', async () => {
            const user = userEvent.setup();
            const { sent } = stubRoutes({ transition: () => accepted('r-9') });
            const { setState, router } = await renderLivePanel(planState('released', { publications: [pending] }));

            announce(1, [pending]);
            await user.click(screen.getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }]));
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
            expect(await screen.findByText('Publishing…')).toBeInTheDocument();
            expect(screen.getByText('1 page goes online.')).toBeInTheDocument();

            announce(2, [{ ...pending, status: 'published' }]);
            setState(planState('published', { publications: [{ ...pending, status: 'published' }] }));

            await waitFor(() => expect(router.state.location.pathname).toBe(`/sessions/${SESSION}/published`));
            expect(router.state.location.search).toEqual({ plan: PLAN_ID });
            expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
        });

        it('opens that page too when a page went to review', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => accepted('r-9') });
            const { setState, router } = await renderLivePanel(planState('released', { publications: [pending] }));

            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await screen.findByText('Publishing…');
            setState(planState('review_requested', { publications: [{ ...pending, status: 'review_requested' }] }));

            await waitFor(() => expect(router.state.location.pathname).toBe(`/sessions/${SESSION}/published`));
        });

        it('takes the overlay away when its run fails, says why, and shows the failed page in the panel', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => accepted('r-9') });
            const { setState, router } = await renderLivePanel(planState('released', { publications: [pending] }));

            announce(1, [pending]);
            await user.click(screen.getByRole('button', { name: 'Publish' }));
            await screen.findByText('Publishing…');

            const error = { type: 't', title: 'Locked', status: 423, genaix_code: 'cms_object_locked' as const, detail: 'The page is locked by A. Kern.' };
            const failed: Publication = { ...pending, status: 'failed', error };

            // The plan stays released: only the failed page and the failed run tell (contract).
            announce(2, [failed]);
            applyEvent(3, 'run.failed', { run_id: 'r-9', run: { id: 'r-9', status: 'failed' }, error });
            setState(planState('released', { publications: [failed] }));

            await waitFor(() => expect(screen.queryByText('Publishing…')).not.toBeInTheDocument());
            expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'planBar.publishing.failed', detailKey: 'errors.genaix.cms_object_locked' },
            ]);
            expect(screen.getByRole('region', { name: 'Neue Nutzungsbedingungen' })).toHaveTextContent('The page is locked by A. Kern.');
            expect(router.state.location.pathname).toBe(`/sessions/${SESSION}/review`);
        });

        it('puts nothing over the page when the request is refused, and notifies like every failed transition', async () => {
            const user = userEvent.setup();

            stubRoutes({ transition: () => problem('plan_closed') });
            await renderPanel(planState('released'));
            await user.click(screen.getByRole('button', { name: 'Publish' }));

            await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'planBar.transitionFailed', detailKey: 'errors.genaix.plan_closed' },
            ]));
            expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
        });

        // Refuses `publish` with a warning until it is acknowledged; `release` is refused the same way.
        function refuseUntilAcknowledged() {
            return stubRoutes({
                transition: (body) => (body.acknowledge_checks
                    ? accepted('r-9')
                    : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] })),
            });
        }

        // Publish, refused by the gates: the checklist says so.
        async function publishRefused(user: ReturnType<typeof userEvent.setup>) {
            await renderPanel(planState('released'));
            await user.click(screen.getByRole('button', { name: 'Publish' }));

            expect(await screen.findByText('Not published: 1 check has a warning.')).toBeInTheDocument();
            expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
        }

        it('publishes anyway at once, acknowledging the warnings, over the whole page', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await publishRefused(user);
            await user.click(await screen.findByRole('button', { name: 'Publish anyway' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }, { action: 'publish', acknowledge_checks: ['chk-alt-text'] }]));
            expect(await screen.findByText('Publishing…')).toBeInTheDocument();
        });

        it('publishes again on "Check again" after a refused publish, which goes out if the checks pass now', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await publishRefused(user);
            await user.click(screen.getByRole('button', { name: 'Check again' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'publish' }, { action: 'publish' }]));
        });

        it('checks again on "Check again" after a refused release, which puts nothing online', async () => {
            const user = userEvent.setup();
            const { sent } = refuseUntilAcknowledged();

            await renderPanel(planState('done'));
            await user.click(screen.getByRole('button', { name: 'Release' }));
            await user.click(await screen.findByRole('button', { name: 'Check again' }));

            await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release' }]));
            expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
        });
    });

    describe('a proposed revision: Accept & review', () => {
        // A revision of a plan accepted at version 2, proposed at version 3 (rc.5).
        function revision(status: PlanStatus = 'proposed', extra: Partial<PlanState> = {}): PlanState {
            const state = planState(status, extra);

            return { ...state, plan: { ...state.plan, version: 3, accepted_version: 2 } };
        }

        it('accepts the revision as proposed, shows the work under way, and releases it once done', async () => {
            const user = userEvent.setup();
            const { sent, messages } = stubRoutes();
            const { setState } = await renderLivePanel(revision());

            expect(actionNames()).toEqual(['Discard', 'Accept & review']);
            await user.click(screen.getByRole('button', { name: 'Accept & review' }));

            await waitFor(() => expect(messages).toEqual([{
                parts: [{ type: 'plan', plan: { plan_id: PLAN_ID, based_on_version: 3 } }],
                options: { mode: 'execute' },
            }]));
            expect(await screen.findByText('The agent carries out the changes…')).toBeInTheDocument();
            expect(screen.getByRole('region', { name: 'Checklist' }).closest('[aria-busy="true"]')).not.toBeNull();

            setState(revision('running'));
            expect(screen.getByText('The agent carries out the changes…')).toBeInTheDocument();
            expect(sent).toEqual([]);

            setState(revision('done'));

            await waitFor(() => expect(sent).toEqual([{ action: 'release' }]));
            expect(screen.queryByText('The agent carries out the changes…')).not.toBeInTheDocument();
        });

        it('shows the question of the plan\'s run, and the way to the chat where it is answered', async () => {
            const user = userEvent.setup();

            stubRoutes();
            const { setState } = await renderLivePanel(revision());

            await user.click(screen.getByRole('button', { name: 'Accept & review' }));
            setState(revision('running'));
            applyEvent(1, 'interaction.requested', {
                run_id: 'r-1',
                plan_id: PLAN_ID,
                interaction: { id: 'i-1', kind: 'ask_user', prompt: 'A second link?', multi: false, other_allowed: false, blocking: true, status: 'pending', requested_at: '2026-10-14T09:00:00Z', expires_at: '2099-01-01T00:00:00Z', run_id: 'r-1' },
            });

            expect(await screen.findByText('The agent has a question about the changes.')).toBeInTheDocument();
            expect(screen.getByRole('link', { name: 'Answer in the chat' })).toHaveAttribute('href', `/sessions/${SESSION}`);
        });

        it('says so when the run that carries it out fails, and releases nothing', async () => {
            const user = userEvent.setup();
            const { sent } = stubRoutes();
            const { setState } = await renderLivePanel(revision());

            await user.click(screen.getByRole('button', { name: 'Accept & review' }));
            await screen.findByText('The agent carries out the changes…');
            setState(revision('accepted'));

            const error = { type: 't', title: 'Locked', status: 423, genaix_code: 'cms_object_locked' as const };

            // The plan stays accepted after a failed execute run (contract §4.1).
            applyEvent(1, 'run.failed', { run_id: 'r-1', run: { id: 'r-1', status: 'failed' }, error });

            await waitFor(() => expect(screen.queryByText('The agent carries out the changes…')).not.toBeInTheDocument());
            expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'review.acceptFailed', detailKey: 'errors.genaix.cms_object_locked' },
            ]);
            expect(sent).toEqual([]);
        });

        it('offers no Accept & review once the plan is not proposed', async () => {
            stubRoutes();
            await renderPanel(revision('done'));

            expect(screen.queryByRole('button', { name: 'Accept & review' })).not.toBeInTheDocument();
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

    it('offers to check again or publish anyway when the gates refuse a release with warnings only', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({
            transition: (body) => {
                if (body.action === 'publish') {
                    return accepted('r-9');
                }

                return body.acknowledge_checks
                    ? Response.json(planState('released').plan)
                    : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] });
            },
        });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        // At the top of the checklist, with the failing checks; not in a dialog.
        expect(await within(screen.getByRole('region', { name: 'Checklist' })).findByText('Not released: 1 check has a warning.')).toBeInTheDocument();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(actionNames()).toEqual(['Discard', 'Check again', 'Publish anyway']);

        await user.click(screen.getByRole('button', { name: 'Publish anyway' }));

        // The release with the warnings acknowledged, then at once the publish.
        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }, { action: 'publish' }]));
        await waitFor(() => expect(screen.queryByText('Not released: 1 check has a warning.')).not.toBeInTheDocument());
    });

    it('does not publish when the release with the warnings acknowledged is refused again', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({
            transition: () => problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] }),
        });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));
        await user.click(await screen.findByRole('button', { name: 'Publish anyway' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }]));
        expect(await screen.findByText('Not released: 1 check has a warning.')).toBeInTheDocument();
        expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
    });

    it('offers Release anyway instead to a user who may not publish the plan\'s pages', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({
            permissions: { 9142: false },
            transition: (body) => (body.acknowledge_checks
                ? Response.json(planState('released').plan)
                : problem('quality_gate_failed', { failing_checks: [altText], failing_gates: [], acknowledgeable_checks: ['chk-alt-text'] })),
        });

        await renderPanel(planState('done', { cms_objects: [writtenPage(9142)] }));
        await user.click(screen.getByRole('button', { name: 'Release' }));
        await screen.findByText('Not released: 1 check has a warning.');

        expect(actionNames()).toEqual(['Discard', 'Check again', 'Release anyway']);
        await user.click(screen.getByRole('button', { name: 'Release anyway' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release', acknowledge_checks: ['chk-alt-text'] }]));
        expect(screen.queryByText('Publishing…')).not.toBeInTheDocument();
    });

    it('offers no way past a blocking check, only to check again', async () => {
        const user = userEvent.setup();
        const { sent } = stubRoutes({ transition: () => problem('quality_gate_failed', { failing_checks: [brokenLink, altText], failing_gates: [{ gate_id: 'links', label: 'Links', kind: 'script', blocking: true, when: 'before_release', status: 'failed', attempt: 1, check_ids: ['chk-links'] }], acknowledgeable_checks: ['chk-alt-text'] }) });

        await renderPanel(planState('done'));
        await user.click(screen.getByRole('button', { name: 'Release' }));

        const checklist = screen.getByRole('region', { name: 'Checklist' });

        expect(await within(checklist).findByText('Not released: 1 blocking check must be fixed.')).toBeInTheDocument();
        // The blocking check as the refusal reported it, before the plan's state has it.
        expect(within(checklist).getByRole('group', { name: 'Links' })).toHaveTextContent('Links resolveFailed · blocking');
        expect(within(checklist).getByText('One link points to a deleted page.')).toBeInTheDocument();
        expect(actionNames()).toEqual(['Discard', 'Check again']);

        await user.click(screen.getByRole('button', { name: 'Check again' }));

        await waitFor(() => expect(sent).toEqual([{ action: 'release' }, { action: 'release' }]));
    });

    it('reports any other refusal as an error, e.g. a closed plan', async () => {
        const user = userEvent.setup();

        stubRoutes({ transition: () => problem('plan_closed') });
        await renderPanel(planState('released'));
        // Not Publish, which has tests of its own ('publishing, over the whole page').
        await user.click(screen.getByRole('button', { name: 'Request review' }));

        await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
            { messageKey: 'planBar.transitionFailed', detailKey: 'errors.genaix.plan_closed' },
        ]));
    });
});
