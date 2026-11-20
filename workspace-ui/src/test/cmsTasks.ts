import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';

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

/** Answers `GET /rest/task` with `tasks`, every other call goes to `fetchMock`. */
export function withTasks(fetchMock: typeof fetch, tasks: CmsTask[]): typeof fetch {
    return async (input, init) => {
        const url = new URL(String(input), 'http://localhost');

        if ((init?.method ?? 'GET') === 'GET' && url.pathname === '/rest/task') {
            return Response.json({ items: tasks, numItems: tasks.length, hasMoreItems: false });
        }

        return fetchMock(input, init);
    };
}
