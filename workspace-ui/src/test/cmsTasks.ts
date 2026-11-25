import type { CmsUser } from '@/services/cmsApiService/cmsApiService';
import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';

/** The logged-in user `withTasks` answers with. */
export const TASK_USER: CmsUser = { id: 5, login: 'mm', firstName: 'Max', lastName: 'Mustermann' };

/** A task as `GET /rest/task` returns it; `overrides` sets what a test is about. */
export function taskFixture(overrides: Partial<CmsTask> & Pick<CmsTask, 'id'>): CmsTask {
    return {
        source: 'manual',
        type: 'custom',
        title: `Task ${overrides.id}`,
        status: 'open',
        actions: [],
        ...overrides,
    };
}

/**
 * Answers `GET /rest/task` with `tasks`, `GET /rest/user/me` with `TASK_USER`, and the link checker: off, unless
 * `brokenLinkPages` is given, then on with that many pages with broken links. Every other call goes to `fetchMock`.
 */
export function withTasks(fetchMock: typeof fetch, tasks: CmsTask[], { brokenLinkPages }: { brokenLinkPages?: number } = {}): typeof fetch {
    return async (input, init) => {
        const url = new URL(String(input), 'http://localhost');
        const isGet = (init?.method ?? 'GET') === 'GET';

        if (isGet && url.pathname === '/rest/user/me') {
            return Response.json({ user: TASK_USER });
        }

        if (isGet && url.pathname === '/rest/task') {
            return Response.json({ items: tasks, numItems: tasks.length, hasMoreItems: false });
        }

        if (isGet && url.pathname === '/rest/admin/features/link_checker') {
            return Response.json({ name: 'link_checker', activated: brokenLinkPages !== undefined });
        }

        if (isGet && url.pathname === '/rest/linkChecker/pages' && brokenLinkPages !== undefined) {
            return Response.json({ items: [], numItems: brokenLinkPages, hasMoreItems: brokenLinkPages > 0 });
        }

        return fetchMock(input, init);
    };
}
