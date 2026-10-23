import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useTranslation } from 'react-i18next';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { InteractionCard } from '@/components/InteractionCard/InteractionCard';
import { PlanAdjustActions } from '@/components/PlanBar/PlanAdjustActions';
import { ensureSessionCmsAuthorization } from '@/helper/sessionAuthorization/sessionAuthorization';
import { usePlanAdjustment } from '@/hooks/usePlanAdjustment';
import type { Interaction, Plan, PlanItem } from '@/services/apiService/genaix/types';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
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
    workflows: [{ id: 'content_create', version: '1.0.0' }],
    status: 'proposed',
    version: 2,
    summary: 'Create the page in Richtlinien.',
    items: [
        { id: 'p1', label: 'Create the page', kind: 'create', workflow: 'content_create', target: PAGE, status: 'pending' },
        { id: 'p2', label: 'Write the intro', kind: 'create', workflow: 'content_create', status: 'pending' },
    ],
};

// The PNDemo plan of S8 while its third version is proposed (rc.5): p1 and p2 done earlier, p3 new, p4
// new and depending on p1 and p3, and a step of the accepted version the revision removed.
const REVISION: Plan = {
    id: 'd4a7e1c9-6b52-4f08-9e3d-2a7c5b8f1e60',
    workflows: [{ id: 'content_create', version: '1.0.0' }, { id: 'content_edit', version: '1.0.0' }],
    status: 'proposed',
    version: 3,
    accepted_version: 2,
    summary: 'Invitation page PNDemo.',
    items: [
        { id: 'p1', label: 'Create page PNDemo', kind: 'create', workflow: 'content_create', status: 'done', change: 'unchanged' },
        { id: 'p2', label: 'Colour title and teaser', kind: 'update', workflow: 'content_edit', status: 'done', change: 'unchanged', depends_on: ['p1'] },
        { id: 'p3', label: 'Upload the PDF', kind: 'create', workflow: 'content_create', status: 'pending', change: 'added' },
        { id: 'p4', label: 'Link the PDF in the text', kind: 'link', workflow: 'content_edit', status: 'pending', change: 'added', depends_on: ['p1', 'p3'] },
        { id: 'p5', label: 'Add a teaser image', kind: 'update', workflow: 'content_edit', status: 'removed', change: 'removed' },
    ],
};

