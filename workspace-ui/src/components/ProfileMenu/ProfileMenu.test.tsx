import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { leaveAfterLogout } from '@/helper/keycloak/keycloak';
import { CMS_USER_QUERY_KEY } from '@/hooks/useCmsAuth';
import type { CmsGroup, CmsUser } from '@/services/cmsApiService/cmsApiService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { ProfileMenu } from './ProfileMenu';

vi.mock('@/helper/keycloak/keycloak', () => ({ leaveAfterLogout: vi.fn() }));

const ERIKA: CmsUser = { id: 3, login: 'emu', firstName: 'Erika', lastName: 'Mustermann' };
const GROUPS: CmsGroup[] = [{ id: 31, name: 'Chief Content Creator' }, { id: 7, name: 'Editors' }];

/** A CMS that answers `GET /rest/user/me?groups=true` with `groups` (or `groupsResponse`), anything else with `OK`. */
function stubCms({ groups = GROUPS, groupsResponse }: { groups?: CmsGroup[]; groupsResponse?: () => Response } = {}) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        if (String(input) === '/rest/user/me?groups=true') {
            return groupsResponse?.() ?? Response.json({ responseInfo: { responseCode: 'OK' }, user: { ...ERIKA, groups } });
        }

        return Response.json({ responseInfo: { responseCode: 'OK' } });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

/** Renders the menu with `user` as the CMS user `LoginGate` loaded (none for `null`). */
function renderMenu(user: CmsUser | null = ERIKA) {
    const rendered = renderWithProviders(<ProfileMenu />);

    if (user) {
        rendered.queryClient.setQueryData(CMS_USER_QUERY_KEY, user);
    }

    return rendered;
}

describe('ProfileMenu', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
        useErrorNotificationStore.setState({ errors: [] });
    });

    it('shows the user\'s initials, named after the user', async () => {
        stubCms();
        renderMenu();

        expect(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' })).toHaveTextContent('EM');
    });

    it('takes the login when the user has no name', async () => {
        stubCms();
        renderMenu({ ...ERIKA, firstName: '', lastName: '' });

        expect(await screen.findByRole('button', { name: 'Profile of emu' })).toHaveTextContent('E');
    });

    it('opens with the name, the login and the groups, fetched only then', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms();

        renderMenu();
        const trigger = await screen.findByRole('button', { name: 'Profile of Erika Mustermann' });

        expect(fetchMock).not.toHaveBeenCalled();

        await user.click(trigger);
        const menu = await screen.findByRole('menu');

        expect(within(menu).getByText('Erika Mustermann')).toBeInTheDocument();
        expect(within(menu).getByText('emu')).toBeInTheDocument();

        const groups = await within(menu).findByRole('group', { name: 'Groups' });

        expect(within(groups).getAllByRole('listitem').map((item) => item.textContent)).toEqual(['Chief Content Creator', 'Editors']);
        expect(fetchMock).toHaveBeenCalledWith('/rest/user/me?groups=true', expect.objectContaining({ credentials: 'same-origin' }));
    });

    it('names one group in the singular', async () => {
        const user = userEvent.setup();

        stubCms({ groups: [GROUPS[0]!] });
        renderMenu();

        await user.click(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' }));

        expect(await screen.findByRole('group', { name: 'Group' })).toHaveTextContent('Chief Content Creator');
    });

    it('leaves the groups out when they could not be fetched, and keeps the logout', async () => {
        const user = userEvent.setup();

        stubCms({ groupsResponse: () => new Response('', { status: 500 }) });
        renderMenu();

        await user.click(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' }));

        expect(await screen.findByRole('menuitem', { name: 'Log out' })).toBeInTheDocument();
        await waitFor(() => expect(screen.queryByText('Chief Content Creator')).not.toBeInTheDocument());
        expect(screen.queryByRole('group', { name: /Group/ })).not.toBeInTheDocument();
    });

    it('without a loaded user shows a person icon and only the logout, and does not ask the CMS', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms();

        renderMenu(null);
        await user.click(await screen.findByRole('button', { name: 'Profile' }));

        expect(await screen.findByRole('menuitem', { name: 'Log out' })).toBeInTheDocument();
        expect(screen.queryByRole('group')).not.toBeInTheDocument();
        expect(fetchMock).not.toHaveBeenCalled();
    });

    describe('logout', () => {
        it('ends the CMS session, then leaves the page', async () => {
            const user = userEvent.setup();
            const fetchMock = stubCms();

            renderMenu();
            await user.click(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' }));
            await user.click(await screen.findByRole('menuitem', { name: 'Log out' }));

            await waitFor(() => expect(leaveAfterLogout).toHaveBeenCalled());
            expect(fetchMock).toHaveBeenCalledWith('/rest/auth/logout', expect.objectContaining({ method: 'POST' }));
        });

        it('shows an error notification when the logout failed, and stays', async () => {
            const user = userEvent.setup();

            vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(new Response('', { status: 500 })));
            renderMenu();

            await user.click(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' }));
            await user.click(await screen.findByRole('menuitem', { name: 'Log out' }));

            await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([{ messageKey: 'login.errors.logout' }]));
            expect(leaveAfterLogout).not.toHaveBeenCalled();
        });

        it('leads to the login with a notification when only the Keycloak logout failed', async () => {
            const user = userEvent.setup();

            stubCms();
            vi.mocked(leaveAfterLogout).mockRejectedValueOnce(new TypeError('Failed to fetch'));
            const { queryClient } = renderMenu();

            await user.click(await screen.findByRole('button', { name: 'Profile of Erika Mustermann' }));
            await user.click(await screen.findByRole('menuitem', { name: 'Log out' }));

            await waitFor(() => expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'login.errors.singleSignOnLogout', detailKey: 'errors.network' },
            ]));
            expect(queryClient.getQueryData(CMS_USER_QUERY_KEY)).toBeNull();
        });
    });
});
