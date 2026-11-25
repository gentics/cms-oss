import { BadgeCheckIcon, ChevronDownIcon, ExternalLinkIcon, type LucideIcon, RocketIcon, UnlinkIcon, UserIcon, UsersIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useBrokenLinkPageCount, useCmsTasks } from '@/hooks/useCmsTasks';
import type { ContextReference } from '@/services/apiService/genaix/types';
import { CMS_LINK_CHECKER_URL, type CmsTask, type CmsTaskSource } from '@/services/cmsApiService/cmsTaskApi';

import { TaskItem } from './TaskItem';

import styles from './TaskList.module.css';

const fromSources = (...sources: CmsTaskSource[]) => (task: CmsTask) => sources.includes(task.source);
const isStored = fromSources('manual', 'job');

// Stored tasks for the user, or claimed by the user, apart from those for a group nobody has taken yet.
const GROUPS: { key: string; icon: LucideIcon; includes: (task: CmsTask) => boolean }[] = [
    { key: 'genaix', icon: RocketIcon, includes: fromSources('genaix') },
    { key: 'publishQueue', icon: BadgeCheckIcon, includes: fromSources('publishQueue') },
    { key: 'linkChecker', icon: UnlinkIcon, includes: fromSources('linkChecker') },
    { key: 'assigned', icon: UserIcon, includes: (task) => isStored(task) && task.assignee?.type !== 'group' },
    { key: 'group', icon: UsersIcon, includes: (task) => isStored(task) && task.assignee?.type === 'group' },
];

interface TaskListProps {
    /** Puts the objects of a publish queue task into the page's start composer, for the approver to say what to change. */
    onEditWithGenaix: (references: ContextReference[]) => void;
}

/**
 * The open tasks of the user, grouped by source, and stored tasks by whether they are the user's or still their
 * group's, for the tasks drawer (`TaskDrawer`), which gives them their title;
 * nothing while loading. Below the link checker tasks, the other pages with broken links the user may edit, as a link
 * to the link checker: their tasks go only to who created or last edited a page. Each group folds in and out by its
 * heading, as "Recent sessions" does (`RecentSessions`); the region holds only the tasks.
 */
export function TaskList({ onEditWithGenaix }: TaskListProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const tasks = useCmsTasks();
    const brokenLinkPages = useBrokenLinkPageCount();
    const [foldedKeys, setFoldedKeys] = useState<string[]>([]);
    const toggle = (key: string) => setFoldedKeys((keys) => (keys.includes(key) ? keys.filter((other) => other !== key) : [...keys, key]));

    if (tasks.isPending) {
        return null;
    }

    const groups = GROUPS.map(({ key, icon, includes }) => ({
        key,
        icon,
        items: tasks.data?.filter(includes) ?? [],
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

            {groups.map(({ key, icon: Icon, items }) => {
                const groupId = `${titleId}-${key}`;
                const contentId = `${groupId}-tasks`;
                const others = key === 'linkChecker' ? otherBrokenLinks : 0;
                const isOpen = !foldedKeys.includes(key);

                return (items.length > 0 || others > 0) && (
                    <div key={key}>
                        <div className={styles.groupHead}>
                            <h3 id={groupId} className={styles.groupTitle}>
                                <Button variant="disclosure" aria-expanded={isOpen} aria-controls={contentId} onClick={() => toggle(key)}>
                                    <span className={styles.chevron} data-open={isOpen || undefined}>
                                        <ChevronDownIcon size={16} aria-hidden="true" />
                                    </span>
                                    <span className={styles.groupIcon}>
                                        <Icon size={16} aria-hidden="true" />
                                    </span>
                                    {t(`tasks.groups.${key}`)}
                                </Button>
                            </h3>
                            {items.length > 0 && <span className={styles.groupCount}>{items.length}</span>}
                        </div>
                        <section id={contentId} className={styles.groupBody} aria-labelledby={groupId} hidden={!isOpen}>
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
                    </div>
                );
            })}
        </div>
    );
}
