import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { leaveAfterLogout, startSingleSignOn } from '@/helper/keycloak/keycloak';
import { getCmsUser, loginToCms, logoutFromCms } from '@/services/cmsApiService/cmsApiService';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

/** Query key of the logged-in CMS user (`GET /rest/user/me`); `null` data means not logged in. */
export const CMS_USER_QUERY_KEY = ['cms', 'user'] as const;

const SINGLE_SIGN_ON_QUERY_KEY = ['cms', 'singleSignOn'] as const;

/**
 * The CMS user of the browser's CMS session cookie, `null` without a valid session. Fetched again
 * when the window regains focus, so an expired session leads back to the login.
 */
export function useCmsUser() {
    return useQuery({ queryKey: CMS_USER_QUERY_KEY, queryFn: getCmsUser, retry: false });
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
