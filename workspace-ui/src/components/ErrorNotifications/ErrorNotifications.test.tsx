import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

import { ErrorNotifications } from './ErrorNotifications';

import '@/i18n';

function addError(messageKey: string, detail?: string) {
    act(() => {
        useErrorNotificationStore.getState().addError({ messageKey, detail });
    });
}

describe('ErrorNotifications', () => {
    beforeEach(() => {
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('renders nothing while there is no error', () => {
        render(<ErrorNotifications />);

        expect(screen.queryByRole('region', { name: 'Errors' })).not.toBeInTheDocument();
    });

    it('shows the translated message and the detail', () => {
        render(<ErrorNotifications />);

        addError('errorNotifications.cmsTokenFailed', 'Request to /rest/admin/token failed with status 403');

        const region = screen.getByRole('region', { name: 'Errors' });
        const alert = within(region).getByRole('alert');

        expect(within(alert).getByText('Getting the CMS token failed')).toBeInTheDocument();
        expect(within(alert).getByText('Request to /rest/admin/token failed with status 403')).toBeInTheDocument();
    });

    it('stacks one notification per error, newest last', () => {
        render(<ErrorNotifications />);

        addError('errorNotifications.cmsTokenFailed', 'first');
        addError('errorNotifications.cmsTokenFailed', 'second');

        const alerts = screen.getAllByRole('alert');

        expect(alerts).toHaveLength(2);
        expect(alerts[0]).toHaveTextContent('first');
        expect(alerts[1]).toHaveTextContent('second');
    });

    it('dismisses only the notification whose close button was clicked', async () => {
        const user = userEvent.setup();

        render(<ErrorNotifications />);

        addError('errorNotifications.cmsTokenFailed', 'first');
        addError('errorNotifications.cmsTokenFailed', 'second');

        await user.click(screen.getAllByRole('button', { name: 'Dismiss' })[0]!);

        const alerts = screen.getAllByRole('alert');

        expect(alerts).toHaveLength(1);
        expect(alerts[0]).toHaveTextContent('second');
    });

    it('shows no notification for a plain console.error', async () => {
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

        render(<ErrorNotifications />);

        await act(async () => {
            console.error('unrelated error');
        });

        expect(errorSpy).toHaveBeenCalledWith('unrelated error');
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
