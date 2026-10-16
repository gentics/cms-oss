import type { TFunction } from 'i18next';

import type { Session } from '@/services/apiService/genaix/types';

/** The name a session is shown with: its title, else its intent, else "Untitled session". */
export function sessionTitle(session: Pick<Session, 'title' | 'intent'>, t: TFunction): string {
    return session.title?.trim() || session.intent?.trim() || t('sessions.untitled');
}

/**
 * The day of an ISO timestamp in a list row, e.g. "23 Sep"; with the year when it is not the
 * current one, e.g. "23 Sep 2025". `now` is for tests.
 */
export function formatSessionDate(iso: string, language: string, now = new Date()): string {
    const date = new Date(iso);
    const sameYear = date.getFullYear() === now.getFullYear();

    return new Intl.DateTimeFormat(language, {
        day: 'numeric',
        month: 'short',
        year: sameYear ? undefined : 'numeric',
    }).format(date);
}
