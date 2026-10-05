import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { useToast } from '@/components/ui/use-toast';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

/** The message of an error, translated again when the language changes. */
function ErrorMessage({ messageKey }: { messageKey: string }) {
    const { t } = useTranslation();

    return t(messageKey);
}

/**
 * Shows every error in `useErrorNotificationStore` as an error toast (`@/components/ui/toast`) that
 * stays until it is dismissed. Dismissing the toast removes the error from the store, and an error
 * removed from the store closes its toast. Needs `UiProvider` around it.
 */
export function ErrorNotifications() {
    const toast = useToast();
    const errors = useErrorNotificationStore((state) => state.errors);
    const dismissError = useErrorNotificationStore((state) => state.dismissError);
    const shownIds = useRef(new Set<string>());

    useEffect(() => {
        const currentIds = new Set(errors.map((error) => error.id));

        for (const error of errors) {
            if (!shownIds.current.has(error.id)) {
                shownIds.current.add(error.id);
                toast.add({
                    id: error.id,
                    type: 'error',
                    // Announced by the toast region (`aria-live="polite"`). Not `priority: 'high'`:
                    // Base UI then sets `aria-hidden` on the focusable toast.
                    priority: 'low',
                    timeout: 0,
                    title: <ErrorMessage messageKey={error.messageKey} />,
                    description: error.detail,
                    onClose: () => dismissError(error.id),
                });
            }
        }

        for (const id of shownIds.current) {
            if (!currentIds.has(id)) {
                shownIds.current.delete(id);
                toast.close(id);
            }
        }
    }, [errors, toast, dismissError]);

    return null;
}
