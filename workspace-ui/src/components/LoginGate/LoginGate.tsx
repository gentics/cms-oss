import type { ReactNode } from 'react';

import { LoginForm } from '@/components/LoginForm/LoginForm';
import { useCmsUser, useSingleSignOn } from '@/hooks/useCmsAuth';

import styles from './LoginGate.module.css';

/**
 * Shows `children` only with a CMS session, as the editor and admin UI do: it asks the CMS for the
 * user of the session cookie (`GET /rest/user/me`). Without one it tries single sign-on through
 * Keycloak, which may leave the page for the Keycloak login, and otherwise shows the login form.
 */
export function LoginGate({ children }: { children: ReactNode }) {
    const user = useCmsUser();
    const loggedOut = user.isSuccess && user.data === null;
    const singleSignOn = useSingleSignOn(loggedOut);

    if (user.data) {
        return children;
    }

    // The CMS could not be asked (e.g. it cannot be reached): the form, saying why.
    if (user.isError) {
        return <LoginForm sessionError={user.error} />;
    }

    // Single sign-on is done or failed: the form. After a single sign-on that created a session the
    // user is fetched before it settles, so without one here the session is gone (expired, or its
    // cookie was not kept); single sign-on runs once per page load, so the button starts it again.
    if (loggedOut && (singleSignOn.isSuccess || singleSignOn.isError)) {
        return (
            <LoginForm
                showSingleSignOn={singleSignOn.data?.showButton || singleSignOn.data?.loggedIn}
                singleSignOnError={singleSignOn.error ?? undefined}
            />
        );
    }

    // Asking the CMS, or single sign-on on its way: an empty canvas, without flashing the form.
    return <div className={styles.waiting} aria-busy="true" />;
}
