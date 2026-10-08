import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { startSingleSignOn } from '@/helper/keycloak/keycloak';
import { createWrapper } from '@/test/renderWithProviders';

import { LoginGate } from './LoginGate';

vi.mock('@/helper/keycloak/keycloak', () => ({ startSingleSignOn: vi.fn(), loginWithKeycloak: vi.fn() }));

const cmsUser = { id: 3, login: 'editor', firstName: 'Eddie', lastName: 'Tor' };

/** `GET /rest/user/me` answers each of `answers` in turn: a user, or `null` for `401`. */
function stubUser(...answers: (typeof cmsUser | null)[]) {
    const fetchMock = vi.fn<typeof fetch>();

    for (const answer of answers) {
        fetchMock.mockResolvedValueOnce(answer ? Response.json({ user: answer }) : new Response('', { status: 401 }));
    }

    vi.stubGlobal('fetch', fetchMock);
}

function renderGate() {
    render(<LoginGate><p>The workspace</p></LoginGate>, { wrapper: createWrapper() });
}

describe('LoginGate', () => {
    beforeEach(() => {
        vi.mocked(startSingleSignOn).mockResolvedValue({ available: false, showButton: false, loggedIn: false });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('shows the app with a CMS session, without single sign-on', async () => {
        stubUser(cmsUser);
        renderGate();

        expect(await screen.findByText('The workspace')).toBeInTheDocument();
        expect(startSingleSignOn).not.toHaveBeenCalled();
    });

    it('shows the login form without a session when the CMS has no Keycloak', async () => {
        stubUser(null);
        renderGate();

        expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Log in with SSO' })).not.toBeInTheDocument();
        expect(screen.queryByText('The workspace')).not.toBeInTheDocument();
    });

    it('offers the SSO button when Keycloak shows one', async () => {
        vi.mocked(startSingleSignOn).mockResolvedValue({ available: true, showButton: true, loggedIn: false });
        stubUser(null);
        renderGate();

        expect(await screen.findByRole('button', { name: 'Log in with SSO' })).toBeInTheDocument();
    });

    it('shows the app once single sign-on created a session', async () => {
        vi.mocked(startSingleSignOn).mockResolvedValue({ available: true, showButton: false, loggedIn: true });
        stubUser(null, cmsUser);
        renderGate();

        expect(await screen.findByText('The workspace')).toBeInTheDocument();
    });

    it('shows the login form with a message when single sign-on failed', async () => {
        vi.mocked(startSingleSignOn).mockRejectedValue(new Error('Keycloak unreachable'));
        stubUser(null);
        renderGate();

        expect(await screen.findByRole('alert')).toHaveTextContent('Single sign-on is not available.');
        expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    });

    it('shows the login form with the reason when the CMS cannot be asked for the session', async () => {
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockResolvedValue(new Response('', { status: 500 })));
        renderGate();

        expect(await screen.findByRole('alert')).toHaveTextContent('The server ran into an error.');
        expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
        expect(startSingleSignOn).not.toHaveBeenCalled();
    });

    it('offers the SSO button again when the session from single sign-on is gone', async () => {
        vi.mocked(startSingleSignOn).mockResolvedValue({ available: true, showButton: false, loggedIn: true });
        stubUser(null, null);
        renderGate();

        expect(await screen.findByRole('button', { name: 'Log in with SSO' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    });
});
