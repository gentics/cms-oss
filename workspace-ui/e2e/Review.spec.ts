import { expect, type Page, test } from '@playwright/test';

// The review page of an S1 session (content creation) in the browser: the plan is released, then
// published, two steps as in the contract (`POST …/plans/{plan_id}/transitions`). GenAIx is stubbed
// with a plan that moves on with each transition; the frame's page too, as the dev server has no CMS.

const SESSION = 's-1';
const PLAN = 'pl-1';
const BASE = `/rest/proxy/genaix/sessions/${SESSION}`;

const page9142 = { type: 'page', id: 9142, node_id: 3, name: 'Neue Nutzungsbedingungen' };

// The plan's state in `status`; once it is published, its page is too.
function planState(status: string) {
    return {
        workflow: 'content_create',
        state: status === 'published' ? 'done' : status === 'released' ? 'awaiting_approval' : 'done',
        plan: { id: PLAN, workflow: 'content_create', status, version: 2, summary: 'New terms page in Richtlinien.', items: [] },
        steps: [],
        checks: [{ id: 'chk-1', label: 'Formal address', severity: 'pass', blocking: false, message: 'Fine.', source: { type: 'guideline', label: 'Tone & Address' } }],
        gates: [],
        approval_chain: [{ id: 'ap-1', label: 'Author releases the draft', actor: { type: 'user' }, status: status === 'done' ? 'pending' : 'done' }],
        cms_objects: [],
        publications: [{
            cms_ref: page9142,
            status: status === 'published' ? 'published' : 'pending',
            approval_chain: [{ id: 'ap-1', label: 'Author releases the draft', actor: { type: 'user' }, status: status === 'done' ? 'pending' : 'done' }],
        }],
        preview: { cms_ref: page9142, preview_url: 'http://127.0.0.1:5173/alohapage?realid=9142&nodeid=3&mode=view', mode: 'view' },
    };
}

/**
 * Stubs the session, its plan and the transitions; returns the actions posted, in order. The event
 * stream announces the plan with `plan.updated`, once at the start and again after each transition,
 * as GenAIx does; each request ends after its events, and the client reconnects.
 */
async function stubReview(page: Page) {
    const actions: string[] = [];
    let status = 'done';
    const frames: string[] = [];
    const announce = () => {
        const seq = frames.length + 1;
        const data = { seq, ts: '2026-10-13T09:00:00Z', session_id: SESSION, plan_id: PLAN, type: 'plan.updated', plan: planState(status).plan };

        frames.push(`id: ${seq}\nevent: plan.updated\ndata: ${JSON.stringify(data)}\n\n`);
    };

    announce();

    // The session is already authorized on the default cms connection, so a transition needs no new CMS token.
    await page.route((url) => url.pathname === '/rest/proxy/genaix/mcp/connections', (route) => route.fulfill({
        json: { items: [{ id: 'c-1', connector: 'cms', default: true }] },
    }));
    await page.route((url) => url.pathname === BASE, (route) => route.fulfill({
        json: { id: SESSION, status: 'active', open_plan_id: PLAN, authorizations: [{ connection_id: 'c-1', status: 'authorized' }] },
    }));
    await page.route((url) => url.pathname === `${BASE}/plans`, (route) => route.fulfill({
        json: { items: [{ id: PLAN, workflow: 'content_create', status, version: 2, summary: '', created_at: '2026-10-13T09:00:00Z' }] },
    }));
    await page.route((url) => url.pathname === `${BASE}/plans/${PLAN}`, (route) => route.fulfill({ json: planState(status) }));
    await page.route((url) => url.pathname === `${BASE}/plans/${PLAN}/transitions`, (route) => {
        const { action } = route.request().postDataJSON() as { action: string };

        actions.push(action);
        status = action === 'release' ? 'released' : action === 'publish' ? 'published' : status;
        announce();

        return route.fulfill({ json: planState(status).plan });
    });
    await page.route((url) => url.pathname === `${BASE}/events`, (route) => {
        const after = Number(new URL(route.request().url()).searchParams.get('after') ?? 0);

        return route.fulfill({ status: 200, contentType: 'text/event-stream', body: frames.slice(after).join('') });
    });
    await page.route((url) => url.pathname === '/rest/proxy/genaix/workflows', (route) => route.fulfill({ json: { items: [] } }));
    await page.route((url) => url.pathname === '/alohapage', (route) => route.fulfill({ contentType: 'text/html', body: '<p>Neue Nutzungsbedingungen ab Q3</p>' }));

    return actions;
}

test.describe('Review & release', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('releases the plan, then publishes it, and shows the page as published', async ({ page }) => {
        const actions = await stubReview(page);

        await page.goto(`#/sessions/${SESSION}/review`);

        const panel = page.getByRole('region', { name: 'Review' });

        await expect(page.getByRole('heading', { level: 1, name: 'Review & release' })).toBeVisible();
        await expect(page.frameLocator('iframe[title="Preview of the page in the CMS"]').getByText('Neue Nutzungsbedingungen ab Q3')).toBeVisible();
        await expect(panel.getByRole('region', { name: 'Checklist' })).toContainText('Formal address');

        await panel.getByRole('button', { name: 'Release' }).click();
        await expect(panel.getByRole('button', { name: 'Publish' })).toBeVisible();

        await panel.getByRole('button', { name: 'Publish' }).click();
        await expect(panel.getByRole('region', { name: 'Pages' })).toContainText('Published');
        await expect(panel.getByRole('button', { name: 'Reopen' })).toBeVisible();

        expect(actions).toEqual(['release', 'publish']);
    });
});
