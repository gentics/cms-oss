import { expect, type Page, test } from '@playwright/test';

// A follow-up on a published plan revises that plan (rc.5, `options.mode: revise`) instead of drafting
// a second one. GenAIx is stubbed: the revision comes back as the same plan at the next version.

const SESSION = 's-1';
const PLAN = 'pl-1';
const BASE = `/rest/proxy/genaix/sessions/${SESSION}`;

function plan(status: string, version: number) {
    return {
        id: PLAN,
        workflows: [{ id: 'content_create', version: '1.0.0' }],
        status,
        version,
        summary: 'New terms page in Richtlinien.',
        items: [{ id: 'p1', label: 'Create the terms page', kind: 'create', workflow: 'content_create', status: 'done' }],
    };
}

/**
 * Stubs a session with a published plan; returns the messages posted. A message makes the stream
 * announce the revision: the plan `drafting` at its version, then `proposed` at the next one.
 */
async function stubSession(page: Page) {
    const messages: unknown[] = [];
    const frames: string[] = [];
    const announce = (data: Record<string, unknown>) => {
        const seq = frames.length + 1;
        const event = { seq, ts: '2026-11-03T09:00:00Z', session_id: SESSION, plan_id: PLAN, ...data };

        frames.push(`id: ${seq}\nevent: ${String(data.type)}\ndata: ${JSON.stringify(event)}\n\n`);
    };

    announce({ type: 'plan.updated', plan: plan('published', 2) });

    // Every other read (the messages, files, plans, modules, sessions) is empty. Registered first, so
    // the routes below take precedence.
    await page.route('**/rest/proxy/genaix/**', (route) => route.fulfill({ json: { items: [], next_cursor: null } }));
    // The session is already authorized on the default cms connection, so a message needs no new CMS token.
    await page.route((url) => url.pathname === '/rest/proxy/genaix/mcp/connections', (route) => route.fulfill({
        json: { items: [{ id: 'c-1', connector: 'cms', default: true }] },
    }));
    // A published plan is not open (`open_plan_id` null), so the message names it (`options.plan_id`).
    await page.route((url) => url.pathname === BASE, (route) => route.fulfill({
        json: { id: SESSION, status: 'active', open_plan_id: null, open_plan_ids: [], authorizations: [{ connection_id: 'c-1', status: 'authorized' }] },
    }));
    await page.route((url) => url.pathname === `${BASE}/plans/${PLAN}`, (route) => route.fulfill({ json: { cms_objects: [] } }));
    await page.route((url) => url.pathname === `${BASE}/messages`, (route) => {
        if (route.request().method() !== 'POST') {
            return route.fulfill({ json: { items: [], next_cursor: null } });
        }

        messages.push(route.request().postDataJSON());
        announce({ type: 'plan.updated', plan: plan('drafting', 2) });
        announce({ type: 'plan.updated', plan: plan('proposed', 3) });

        return route.fulfill({ status: 202, json: { message_id: 'm-1', run_id: 'r-1', session_id: SESSION } });
    });
    // Each request ends after its events, and the client reconnects.
    await page.route((url) => url.pathname === `${BASE}/events`, (route) => {
        const after = Number(new URL(route.request().url()).searchParams.get('after') ?? 0);

        return route.fulfill({ status: 200, contentType: 'text/event-stream', body: frames.slice(after).join('') });
    });

    return messages;
}

test.describe('Revising a plan', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('revises the published plan with a follow-up, and no second plan comes', async ({ page }) => {
        const messages = await stubSession(page);

        await page.goto(`#/sessions/${SESSION}`);

        const bar = page.getByRole('region', { name: 'Open plan' });
        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(bar).toContainText('Published');
        await expect(page.getByRole('button', { name: 'Side question' })).toHaveAttribute('aria-pressed', 'false');

        await field.click();
        await field.pressSequentially('Create a FAQ page for it');
        await field.press('Enter');

        await expect(bar).toContainText('Proposed');
        await expect(bar).toContainText('New terms page in Richtlinien.');
        await expect(page.getByRole('button', { name: /open plans?$/ })).toHaveCount(0);
        expect(messages).toEqual([{ parts: [{ type: 'text', text: 'Create a FAQ page for it' }], options: { mode: 'revise', plan_id: PLAN } }]);
    });
});
