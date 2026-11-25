import { Link } from '@tanstack/react-router';
import { CheckIcon, ChevronDownIcon, ChevronUpIcon, ExternalLinkIcon, EyeIcon, SparklesIcon, Undo2Icon, UserIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { taskMeta, taskReferences, taskTitle } from '@/helper/taskDisplay/taskDisplay';
import { openTaskInCms, type TaskRequestAction, useStartSessionFromTask, useTaskAction } from '@/hooks/useCmsTasks';
import type { ContextReference } from '@/services/apiService/genaix/types';
import type { CmsTask, CmsTaskAction } from '@/services/cmsApiService/cmsTaskApi';

import { AssignBackDialog } from './AssignBackDialog';

import styles from './TaskItem.module.css';

interface TaskItemProps {
    task: CmsTask;
    /** Hands a publish queue task's objects to the start composer instead of starting a session. */
    onEditWithGenaix: (references: ContextReference[]) => void;
}

/** One task with its actions; a GenAIx task only links to its session. */
export function TaskItem({ task, onEditWithGenaix }: TaskItemProps) {
    const { t, i18n } = useTranslation();
    const taskAction = useTaskAction();
    const { startFromTask, isStarting } = useStartSessionFromTask();
    const [isAssignOpen, setIsAssignOpen] = useState(false);
    const [isInstructionOpen, setIsInstructionOpen] = useState(false);
    const instructionId = useId();
    // What "Start session" sends GenAIx (`taskStartParts`), so the recipient can read it first.
    const instruction = task.genaix?.instruction;
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

            {instruction && (
                <>
                    <div className={styles.details}>
                        <Button variant="ghost" size="sm" aria-expanded={isInstructionOpen} aria-controls={instructionId} onClick={() => setIsInstructionOpen((open) => !open)}>
                            {t(isInstructionOpen ? 'tasks.actions.hideDetails' : 'tasks.actions.showDetails')}
                            {isInstructionOpen ? <ChevronUpIcon size={14} /> : <ChevronDownIcon size={14} />}
                        </Button>
                    </div>
                    <p id={instructionId} className={styles.instruction} hidden={!isInstructionOpen}>{instruction}</p>
                </>
            )}

            {task.source !== 'genaix' && (
                <div className={styles.actions}>
                    {has('approve') && (
                        <Button variant="primary" size="sm" disabled={taskAction.isPending} onClick={() => run('approve')}>
                            <CheckIcon size={14} />
                            {t(task.type === 'approveOffline' ? 'tasks.actions.approveOffline' : 'tasks.actions.approve')}
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
                    {has('startSession') && (task.source === 'publishQueue'
                        // The approver says what to add or change before publishing, so no generic first message is sent.
                        ? (
                            <Button variant="ghost" size="sm" onClick={() => onEditWithGenaix(taskReferences(task))}>
                                <SparklesIcon size={14} />
                                {t('tasks.actions.editWithGenaix')}
                            </Button>
                        )
                        : (
                            <Button variant="ghost" size="sm" disabled={isStarting} onClick={() => startFromTask(task)}>
                                <SparklesIcon size={14} />
                                {t(task.type === 'fixBrokenLinks' ? 'tasks.actions.fixWithGenaix' : 'tasks.actions.startSession')}
                            </Button>
                        ))}
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
