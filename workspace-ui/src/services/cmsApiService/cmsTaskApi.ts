import { HttpError, httpRequest } from '@/services/httpService/httpService';

export type CmsTaskSource = 'manual' | 'job' | 'genaix' | 'publishQueue' | 'linkChecker';

export type CmsTaskStatus = 'open' | 'inProgress' | 'done' | 'dismissed';

export type CmsTaskAction = 'open' | 'view' | 'approve' | 'assignBack' | 'complete' | 'dismiss' | 'edit' | 'delete' | 'startSession' | 'claim' | 'unclaim';

export interface CmsTaskAssignee {
    type: 'user' | 'group';
    id: number;
    name?: string;
}

/** A CMS object of a task. */
export interface CmsTaskObject {
    type: string;
    id: number;
    nodeId?: number;
    name?: string;
}

/** A task of `GET /rest/task` (`UserTaskModel.java`). */
export interface CmsTask {
    id: string;
    source: CmsTaskSource;
    type: string;
    title?: string;
    description?: string;
    status: CmsTaskStatus;
    /** Unix timestamp in seconds. */
    dueAt?: number | null;
    assignee?: CmsTaskAssignee | null;
    assigneeGroup?: CmsTaskAssignee | null;
    creator?: { type: 'user' | 'system'; id?: number; name?: string };
    objects?: CmsTaskObject[];
    details?: Record<string, unknown>;
    actions: CmsTaskAction[];
    genaix?: { sessionId?: string; planId?: string; instruction?: string };
}

export interface CmsUserItem {
    id: number;
    login?: string;
    firstName?: string;
    lastName?: string;
}

interface CmsPageVersion {
    number: string;
    timestamp: number;
}

const CMS_GET: RequestInit = { credentials: 'same-origin', headers: { Accept: 'application/json' } };

type CmsResponse = { responseInfo?: { responseCode?: string } };

// The CMS refuses some writes with `200` and `responseInfo.responseCode`; thrown as `HttpError` so `errorMessageKey` maps it.
async function cmsWrite<T extends CmsResponse = CmsResponse>(url: string, method: string, body?: unknown): Promise<T> {
    const response = await httpRequest<T>(url, {
        method,
        credentials: 'same-origin',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (response.responseInfo?.responseCode !== undefined && response.responseInfo.responseCode !== 'OK') {
        throw new HttpError(url, 200, response);
    }

    return response;
}

/** The open tasks of the current user. */
export async function listCmsTasks(signal?: AbortSignal): Promise<CmsTask[]> {
    const { items } = await httpRequest<{ items?: CmsTask[] }>('/rest/task', { ...CMS_GET, signal });

    return items ?? [];
}

/** Sets a task done. */
export async function completeCmsTask(id: string): Promise<CmsTask> {
    const { task } = await cmsWrite<CmsResponse & { task: CmsTask }>(`/rest/task/${encodeURIComponent(id)}`, 'PUT', { status: 'done' });

    return task;
}

/** Assigns a group task to the current user. */
export async function claimCmsTask(id: string): Promise<CmsTask> {
    const { task } = await cmsWrite<CmsResponse & { task: CmsTask }>(`/rest/task/${encodeURIComponent(id)}/claim`, 'POST');

    return task;
}

/** Gives a claimed task back to its group. */
export async function unclaimCmsTask(id: string): Promise<CmsTask> {
    const { task } = await cmsWrite<CmsResponse & { task: CmsTask }>(`/rest/task/${encodeURIComponent(id)}/unclaim`, 'POST');

    return task;
}

/** Approves pages in the publish queue. */
export async function approvePubqueuePages(pageIds: number[]): Promise<void> {
    await cmsWrite('/rest/page/pubqueue/approve', 'POST', { ids: pageIds });
}

/** Hands pages back for revision. */
export async function assignCmsPages({ pageIds, userIds, message }: { pageIds: number[]; userIds: number[]; message: string }): Promise<void> {
    await cmsWrite('/rest/page/assign', 'POST', { pageIds: pageIds.map(String), userIds, message });
}

/** The CMS users; needs the admin view permission on users. */
export async function listCmsUsers(signal?: AbortSignal): Promise<CmsUserItem[]> {
    const { items } = await httpRequest<{ items?: CmsUserItem[] }>('/rest/user', { ...CMS_GET, signal });

    return items ?? [];
}

/** The page in the CMS UI. */
export function cmsPageUrl(pageId: number, nodeId: number, editMode = 'preview', options: object = {}): string {
    const encoded = encodeURIComponent(btoa(JSON.stringify(options)));

    return `/editor/#/editor/(detail:node/${nodeId}/page/${pageId}/${editMode};options=${encoded})`;
}

/** The CMS UI version comparison of a page, else its preview. */
export async function cmsVersionCompareUrl(pageId: number, nodeId: number): Promise<string> {
    const detail = (editMode: string, options: object) => cmsPageUrl(pageId, nodeId, editMode, options);

    try {
        const params = new URLSearchParams({ nodeId: String(nodeId), versioninfo: 'true' });
        const { page } = await httpRequest<{ page: { currentVersion?: CmsPageVersion; publishedVersion?: CmsPageVersion } }>(
            `/rest/page/load/${pageId}?${params}`,
            CMS_GET,
        );
        const oldVersion = page.publishedVersion ?? { timestamp: 0, number: '0.0' };

        return page.currentVersion ? detail('compareVersionContents', { version: page.currentVersion, oldVersion }) : detail('preview', {});
    } catch {
        return detail('preview', {});
    }
}
