import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { taskFixture } from '@/test/cmsTasks';
import { renderWithProviders } from '@/test/renderWithProviders';

import { AssignBackDialog } from './AssignBackDialog';

const task = taskFixture({
    id: 'publishQueue:page:9142',
    source: 'publishQueue',
    type: 'approvePublish',
    objects: [{ type: 'page', id: 9142, nodeId: 3, name: 'Terms of use' }],
    details: { requestedBy: { id: 42, name: 'Example Erika' } },
    actions: ['assignBack'],
});

// `users: null` answers `GET /rest/user` with 403.
function stubCms(users: { id: number; firstName: string; lastName: string }[] | null) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        if (String(input) === '/rest/user') {
            return users ? Response.json({ items: users }) : new Response(null, { status: 403 });
        }

        return Response.json({ responseInfo: { responseCode: 'OK' } });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

describe('AssignBackDialog', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('preselects who asked to publish and hands the page back to the chosen users with the message', async () => {
        const user = userEvent.setup();
        const onOpenChange = vi.fn();
        const fetchMock = stubCms([{ id: 42, firstName: 'Erika', lastName: 'Example' }, { id: 43, firstName: 'Max', lastName: 'Sample' }]);

        renderWithProviders(<AssignBackDialog task={task} open onOpenChange={onOpenChange} />);

        expect(await screen.findByRole('checkbox', { name: 'Erika Example' })).toBeChecked();
        await user.click(screen.getByRole('checkbox', { name: 'Max Sample' }));
        await user.type(screen.getByRole('textbox', { name: 'Message' }), 'Please fix the date');
        await user.click(screen.getByRole('button', { name: 'Assign' }));

        await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
        const [, init] = fetchMock.mock.calls.find(([input]) => String(input) === '/rest/page/assign')!;

        expect(JSON.parse(init?.body as string)).toEqual({ pageIds: ['9142'], userIds: [42, 43], message: 'Please fix the date' });
    });

    it('offers who asked to publish when the user list is refused', async () => {
        stubCms(null);

        renderWithProviders(<AssignBackDialog task={task} open onOpenChange={vi.fn()} />);

        expect(await screen.findByRole('checkbox', { name: 'Example Erika' })).toBeChecked();
        expect(screen.getAllByRole('checkbox')).toHaveLength(1);
    });
});