// Answers every request with `response` (an accepted message by default) and returns the mock, to read what was posted.
function stubFetch(response = () => Response.json({ message_id: 'm-1', run_id: 'r-1', session_id: 's-1' }, { status: 202 })) {
    const fetchMock = vi.fn<typeof fetch>(async () => response());

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

async function postedBody(fetchMock: ReturnType<typeof stubFetch>): Promise<unknown> {
    await waitFor(() => expect(fetchMock).toHaveBeenCalled());

    const [input, init] = fetchMock.mock.calls[0]!;

    expect(String(input)).toBe('/rest/proxy/genaix/sessions/s-1/messages');

    return JSON.parse(String(init?.body));
}

// The card with its adjustment, its note and its buttons, as the plan bar puts them together (`PlanBar`).
function AdjustablePlan({ plan }: { plan: Plan }) {
    const { t } = useTranslation();
    const adjustment = usePlanAdjustment('s-1', plan, true);

    return (
        <>
            {adjustment.noteKey && <p>{t(adjustment.noteKey)}</p>}
            <PlanCard plan={plan} sessionId="s-1" adjustment={adjustment} />
            <PlanAdjustActions adjustment={adjustment} />
        </>
    );
}

describe('PlanCard', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
        useErrorNotificationStore.setState({ errors: [] });
        vi.mocked(ensureSessionCmsAuthorization).mockResolvedValue();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('accepts the plan as proposed with a plan part naming the plan and its version', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<AdjustablePlan plan={PLAN} />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{ type: 'plan', plan: { plan_id: PLAN.id, based_on_version: 2 } }],
            options: { mode: 'execute' },
        });
    });

    it('accepts the adjusted plan with its steps in order, kept steps by their id, a new one without', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<AdjustablePlan plan={PLAN} />, { wrapper: createWrapper() });
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
    it('offers Accept again on a revision proposed after the user accepted an earlier version (rc.5)', async () => {
        const user = userEvent.setup();

        stubFetch();

        const v1 = { ...PLAN, version: 1 };
        const { rerender } = render(<AdjustablePlan plan={v1} />, { wrapper: createWrapper() });

        await user.click(screen.getByRole('button', { name: 'Accept plan' }));
        await waitFor(() => expect(screen.queryByRole('button', { name: 'Accept plan' })).not.toBeInTheDocument());

        // Executed and done, then "Ask for a change": drafted at the same version, proposed at the next one.
        rerender(<AdjustablePlan plan={{ ...v1, status: 'done', accepted_version: 1 }} />);
        rerender(<AdjustablePlan plan={{ ...v1, status: 'drafting', accepted_version: 1 }} />);
        rerender(<AdjustablePlan plan={{ ...v1, status: 'proposed', version: 2, accepted_version: 1 }} />);

        expect(screen.getByRole('button', { name: 'Accept plan' })).toBeEnabled();
        expect(screen.getByRole('button', { name: 'Adjust' })).toBeEnabled();
    });

    it('marks what a proposed revision added, and strikes a removed step through (rc.5)', () => {
        render(<AdjustablePlan plan={REVISION} />, { wrapper: createWrapper() });

        const [p3, , p5] = screen.getAllByRole('listitem');

        expect(p3).toHaveTextContent('Upload the PDFNew');
        expect(p5).toHaveTextContent('Removed');
    });

    it('shows only the next steps of a revision, not what an earlier version did, until it is accepted', () => {
        const items: PlanItem[] = [
            { id: 'p1', label: 'Create page Fair', kind: 'create', workflow: 'content_create', status: 'done', creates: { type: 'page', label: 'Fair' } },
            { id: 'p2', label: 'Create page Expo', kind: 'create', workflow: 'content_create', status: 'pending', creates: { type: 'page', label: 'Expo' } },
            { id: 'p3', label: 'Create page Gala', kind: 'create', workflow: 'content_create', status: 'pending', creates: { type: 'page', label: 'Gala' } },
        ];
        const revision: Plan = { ...PLAN, version: 3, accepted_version: 2, items };
        const { rerender } = render(<AdjustablePlan plan={revision} />, { wrapper: createWrapper() });

        expect(screen.queryByText('Create page Fair')).not.toBeInTheDocument();
        expect(screen.getAllByRole('listitem')[0]).toHaveTextContent(/^1Create page Expo/);
        expect(screen.getByRole('group', { name: 'At a glance' })).toHaveTextContent('+2 new pages');

        rerender(<AdjustablePlan plan={{ ...revision, status: 'running' }} />);

        expect(screen.getByText('Create page Fair')).toBeInTheDocument();
        expect(screen.getByRole('group', { name: 'At a glance' })).toHaveTextContent('1/3 new pages');
    });

    it('marks nothing on a plan that was never accepted, where every step is added', () => {
        const items: PlanItem[] = PLAN.items.map((item) => ({ ...item, change: 'added' }));

        render(<AdjustablePlan plan={{ ...PLAN, items }} />, { wrapper: createWrapper() });

        expect(screen.queryByText('New')).not.toBeInTheDocument();
    });

    it('accepts a revision as proposed without sending the removed step', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<AdjustablePlan plan={REVISION} />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{ type: 'plan', plan: { plan_id: REVISION.id, based_on_version: 3 } }],
            options: { mode: 'execute' },
        });
    });

    it('hides done steps while adjusting, and blocks what breaks an order of dependencies', async () => {
        const user = userEvent.setup();

        render(<AdjustablePlan plan={REVISION} />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));

        // Only the pending steps, numbered from 1; p1 and p2 are done and kept out of view.
        expect(screen.getAllByRole('textbox')).toHaveLength(2);
        expect(screen.getByRole('textbox', { name: 'Step 1' })).toHaveValue('Upload the PDF');
        expect(screen.queryByText('Create page PNDemo')).not.toBeInTheDocument();

        const [upP3, upP4] = screen.getAllByRole('button', { name: 'Move step up' });
        const [downP3, downP4] = screen.getAllByRole('button', { name: 'Move step down' });
        const [removeP3, removeP4] = screen.getAllByRole('button', { name: 'Remove step' });

        // p3 cannot pass the done p2, nor move below p4, which needs it; p4 cannot move above p3.
        expect(upP3).toBeDisabled();
        expect(downP3).toBeDisabled();
        expect(upP4).toBeDisabled();
        expect(downP4).toBeDisabled();
        // p4 needs p3, so p3 cannot be removed; p4 can.
        expect(removeP3).toBeDisabled();
        expect(removeP4).toBeEnabled();
    });

    it('sends a removed step again only once the user restored it', async () => {
        const user = userEvent.setup();
        const fetchMock = stubFetch();

        render(<AdjustablePlan plan={REVISION} />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.click(screen.getByRole('button', { name: 'Restore' }));
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await postedBody(fetchMock)).toEqual({
            parts: [{
                type: 'plan',
                plan: {
                    plan_id: REVISION.id,
                    based_on_version: 3,
                    items: [
                        { id: 'p1', label: 'Create page PNDemo' },
                        { id: 'p2', label: 'Colour title and teaser' },
                        { id: 'p3', label: 'Upload the PDF' },
                        { id: 'p4', label: 'Link the PDF in the text' },
                        { id: 'p5', label: 'Add a teaser image' },
                    ],
                },
            }],
            options: { mode: 'execute' },
        });
    });

    it('highlights the step the server refused with 422 instead of an error notification', async () => {
        const user = userEvent.setup();

        stubFetch(() => Response.json({
            type: 't',
            title: 'Validation failed',
            status: 422,
            genaix_code: 'validation_failed',
            errors: [{ pointer: '/parts/0/plan/items/1', detail: 'p2 depends on p1', code: 'dependency_order' }],
        }, { status: 422 }));

        render(<AdjustablePlan plan={PLAN} />, { wrapper: createWrapper() });
        await user.click(screen.getByRole('button', { name: 'Adjust' }));
        await user.clear(screen.getByRole('textbox', { name: 'Step 2' }));
        await user.type(screen.getByRole('textbox', { name: 'Step 2' }), 'Write a short intro');
        await user.click(screen.getByRole('button', { name: 'Done' }));
        await user.click(screen.getByRole('button', { name: 'Accept plan' }));

        expect(await screen.findByText('Comes before a step it needs')).toBeInTheDocument();
        expect(screen.getAllByRole('listitem')[1]).toHaveTextContent('Write a short intro');
        expect(screen.getAllByRole('listitem')[1]).toHaveTextContent('Comes before a step it needs');
        expect(screen.getByText('Adjust the highlighted steps')).toBeInTheDocument();
        expect(useErrorNotificationStore.getState().errors).toEqual([]);
    });

    describe('plans at a glance (rc.5 addition)', () => {
        // S1's proposal with the addition's members: p1 creates the page in Richtlinien (its target), p2 and
        // p3 fill its tags, p6 links to it from another page.
        const FOLDER = { type: 'folder' as const, id: '42', node_id: 3, label: 'Richtlinien' };
        const WARRANTY = { type: 'page' as const, id: '8871', node_id: 3, label: 'Garantiebedingungen' };
        const GLANCE: Plan = {
            ...PLAN,
            version: 1,
            items: [
                { id: 'p1', label: 'Create the page', kind: 'create', workflow: 'content_create', status: 'pending', target: FOLDER, creates: { type: 'page', label: 'Neue Nutzungsbedingungen ab Q3', path: '/Richtlinien/' } },
                { id: 'p2', label: 'Hero', kind: 'construct', workflow: 'content_create', status: 'pending', parent: 'p1', depends_on: ['p1'] },
                { id: 'p3', label: 'Cancellation policy (verbatim)', kind: 'construct', workflow: 'content_create', status: 'pending', parent: 'p1' },
                { id: 'p6', label: 'Link from Garantiebedingungen', kind: 'link', workflow: 'content_create', status: 'pending', target: WARRANTY },
            ],
        };

        it('folds the sub-steps under their item, opened with its "n steps"', async () => {
            const user = userEvent.setup();

            render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });

            expect(screen.getAllByRole('listitem')).toHaveLength(2);
            expect(screen.queryByText('Hero')).not.toBeVisible();

            await user.click(screen.getByRole('button', { name: '2 steps' }));

            expect(screen.getByRole('button', { name: '2 steps' })).toHaveAttribute('aria-expanded', 'true');
            expect(screen.getAllByRole('listitem')).toHaveLength(4);
            expect(screen.getByText('Hero')).toBeVisible();
        });

        it('lists the sub-steps of a plan with one item on one level with it, numbered in order', () => {
            render(<AdjustablePlan plan={{ ...GLANCE, items: GLANCE.items.filter(({ id }) => id !== 'p6') }} />, { wrapper: createWrapper() });

            const items = screen.getAllByRole('listitem');

            expect(items).toHaveLength(3);
            expect(items[0]).toHaveTextContent(/^1Create the page/);
            expect(items[1]).toHaveTextContent(/^2Hero/);
            expect(items[2]).toHaveTextContent(/^3Cancellation policy/);
            expect(screen.queryByRole('button', { name: '2 steps' })).not.toBeInTheDocument();
        });

        it('shows what an item will create, then what it created', () => {
            const { rerender } = render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });
            const [p1] = screen.getAllByRole('listitem');

            // What it creates, with where; its target, the parent folder, is in that path.
            expect(p1).toHaveTextContent('Neue Nutzungsbedingungen ab Q3 · /Richtlinien/');

            const created = { type: 'page' as const, id: '9142', node_id: 3, label: 'Neue Nutzungsbedingungen ab Q3' };

            rerender(<AdjustablePlan plan={{ ...GLANCE, status: 'running', items: GLANCE.items.map((item) => (item.id === 'p1' ? { ...item, status: 'done', created } : item)) }} />);

            expect(screen.getAllByRole('listitem')[0]).toHaveTextContent('Neue Nutzungsbedingungen ab Q3');
            expect(screen.getAllByRole('listitem')[0]).not.toHaveTextContent('/Richtlinien/');
        });

        it('keeps the sub-steps read-only while adjusting, and moves them with their item', async () => {
            const user = userEvent.setup();
            const fetchMock = stubFetch();

            render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });
            await user.click(screen.getByRole('button', { name: 'Adjust' }));

            // A field for p1 and p6 only; the sub-steps are shown under p1.
            expect(screen.getAllByRole('textbox')).toHaveLength(2);
            expect(screen.getByText('Hero')).toBeInTheDocument();

            await user.click(screen.getAllByRole('button', { name: 'Move step up' })[1]!);
            await user.click(screen.getByRole('button', { name: 'Done' }));
            await user.click(screen.getByRole('button', { name: 'Accept plan' }));

            expect(await postedBody(fetchMock)).toMatchObject({
                parts: [{
                    plan: {
                        items: [
                            { id: 'p6', label: 'Link from Garantiebedingungen', target: WARRANTY },
                            { id: 'p1', label: 'Create the page', target: FOLDER },
                            { id: 'p2', label: 'Hero' },
                            { id: 'p3', label: 'Cancellation policy (verbatim)' },
                        ],
                    },
                }],
            });
        });

        it('removes the sub-steps with their item', async () => {
            const user = userEvent.setup();
            const fetchMock = stubFetch();

            render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });
            await user.click(screen.getByRole('button', { name: 'Adjust' }));
            await user.click(screen.getAllByRole('button', { name: 'Remove step' })[0]!);

            expect(screen.queryByText('Hero')).not.toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: 'Done' }));
            await user.click(screen.getByRole('button', { name: 'Accept plan' }));

            expect(await postedBody(fetchMock)).toMatchObject({
                parts: [{ plan: { items: [{ id: 'p6', label: 'Link from Garantiebedingungen', target: WARRANTY }] } }],
            });
        });

        it('adds no step past 200', async () => {
            const user = userEvent.setup();
            const items: PlanItem[] = Array.from({ length: 200 }, (_, i) => ({ id: `p${i}`, label: `Step ${i}`, kind: 'update', workflow: 'content_edit', status: 'pending' }));

            render(<AdjustablePlan plan={{ ...PLAN, items }} />, { wrapper: createWrapper() });
            await user.click(screen.getByRole('button', { name: 'Adjust' }));

            expect(screen.getByRole('button', { name: 'Add a step' })).toBeDisabled();
            expect(screen.getByText('At most 200 steps')).toBeInTheDocument();
        });

        it('counts the top-level items as tiles, "+n" while proposed and "done/total" once accepted', () => {
            const { rerender } = render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });
            const glance = screen.getByRole('group', { name: 'At a glance' });

            expect(glance).toHaveTextContent('+1 new page');
            expect(glance).toHaveTextContent('1 link');

            rerender(<AdjustablePlan plan={{ ...GLANCE, status: 'running', items: GLANCE.items.map((item) => (item.id === 'p1' ? { ...item, status: 'done' } : item)) }} />);

            expect(screen.getByRole('group', { name: 'At a glance' })).toHaveTextContent('1/1 new page');
            expect(screen.getByRole('group', { name: 'At a glance' })).toHaveTextContent('0/1 link');
        });

        describe('Needs you', () => {
            const question: Interaction = {
                id: 'i-1',
                kind: 'choice',
                prompt: 'Which template should the page use?',
                options: [{ id: 'tpl-17', label: 'Kampagnen-Landingpage' }],
                multi: false,
                other_allowed: false,
                blocking: true,
                status: 'pending',
                requested_at: new Date().toISOString(),
                expires_at: new Date(Date.now() + 10 * 60_000).toISOString(),
                run_id: 'r-1',
            };

            function ask(planId: string) {
                useWorkspaceEventStore.getState().applyEvent('s-1', { seq: 1, ts: '2026-10-15T09:00:00Z', session_id: 's-1', run_id: 'r-1', plan_id: planId, type: 'interaction.requested', interaction: question });
            }

            it('says what the plan\'s run waits on, and "Answer" takes the user to the question', async () => {
                const user = userEvent.setup();

                ask(GLANCE.id);
                render(
                    <>
                        <AdjustablePlan plan={GLANCE} />
                        <InteractionCard interaction={question} sessionId="s-1" />
                    </>,
                    { wrapper: createWrapper() },
                );

                const needsYou = screen.getByRole('region', { name: 'Needs you' });

                expect(needsYou).toHaveTextContent('Which template should the page use?');

                await user.click(within(needsYou).getByRole('button', { name: 'Answer' }));

                // The question's card, not a control in it.
                expect(document.activeElement).toHaveAttribute('id', 'interaction-card-i-1');
                expect(document.activeElement).toContainElement(screen.getByRole('radiogroup', { name: 'Which template should the page use?' }));
            });

            it('offers no Accept or Adjust while the question is open', () => {
                ask(GLANCE.id);
                render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });

                expect(screen.queryByRole('button', { name: 'Accept plan' })).not.toBeInTheDocument();
                expect(screen.queryByRole('button', { name: 'Adjust' })).not.toBeInTheDocument();
            });

            it('says nothing for a question of another plan, nor once the plan is accepted', () => {
                ask('another-plan');
                const { rerender } = render(<AdjustablePlan plan={GLANCE} />, { wrapper: createWrapper() });

                expect(screen.queryByRole('region', { name: 'Needs you' })).not.toBeInTheDocument();

                useWorkspaceEventStore.setState({ sessions: {} });
                ask(GLANCE.id);
                rerender(<AdjustablePlan plan={{ ...GLANCE, status: 'running' }} />);

                expect(screen.queryByRole('region', { name: 'Needs you' })).not.toBeInTheDocument();
            });
        });
    });
});
