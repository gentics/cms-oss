import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { taskStartParts } from '@/helper/taskDisplay/taskDisplay';
import { cmsKeys, useCmsFeature } from '@/hooks/useCmsQueries';
import { useSessionStart } from '@/hooks/useSessionStart';
import {
    approvePubqueuePages,
    assignCmsPages,
    claimCmsTask,
    cmsPageUrl,
    type CmsTask,
    type CmsTaskCreate,
    cmsVersionCompareUrl,
    completeCmsTask,
    countBrokenLinkPages,
    createCmsTask,
    listCmsTasks,
    listCmsUsers,
    unclaimCmsTask,
} from '@/services/cmsApiService/cmsTaskApi';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';

export type TaskRequestAction = 'approve' | 'complete' | 'claim' | 'unclaim';

/** The open tasks of the current user. */
export function useCmsTasks() {
    return useQuery({ queryKey: cmsKeys.tasks(), queryFn: ({ signal }) => listCmsTasks(signal) });
}

/**
 * How many pages the user may edit have broken links, own link checker tasks included. Only with the link checker
 * on, since the count without a node does not check it; a failure shows nothing, the count is only a hint.
 */
export function useBrokenLinkPageCount() {
    const linkChecker = useCmsFeature('link_checker');

    return useQuery({
        queryKey: cmsKeys.brokenLinkPages(),
        queryFn: ({ signal }) => countBrokenLinkPages(signal),
        enabled: linkChecker.data === true,
        retry: false,
    });
}

/** The CMS users; not retried, since it fails without permission. */
export function useCmsUsers(enabled: boolean) {
    return useQuery({ queryKey: cmsKeys.users(), queryFn: ({ signal }) => listCmsUsers(signal), enabled, retry: false });
}

function pageId(task: CmsTask): number {
    const page = task.objects?.find((object) => object.type === 'page');

    if (!page) {
        throw new Error(`Task ${task.id} has no page`);
    }

    return page.id;
}

function runAction(task: CmsTask, action: TaskRequestAction): Promise<unknown> {
    switch (action) {
        case 'approve':
            return approvePubqueuePages([pageId(task)]);
        case 'complete':
            return completeCmsTask(task.id);
        case 'claim':
            return claimCmsTask(task.id);
        case 'unclaim':
            return unclaimCmsTask(task.id);
    }
}

/** Runs a task action; not retried, since approving twice would publish twice. */
export function useTaskAction() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ task, action }: { task: CmsTask; action: TaskRequestAction }) => runAction(task, action),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cmsKeys.tasks() }),
        onError: (error) => {
            useErrorNotificationStore.getState().addError({ messageKey: 'tasks.actionFailed', detailKey: errorMessageKey(error) });
        },
        retry: false,
    });
}

/** Creates a stored task; not retried, the caller reports a failure. */
export function useCreateCmsTask() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (task: CmsTaskCreate) => createCmsTask(task),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cmsKeys.tasks() }),
        retry: false,
    });
}

/** Hands the page of a publish queue task back for revision. */
export function useAssignBackTask() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ task, userIds, message }: { task: CmsTask; userIds: number[]; message: string }) => assignCmsPages({ pageIds: [pageId(task)], userIds, message }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: cmsKeys.tasks() }),
        onError: (error) => {
            useErrorNotificationStore.getState().addError({ messageKey: 'tasks.assignBack.failed', detailKey: errorMessageKey(error) });
        },
        retry: false,
    });
}

/** Opens the page of a task in the CMS UI; the tab opens before the await, or the browser blocks it. */
export async function openTaskInCms(task: CmsTask): Promise<void> {
    const page = task.objects?.find((object) => object.type === 'page');

    if (!page?.nodeId) {
        return;
    }

    const tab = window.open('', '_blank');

    if (!tab) {
        return;
    }

    tab.opener = null;
    tab.location.href = task.source === 'publishQueue' ? await cmsVersionCompareUrl(page.id, page.nodeId) : cmsPageUrl(page.id, page.nodeId);
}

/** Starts a GenAIx session from a task and opens it. */
export function useStartSessionFromTask() {
    const { t } = useTranslation();
    const { start, isStarting } = useSessionStart();

    return {
        startFromTask: (task: CmsTask) => start({ parts: taskStartParts(task, t), files: [] }),
        isStarting,
    };
}
