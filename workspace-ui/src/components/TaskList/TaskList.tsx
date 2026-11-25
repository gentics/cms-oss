import { ExternalLinkIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useBrokenLinkPageCount, useCmsTasks } from '@/hooks/useCmsTasks';
import type { ContextReference } from '@/services/apiService/genaix/types';
import { CMS_LINK_CHECKER_URL, type CmsTask, type CmsTaskSource } from '@/services/cmsApiService/cmsTaskApi';

import { TaskItem } from './TaskItem';

import styles from './TaskList.module.css';

const GROUPS: { key: string; sources: CmsTaskSource[] }[] = [
    { key: 'genaix', sources: ['genaix'] },
    { key: 'publishQueue', sources: ['publishQueue'] },
    { key: 'linkChecker', sources: ['linkChecker'] },
    { key: 'assigned', sources: ['manual', 'job'] },
];

interface TaskListProps {
    /** Puts the objects of a publish queue task into the page's start composer, for the approver to say what to change. */
    onEditWithGenaix: (references: ContextReference[]) => void;
}

/**
 * The open tasks of the user, grouped by source, for the tasks drawer (`TaskDrawer`), which gives them their title;
 * nothing while loading. Below the link checker tasks, the other pages with broken links the user may edit, as a link
 * to the link checker: their tasks go only to who created or last edited a page.
 */
export function TaskList({ onEditWithGenaix }: TaskListProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const tasks = useCmsTasks();
    const brokenLinkPages = useBrokenLinkPageCount();

    if (tasks.isPending) {
        return null;
    }

    const groups = GROUPS.map(({ key, sources }) => ({
        key,
        items: tasks.data?.filter((task: CmsTask) => sources.includes(task.source)) ?? [],
    }));
    const ownBrokenLinks = groups.find(({ key }) => key === 'linkChecker')?.items.length ?? 0;
    // Not below 0: the tasks and the count are two requests, a page may have changed in between.
    const otherBrokenLinks = Math.max(0, (brokenLinkPages.data ?? 0) - ownBrokenLinks);
    const isEmpty = tasks.isSuccess && tasks.data.length === 0;

    if (isEmpty && otherBrokenLinks === 0) {
        return <p className={styles.empty}>{t('tasks.empty')}</p>;
    }

    return (
        <div className={styles.tasks}>
            {isEmpty && <p className={styles.empty}>{t('tasks.empty')}</p>}

            {tasks.isError && (
                <div className={styles.state} role="alert">
                    <p>{t(errorMessageKey(tasks.error))}</p>
                    <Button size="sm" onClick={() => void tasks.refetch()}>{t('sessions.retry')}</Button>
                </div>
            )}

            {groups.map(({ key, items }) => {
                const groupId = `${titleId}-${key}`;
                const others = key === 'linkChecker' ? otherBrokenLinks : 0;

                return (items.length > 0 || others > 0) && (
                    <section key={key} className={styles.group} aria-labelledby={groupId}>
                        <h3 id={groupId} className={styles.groupTitle}>{t(`tasks.groups.${key}`)}</h3>
                        {items.length > 0 && (
                            <ul className={styles.list}>
                                {items.map((task) => <li key={task.id}><TaskItem task={task} onEditWithGenaix={onEditWithGenaix} /></li>)}
                            </ul>
                        )}
                        {others > 0 && (
                            <div className={styles.more}>
                                <span>{t(ownBrokenLinks > 0 ? 'tasks.otherBrokenLinks' : 'tasks.brokenLinkPages', { count: others })}</span>
                                <a href={CMS_LINK_CHECKER_URL} target="_blank" rel="noopener noreferrer" className={styles.link}>
                                    <ExternalLinkIcon size={14} />
                                    {t('tasks.actions.openLinkChecker')}
                                </a>
                            </div>
                        )}
                    </section>
                );
            })}
        </div>
    );
}
