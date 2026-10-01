import { QueryCache, QueryClient, type QueryClientConfig } from '@tanstack/react-query';

import { errorMessageKey } from '@/services/errorMapper/errorMapper';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

declare module '@tanstack/react-query' {
    interface Register {
        queryMeta: {
            /**
             * Opts the query into an error notification: the i18n key of its message, for example
             * `errorNotifications.cmsTokenFailed`. The detail is the error mapped by `errorMessageKey`.
             */
            errorMessageKey?: string;
        };
    }
}

/**
 * The app's TanStack Query client. A query that sets `meta.errorMessageKey` shows an error
 * notification once it has failed for good, after its last retry. `config` is for tests, e.g.
 * `retryDelay: 0`.
 */
export function createQueryClient(config?: QueryClientConfig): QueryClient {
    return new QueryClient({
        ...config,
        queryCache: new QueryCache({
            onError: (error, query) => {
                const messageKey = query.meta?.errorMessageKey;

                if (messageKey) {
                    useErrorNotificationStore.getState().addError({ messageKey, detailKey: errorMessageKey(error) });
                }
            },
        }),
    });
}
