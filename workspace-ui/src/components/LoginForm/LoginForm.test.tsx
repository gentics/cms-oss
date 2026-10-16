import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { loginWithKeycloak } from '@/helper/keycloak/keycloak';
import { CmsLoginError } from '@/services/cmsApiService/cmsApiService';
import { createWrapper } from '@/test/renderWithProviders';

import { LoginForm } from './LoginForm';

vi.mock('@/helper/keycloak/keycloak', () => ({ loginWithKeycloak: vi.fn() }));

function stubLogin(response: Response) {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(response);

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

async function logIn(username: string, password: string) {
    const user = userEvent.setup();

    await user.type(screen.getByLabelText('User name'), username);
    await user.type(screen.getByLabelText('Password'), password);
    await user.click(screen.getByRole('button', { name: 'Log in' }));
}

describe('LoginForm', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        vi.clearAllMocks();
    });

    it('shows the brand and the fields, and logs in only with both filled in', async () => {
        const user = userEvent.setup();
        const fetchMock = stubLogin(Response.json({ responseInfo: { responseCode: 'OK' } }));

        render(<LoginForm />, { wrapper: createWrapper() });

        expect(screen.getByRole('heading', { name: 'Gentics Workspace' })).toBeInTheDocument();
        expect(screen.getByLabelText('User name')).toHaveFocus();
        expect(screen.queryByRole('button', { name: 'Log in with SSO' })).not.toBeInTheDocument();

        await user.type(screen.getByLabelText('User name'), 'editor');
        await user.click(screen.getByRole('button', { name: 'Log in' }));

        expect(fetchMock).not.toHaveBeenCalled();
        expect(screen.getByLabelText('Password')).toHaveFocus();
    });

    it('logs in with values the browser filled in without an input event (autofill)', async () => {
        const user = userEvent.setup();
        const fetchMock = stubLogin(Response.json({ responseInfo: { responseCode: 'OK' }, user: { id: 3, login: 'editor', firstName: 'E', lastName: 'T' } }));

        render(<LoginForm />, { wrapper: createWrapper() });

        // Set on the element only, as autofill does before the user touches the page.
        (screen.getByLabelText('User name') as HTMLInputElement).value = 'editor';
        (screen.getByLabelText('Password') as HTMLInputElement).value = 'secret';
        await user.click(screen.getByRole('button', { name: 'Log in' }));

        expect(fetchMock).toHaveBeenCalledWith('/rest/auth/login', expect.objectContaining({ body: JSON.stringify({ login: 'editor', password: 'secret' }) }));
    });

    it('posts the credentials', async () => {
        const fetchMock = stubLogin(Response.json({ responseInfo: { responseCode: 'OK' }, user: { id: 3, login: 'editor', firstName: 'E', lastName: 'T' } }));

        render(<LoginForm />, { wrapper: createWrapper() });
        await logIn('editor', 'secret');

        expect(fetchMock).toHaveBeenCalledWith('/rest/auth/login', expect.objectContaining({ body: JSON.stringify({ login: 'editor', password: 'secret' }) }));
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it.each([
        ['NOTFOUND', 'The user name or the password is wrong.'],
        ['MAINTENANCEMODE', 'The CMS is in maintenance mode. Logging in is not possible right now.'],
        ['FAILURE', 'The login failed.'],
    ])('says why a login with response code %s was refused', async (responseCode, message) => {
        stubLogin(Response.json({ responseInfo: { responseCode } }));

        render(<LoginForm />, { wrapper: createWrapper() });
        await logIn('editor', 'wrong');

        expect(await screen.findByRole('alert')).toHaveTextContent(message);
    });

    it('offers single sign-on as a button and redirects to Keycloak from it', async () => {
        const user = userEvent.setup();

        vi.mocked(loginWithKeycloak).mockResolvedValue();
        render(<LoginForm showSingleSignOn />, { wrapper: createWrapper() });

        await user.click(screen.getByRole('button', { name: 'Log in with SSO' }));

        expect(loginWithKeycloak).toHaveBeenCalled();
    });

    it('says when single sign-on failed', () => {
        render(<LoginForm singleSignOnError={new Error('unreachable')} />, { wrapper: createWrapper() });

        expect(screen.getByRole('alert')).toHaveTextContent('Single sign-on is not available.');
    });

    it.each([
        ['MAINTENANCEMODE', 'The CMS is in maintenance mode. Logging in is not possible right now.'],
        ['NOTFOUND', 'Single sign-on is not available.'],
    ])('says why the CMS refused a single sign-on with response code %s', (responseCode, message) => {
        render(<LoginForm singleSignOnError={new CmsLoginError(responseCode)} />, { wrapper: createWrapper() });

        expect(screen.getByRole('alert')).toHaveTextContent(message);
    });

    it('says why the CMS could not be asked for the session', () => {
        render(<LoginForm sessionError={new TypeError('Failed to fetch')} />, { wrapper: createWrapper() });

        expect(screen.getByRole('alert')).toHaveTextContent('The server could not be reached. Please check your connection.');
    });
});
