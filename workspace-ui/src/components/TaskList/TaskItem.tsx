import { Link } from '@tanstack/react-router';
import { CheckIcon, ExternalLinkIcon, EyeIcon, SparklesIcon, Undo2Icon, UserIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { taskMeta, taskTitle } from '@/helper/taskDisplay/taskDisplay';
import { openTaskInCms, type TaskRequestAction, useStartSessionFromTask, useTaskAction } from '@/hooks/useCmsTasks';
import type { CmsTask, CmsTaskAction } from '@/services/cmsApiService/cmsTaskApi';

import { AssignBackDialog } from './AssignBackDialog';

import styles from './TaskItem.module.css';

/** One task with its actions; a GenAIx task only links to its session. */
export function TaskItem({ task }: { task: CmsTask }) {
    const { t, i18n } = useTranslation();
    const taskAction = useTaskAction();
    const { startFromTask, isStarting } = useStartSessionFromTask();
    const [isAssignOpen, setIsAssignOpen] = useState(false);
    const has = (action: CmsTaskAction) => task.actions.includes(action);
    const title = taskTitle(task, t);
    const meta = taskMeta(task, t, i18n.language);
    const sessionId = task.source === 'genaix' ? task.genaix?.sessionId : undefined;
    const liveUrl = typeof task.details?.liveUrl === 'string' ? task.details.liveUrl : undefined;
    const hasPage = task.objects?.some((object) => object.type === 'page' && object.nodeId !== undefined) ?? false;
    const run = (action: TaskRequestAction) => taskAction.mutate({ task, action });

    return (
        <div className={styles.task}>
            {sessionId && has('open')
                ? <Link to="/sessions/$id" params={{ id: sessionId }} className={styles.name}>{title}</Link>
                : <span className={styles.name}>{title}</span>}
            {meta && <span className={styles.meta}>{meta}</span>}

            {task.source !== 'genaix' && (
                <div className={styles.actions}>
                    {has('approve') && (
                        <Button variant="primary" size="sm" disabled={taskAction.isPending} onClick={() => run('approve')}>
                            <CheckIcon size={14} />
                            {t('tasks.actions.approve')}
                        </Button>
                    )}
                    {has('assignBack') && (
                        <Button variant="ghost" size="sm" onClick={() => setIsAssignOpen(true)}>
                            <Undo2Icon size={14} />
                            {t('tasks.actions.assignBack')}
                        </Button>
                    )}
                    {has('open') && hasPage && (
                        <Button variant="ghost" size="sm" onClick={() => void openTaskInCms(task)}>
                            <ExternalLinkIcon size={14} />
                            {t(task.source === 'publishQueue' ? 'tasks.actions.compare' : 'tasks.actions.open')}
                        </Button>
                    )}
                    {has('view') && liveUrl && (
                        <a href={liveUrl} target="_blank" rel="noopener noreferrer" className={styles.link}>
                            <EyeIcon size={14} />
                            {t('tasks.actions.view')}
                        </a>
                    )}
                    {has('startSession') && (
                        <Button variant="ghost" size="sm" disabled={isStarting} onClick={() => startFromTask(task)}>
                            <SparklesIcon size={14} />
                            {t(task.type === 'fixBrokenLinks' ? 'tasks.actions.fixWithGenaix' : 'tasks.actions.startSession')}
                        </Button>
                    )}
                    {has('claim') && (
                        <Button variant="ghost" size="sm" disabled={taskAction.isPending} onClick={() => run('claim')}>
                            <UserIcon size={14} />
                            {t('tasks.actions.claim')}
                        </Button>
                    )}
                    {has('unclaim') && (
                        <Button variant="ghost" size="sm" disabled={taskAction.isPending} onClick={() => run('unclaim')}>
                            <Undo2Icon size={14} />
                            {t('tasks.actions.unclaim')}
                        </Button>
                    )}
                    {has('complete') && (
                        <Button variant="ghost" size="sm" disabled={taskAction.isPending} onClick={() => run('complete')}>
                            <CheckIcon size={14} />
                            {t('tasks.actions.complete')}
                        </Button>
                    )}
                </div>
            )}

            {has('assignBack') && <AssignBackDialog task={task} open={isAssignOpen} onOpenChange={setIsAssignOpen} />}
        </div>
    );
}
