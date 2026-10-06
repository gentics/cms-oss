import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import i18n from '@/i18n';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import { ErrorNotifications } from './ErrorNotifications';

function addError(messageKey: string, detail?: string) {
    act(() => {
        useErrorNotificationStore.getState().addError({ messageKey, detail });
    });
}

function renderNotifications() {
    return render(<ErrorNotifications />, { wrapper: UiProvider });
}

/** The toast region, a polite live region that announces what is added to it. */
function notifications() {
    return screen.getByRole('region', { name: 'Notifications' });
}

function errorToasts() {
    return notifications().querySelectorAll('[data-slot="toast"][data-type="error"]');
}

describe('ErrorNotifications', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(async () => {
        vi.restoreAllMocks();
        await i18n.changeLanguage('en');
    });

    it('shows nothing while there is no error', () => {
        renderNotifications();

        expect(errorToasts()).toHaveLength(0);
    });

    it('shows the translated message and the detail in the live region', async () => {
        renderNotifications();

        addError('errorNotifications.cmsTokenFailed', 'Request to /rest/admin/token failed with status 403');

        const toast = await within(notifications()).findByRole('dialog', { name: 'Getting the CMS token failed' });

        expect(notifications()).toHaveAttribute('aria-live', 'polite');
        expect(toast).toHaveAttribute('data-type', 'error');
        expect(toast).toHaveAccessibleDescription('Request to /rest/admin/token failed with status 403');
    });

    it('shows the detail translated when it is given as a key', async () => {
        renderNotifications();

        act(() => {
            useErrorNotificationStore.getState().addError({
                messageKey: 'errorNotifications.cmsTokenFailed',
                detailKey: 'errors.cms.PERMISSION',
            });
        });

        const toast = await within(notifications()).findByRole('dialog', { name: 'Getting the CMS token failed' });

        expect(toast).toHaveAccessibleDescription('You don\'t have permission for this in the CMS.');
    });

    it('shows one error toast per error', async () => {
        renderNotifications();

        addError('errorNotifications.cmsTokenFailed', 'first');
        addError('errorNotifications.cmsTokenFailed', 'second');

        await waitFor(() => expect(errorToasts()).toHaveLength(2));
    });

    it('dismisses only the toast whose close button was clicked and removes its error', async () => {
        const user = userEvent.setup();

        renderNotifications();

        addError('errorNotifications.cmsTokenFailed', 'first');
        addError('errorNotifications.cmsTokenFailed', 'second');

        const first = await screen.findByText('first', { selector: '[data-slot="toast-description"]' });
        const toast = first.closest<HTMLElement>('[data-slot="toast"]')!;

        await user.click(toast.querySelector<HTMLElement>('[data-slot="toast-close"]')!);

        await waitFor(() => expect(screen.queryByText('first', { selector: '[data-slot="toast-description"]' })).not.toBeInTheDocument());
        expect(screen.getByText('second', { selector: '[data-slot="toast-description"]' })).toBeInTheDocument();
        expect(useErrorNotificationStore.getState().errors.map((error) => error.detail)).toEqual(['second']);
    });

    it('closes the toast when its error is removed from the store', async () => {
        renderNotifications();

        addError('errorNotifications.cmsTokenFailed', 'first');
        await screen.findByText('first');

        act(() => {
            const [error] = useErrorNotificationStore.getState().errors;
            useErrorNotificationStore.getState().dismissError(error!.id);
        });

        await waitFor(() => expect(screen.queryByText('first')).not.toBeInTheDocument());
    });

    it('translates the message again when the language changes', async () => {
        renderNotifications();

        addError('errorNotifications.cmsTokenFailed');
        await screen.findByText('Getting the CMS token failed');

        await act(async () => {
            await i18n.changeLanguage('de');
        });

        expect(screen.getByText('Der CMS-Token konnte nicht abgerufen werden')).toBeInTheDocument();
    });

    it('shows no notification for a plain console.error', async () => {
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        renderNotifications();

        await act(async () => {
            console.error('unrelated error');
        });

        expect(errorSpy).toHaveBeenCalledWith('unrelated error');
        expect(errorToasts()).toHaveLength(0);
    });
});
