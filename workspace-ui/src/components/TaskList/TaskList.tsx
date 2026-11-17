import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useCmsTasks } from '@/hooks/useCmsTasks';
import type { CmsTask, CmsTaskSource } from '@/services/cmsApiService/cmsTaskApi';

import { TaskItem } from './TaskItem';

import styles from './TaskList.module.css';

const GROUPS: { key: string; sources: CmsTaskSource[] }[] = [
    { key: 'genaix', sources: ['genaix'] },
    { key: 'publishQueue', sources: ['publishQueue'] },
    { key: 'linkChecker', sources: ['linkChecker'] },
    { key: 'assigned', sources: ['manual', 'job'] },
];

/** The open tasks of the user, grouped by source; nothing while loading or empty. */
export function TaskList() {
    const { t } = useTranslation();
    const titleId = useId();
    const tasks = useCmsTasks();

    if (tasks.isPending || (tasks.isSuccess && tasks.data.length === 0)) {
        return null;
    }

    const groups = GROUPS.map(({ key, sources }) => ({
        key,
        items: tasks.data?.filter((task: CmsTask) => sources.includes(task.source)) ?? [],
    }));

    return (
        <section className={styles.tasks} aria-labelledby={titleId}>
            <h2 id={titleId} className={styles.title}>{t('tasks.title')}</h2>

            {tasks.isError && (
                <div className={styles.state} role="alert">
                    <p>{t(errorMessageKey(tasks.error))}</p>
                    <Button size="sm" onClick={() => void tasks.refetch()}>{t('sessions.retry')}</Button>
                </div>
            )}

            {groups.map(({ key, items }) => {
                const groupId = `${titleId}-${key}`;

                return items.length > 0 && (
                    <section key={key} className={styles.group} aria-labelledby={groupId}>
                        <h3 id={groupId} className={styles.groupTitle}>{t(`tasks.groups.${key}`)}</h3>
                        <ul className={styles.list}>
                            {items.map((task) => <li key={task.id}><TaskItem task={task} /></li>)}
                        </ul>
                    </section>
                );
            })}
        </section>
    );
}
