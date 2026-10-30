import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { leaveAfterLogout, startSingleSignOn } from '@/helper/keycloak/keycloak';
import { getCmsUiLanguage, getCmsUser, loginToCms, logoutFromCms } from '@/services/cmsApiService/cmsApiService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

/** Query key of the logged-in CMS user (`GET /rest/user/me`); `null` data means not logged in. */
export const CMS_USER_QUERY_KEY = ['cms', 'user'] as const;

const SINGLE_SIGN_ON_QUERY_KEY = ['cms', 'singleSignOn'] as const;

const UI_LANGUAGE_QUERY_KEY = ['cms', 'uiLanguage'] as const;

// The languages the Workspace has (`src/i18n/locales`).
const UI_LANGUAGES = ['de', 'en'];

/**
 * The CMS user of the browser's CMS session cookie, `null` without a valid session. Fetched again
 * when the window regains focus, so an expired session leads back to the login. `refetchOnMount:
 * false` reads it from the cache when a component mounts, e.g. on a page inside `LoginGate`, which
 * has loaded it already.
 */
export function useCmsUser({ refetchOnMount }: { refetchOnMount?: boolean } = {}) {
    return useQuery({ queryKey: CMS_USER_QUERY_KEY, queryFn: getCmsUser, retry: false, refetchOnMount });
}

/**
 * Single sign-on through Keycloak, started once `enabled` (while there is no CMS session). When it
 * created a CMS session, the CMS user is fetched again.
 */
export function useSingleSignOn(enabled: boolean) {
    const queryClient = useQueryClient();

    return useQuery({
        queryKey: SINGLE_SIGN_ON_QUERY_KEY,
        queryFn: async () => {
            const state = await startSingleSignOn();

            if (state.loggedIn) {
                await queryClient.invalidateQueries({ queryKey: CMS_USER_QUERY_KEY });
            }

            return state;
        },
        enabled,
        // Keycloak is set up once per page load.
        staleTime: Infinity,
        gcTime: Infinity,
        retry: false,
    });
}

/** Logs in with user name and password; on success the CMS user is the one returned. */
export function useCmsLogin() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ login, password }: { login: string; password: string }) => loginToCms(login, password),
        onSuccess: (user) => queryClient.setQueryData(CMS_USER_QUERY_KEY, user),
    });
}

/**
 * Logs out of the CMS and of Keycloak too; then the page starts fresh and shows the login
 * (`leaveAfterLogout`). A failed CMS logout shows an error notification. When only the Keycloak
 * logout fails, the CMS session is gone already: the login is shown, with a notification that the
 * single sign-on login is still there.
 */
export function useCmsLogout() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            await logoutFromCms();

            try {
                await leaveAfterLogout();
            } catch (error) {
                queryClient.setQueryData(CMS_USER_QUERY_KEY, null);
                useErrorNotificationStore.getState().addError({ messageKey: 'login.errors.singleSignOnLogout', detailKey: errorMessageKey(error) });
            }
        },
        onError: (error) => {
            useErrorNotificationStore.getState().addError({ messageKey: 'login.errors.logout', detailKey: errorMessageKey(error) });
        },
    });
}

/**
 * Takes the UI language of the CMS session (`getCmsUiLanguage`), once per page load after `enabled`
 * (a CMS session exists): `de` or `en` becomes the Workspace's language and the page's `lang`. Another
 * language, or an error, leaves the current one (German until then, `src/i18n`). Each answer is applied
 * once: a language the user switches to afterwards is not switched back.
 */
export function useCmsUiLanguage(enabled: boolean) {
    const { i18n } = useTranslation();
    const language = useQuery({ queryKey: UI_LANGUAGE_QUERY_KEY, queryFn: getCmsUiLanguage, enabled, staleTime: Infinity, retry: false });

    // The answer applied last; the effect runs again when the UI language changes.
    const appliedRef = useRef<string | undefined>(undefined);

    useEffect(() => {
        const code = language.data;

        if (!code || code === appliedRef.current) {
            return;
        }

        appliedRef.current = code;

        if (UI_LANGUAGES.includes(code) && code !== i18n.language) {
            document.documentElement.lang = code;
            void i18n.changeLanguage(code);
        }
    }, [language.data, i18n]);
}
