import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { CmsObjectRef, Plan, PlanStatus, Workflow } from '@/services/apiService/genaix/types';
import { useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';
import { createWrapper } from '@/test/renderWithProviders';

import { usePlanPresence } from './usePlanPresence';

const SESSION = 's-1';

// A module confirmed in the chat (`plan_approval: false`), like `admin_user`.
const ADMIN_USER: Workflow = { id: 'admin_user', version: '0.9.0', title: 'admin_user', description: '', roles: [], optional: true, steps_template: [], plan_approval: false };

function plan(status: PlanStatus): Plan {
    return { id: 'pl-1', workflows: [{ id: 'admin_user', version: '0.9.0' }], status, version: 1, summary: '', items: [] };
}

// Answers the module catalogue and the plan's state (`GET …/plans/pl-1`) with `cmsObjects`.
function stubGenaix(cmsObjects: CmsObjectRef[]) {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
        const url = String(input);

        if (url.endsWith('/workflows')) {
            return Response.json({ items: [ADMIN_USER] });
        }

        return url.endsWith(`/sessions/${SESSION}/plans/pl-1`)
            ? Response.json({ plan: plan('done'), cms_objects: cmsObjects })
            : Response.json({ items: [], next_cursor: null });
    }));
}

function planStateRequests() {
    return vi.mocked(fetch).mock.calls.map(([input]) => String(input)).filter((url) => url.endsWith('/plans/pl-1'));
}

describe('usePlanPresence', () => {
    beforeEach(() => {
        useWorkspaceEventStore.setState({ sessions: {} });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('keeps the head of a done plan confirmed in the chat that created a CMS user', async () => {
        stubGenaix([{ type: 'user', id: 1342, name: 'Sophie Kaiser', operation: 'created' }]);

        const { result } = renderHook(() => usePlanPresence(SESSION, plan('done')), { wrapper: createWrapper() });

        await waitFor(() => expect(result.current).toBe('head'));
    });

    it('shows nothing of such a plan that created no user', async () => {
        stubGenaix([{ type: 'group', id: 31, operation: 'read' }]);

        const { result } = renderHook(() => usePlanPresence(SESSION, plan('done')), { wrapper: createWrapper() });

        await waitFor(() => expect(planStateRequests()).toHaveLength(1));
        expect(result.current).toBe('none');
    });

    it('does not ask for the state of a plan whose run has not finished', async () => {
        stubGenaix([{ type: 'user', id: 1342, operation: 'created' }]);

        const { result } = renderHook(() => usePlanPresence(SESSION, plan('running')), { wrapper: createWrapper() });

        // `head` from the module catalogue, as for any plan under way.
        await waitFor(() => expect(result.current).toBe('head'));
        expect(planStateRequests()).toEqual([]);
    });
});
