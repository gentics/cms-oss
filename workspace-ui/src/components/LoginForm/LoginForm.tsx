import { CircleAlertIcon, KeyRoundIcon, LogInIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { loginWithKeycloak } from '@/helper/keycloak/keycloak';
import { useCmsLogin } from '@/hooks/useCmsAuth';
import { CmsLoginError } from '@/services/cmsApiService/cmsApiService';

import styles from './LoginForm.module.css';

/** The i18n key of the message for a login that did not succeed. */
function loginErrorKey(error: unknown): string {
    if (error instanceof CmsLoginError) {
        switch (error.responseCode) {
            case 'NOTFOUND':
                return 'login.errors.invalidCredentials';
            case 'MAINTENANCEMODE':
                return 'login.errors.maintenance';
            default:
                return 'login.errors.failed';
        }
    }

    return errorMessageKey(error);
}

/**
 * The i18n key of the message for a single sign-on that did not succeed. `/rest/auth/ssologin` answers
 * `NOTFOUND` for a Keycloak login without a CMS session, so only maintenance mode has its own message.
 */
function singleSignOnErrorKey(error: unknown): string {
    return error instanceof CmsLoginError && error.responseCode === 'MAINTENANCEMODE' ? 'login.errors.maintenance' : 'login.errors.singleSignOn';
}

interface LoginFormProps {
    /** Offers single sign-on as a button (`keycloak.show_sso_button`). */
    showSingleSignOn?: boolean;
    /** The error single sign-on ended with, shown until the user logs in another way. */
    singleSignOnError?: unknown;
    /** The error asking the CMS for the session ended with (e.g. the CMS cannot be reached). */
    sessionError?: unknown;
}

/**
 * The login of the workspace, as in the editor and admin UI: user name and password
 * (`POST /rest/auth/login`) and, when the CMS offers it, single sign-on through Keycloak.
 */
export function LoginForm({ showSingleSignOn = false, singleSignOnError, sessionError }: LoginFormProps) {
    const { t } = useTranslation();
    const id = useId();
    const [redirectError, setRedirectError] = useState<unknown>();
    const login = useCmsLogin();
    const singleSignOnFailure = redirectError ?? singleSignOnError;
    const errorKey = login.error
        ? loginErrorKey(login.error)
        : singleSignOnFailure
            ? singleSignOnErrorKey(singleSignOnFailure)
            : sessionError
                ? errorMessageKey(sessionError)
                : undefined;

    // Reads the fields from the form, not from React state: values the browser filled in itself
    // (autofill) may not have sent an input event yet.
    function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const form = event.currentTarget;
        const fields = new FormData(form);
        const username = String(fields.get('username') ?? '');
        const password = String(fields.get('password') ?? '');

        if (username && password) {
            login.mutate({ login: username, password });
        } else {
            form.querySelector<HTMLInputElement>(username ? '[name="password"]' : '[name="username"]')?.focus();
        }
    }

    function loginWithSingleSignOn() {
        loginWithKeycloak().catch(setRedirectError);
    }

    return (
        <main className={styles.page}>
            <form className={styles.card} aria-labelledby={`${id}-title`} onSubmit={submit} noValidate>
                <h1 id={`${id}-title`} className={styles.title}>
                    <i className={styles.logo} aria-hidden="true" />
                    {t('topbar.brand')}
                </h1>

                {errorKey && (
                    <p className={styles.error} role="alert">
                        <CircleAlertIcon size={16} aria-hidden />
                        {t(errorKey)}
                    </p>
                )}

                <div className={styles.field}>
                    <Label htmlFor={`${id}-username`}>{t('login.username')}</Label>
                    <Input
                        id={`${id}-username`}
                        name="username"
                        autoComplete="username"
                        autoFocus
                    />
                </div>

                <div className={styles.field}>
                    <Label htmlFor={`${id}-password`}>{t('login.password')}</Label>
                    <Input
                        id={`${id}-password`}
                        name="password"
                        type="password"
                        autoComplete="current-password"
                    />
                </div>

                <Button type="submit" variant="primary" disabled={login.isPending}>
                    <LogInIcon size={16} aria-hidden />
                    {t('login.submit')}
                </Button>

                {showSingleSignOn && (
                    <Button type="button" variant="secondary" onClick={loginWithSingleSignOn}>
                        <KeyRoundIcon size={16} aria-hidden />
                        {t('login.singleSignOn')}
                    </Button>
                )}
            </form>
        </main>
    );
}
