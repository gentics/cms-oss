import { PublishQueueNode } from '@gentics/cms-models';

export type PublishType = keyof Pick<PublishQueueNode, 'folders' | 'pages' | 'files' | 'forms'>;

export const PUBLISH_PLURAL_MAPPING: Record<PublishType, string> = {
    folders: 'folder',
    pages: 'page',
    files: 'file',
    forms: 'form',
};
