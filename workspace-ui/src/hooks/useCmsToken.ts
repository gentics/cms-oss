import { useQuery } from '@tanstack/react-query';

import { getCmsToken } from '@/services/cmsApiService/cmsApiService';
import { HttpError } from '@/services/httpService/httpService';
import type { CmsToken } from '@/store/useCmsTokenStore';

/** TanStack Query's default number of retries, kept for the retries that are allowed. */
const MAX_RETRIES = 3;

/** What the query exposes about the CMS token. The secret itself stays in `useCmsTokenStore`. */
export type CmsTokenStatus = Omit<CmsToken, 'token'>;

/**
 * Makes sure this page session has a CMS API token: the one in `useCmsTokenStore`, or a new one
 * (`getCmsToken`). The query result leaves out the secret, so it never lands in the query cache or
 * the React Query Devtools.
 */
export function useCmsToken() {
    return useQuery({
        queryKey: ['cmsToken'],
        queryFn: async (): Promise<CmsTokenStatus> => {
            const { id, name, expires } = await getCmsToken();

            return { id, name, expires };
        },
        // Only a response that is not 200 is retried (`httpRequest` throws `HttpError` for those).
        // A 200 whose body can't be read (for example not JSON) is not: the CMS may already have
        // created a token, and a retry would create another. Network errors carry no response
        // code and are not retried.
        retry: (failureCount, error) => error instanceof HttpError && failureCount < MAX_RETRIES,
        // One notification once the last retry has failed (`createQueryClient`).
        meta: { errorMessageKey: 'errorNotifications.cmsTokenFailed' },
    });
}
