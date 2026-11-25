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

/** A stored task to create (`POST /rest/task`); `source` defaults to `manual`, `assignee` to the creator. */
export interface CmsTaskCreate {
    type: string;
    title: string;
    description?: string;
    assignee?: Pick<CmsTaskAssignee, 'type' | 'id'>;
    objects?: CmsTaskObject[];
    details?: Record<string, unknown>;
    /** With the same source, the CMS updates the task that has it instead of creating another. */
    externalKey?: string;
    genaix?: { instruction?: string };
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

/** Creates a stored task, or updates the one with the same `externalKey`. */
export async function createCmsTask(task: CmsTaskCreate): Promise<CmsTask> {
    const { task: created } = await cmsWrite<CmsResponse & { task: CmsTask }>('/rest/task', 'POST', task);

    return created;
}

/**
 * The CMS group that construct requests go to (`VITE_CONSTRUCT_REQUEST_GROUP_ID`, `.env.example`); read on every
 * call, not once. Anything but a positive integer counts as none.
 */
export function constructRequestGroupId(): number | undefined {
    const value = Number(import.meta.env.VITE_CONSTRUCT_REQUEST_GROUP_ID);

    return Number.isInteger(value) && value > 0 ? value : undefined;
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

/**
 * How many pages the user may edit have broken links (`GET /rest/linkChecker/pages`); `numItems` counts after the
 * permission filter, and `pageSize=0` leaves the pages out.
 */
export async function countBrokenLinkPages(signal?: AbortSignal): Promise<number> {
    const params = new URLSearchParams({ editable: 'true', status: 'invalid', pageSize: '0' });
    const { numItems } = await httpRequest<{ numItems?: number }>(`/rest/linkChecker/pages?${params}`, { ...CMS_GET, signal });

    return numItems ?? 0;
}

/** The link checker in the CMS UI: the custom tool, whose key must be `linkchecker` (`cms-ui/apps/ct-link-checker/README.md`). */
export const CMS_LINK_CHECKER_URL = '/editor/#/tools/linkchecker';

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
