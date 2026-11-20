import type { TFunction } from 'i18next';

import { formatSessionDate } from '@/helper/sessionDisplay/sessionDisplay';
import type { ContextReference, UserMessagePart } from '@/services/apiService/genaix/types';
import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';

// Types with a translated name; other stored tasks bring their own title.
const NAMED_TYPES = new Set(['approvePublish', 'approveOffline', 'fixBrokenLinks', 'genaixReadyToPublish', 'requestConstruct']);

const REFERENCE_TYPES = new Set<ContextReference['type']>(['node', 'folder', 'page', 'file', 'image', 'template']);

function isReferenceType(type: string): type is ContextReference['type'] {
    return REFERENCE_TYPES.has(type as ContextReference['type']);
}

function objectName(task: CmsTask): string {
    return task.objects?.[0]?.name ?? '';
}

/** The broken URLs of a link checker task. */
export function brokenUrls(task: CmsTask): string[] {
    const links = task.details?.links;

    if (!Array.isArray(links)) {
        return [];
    }

    return links.flatMap((link: unknown) => (typeof link === 'object' && link !== null && 'url' in link && typeof link.url === 'string' ? [link.url] : []));
}

/** Who asked to publish, if known. */
export function requestedBy(task: CmsTask): { id: number; name: string } | undefined {
    const value = task.details?.requestedBy;

    if (typeof value === 'object' && value !== null && 'id' in value && typeof value.id === 'number') {
        return { id: value.id, name: 'name' in value && typeof value.name === 'string' ? value.name : '' };
    }

    return undefined;
}

/** The title of a task. */
export function taskTitle(task: CmsTask, t: TFunction): string {
    if (task.title) {
        return task.title;
    }

    return NAMED_TYPES.has(task.type) ? t(`tasks.name.${task.type}`, { name: objectName(task) }) : objectName(task);
}

/** The meta line of a task. */
export function taskMeta(task: CmsTask, t: TFunction, language: string): string {
    const parts: string[] = [];
    const requester = requestedBy(task);

    if (requester?.name) {
        parts.push(t('tasks.meta.requestedBy', { name: requester.name }));
    }

    if (task.source === 'linkChecker') {
        parts.push(t('tasks.meta.brokenLinks', { count: brokenUrls(task).length }));
    }

    if (task.assignee?.type === 'group') {
        parts.push(t('tasks.meta.forGroup', { name: task.assignee.name ?? '' }));
    } else if (task.assigneeGroup && task.assignee?.type === 'user') {
        parts.push(t('tasks.meta.claimedBy', { name: task.assignee.name ?? '' }));
    }

    if (task.dueAt) {
        parts.push(t('tasks.meta.due', { date: formatSessionDate(new Date(task.dueAt * 1000).toISOString(), language) }));
    }

    return parts.join(' · ');
}

/** The objects of a task as GenAIx references. */
export function taskReferences(task: CmsTask): ContextReference[] {
    return (task.objects ?? []).flatMap((object) => (isReferenceType(object.type)
        ? [{ type: object.type, id: String(object.id), node_id: object.nodeId, label: object.name }]
        : []));
}

/** The first message of a session started from a task. */
export function taskStartParts(task: CmsTask, t: TFunction): UserMessagePart[] {
    const instruction = task.genaix?.instruction
        ?? ([task.title, task.description].filter(Boolean).join('\n\n')
            || t(`tasks.instruction.${task.type === 'fixBrokenLinks' ? 'fixBrokenLinks' : 'default'}`, { urls: brokenUrls(task).join('\n') }));
    const references: UserMessagePart[] = taskReferences(task).map((ref) => ({ type: 'reference', ref }));

    return [...references, { type: 'text', text: instruction }];
}
