import { Undo2Icon, UserIcon, UserPlusIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import type { CmsTask } from '@/services/cmsApiService/cmsTaskApi';

import styles from './TaskAssignee.module.css';

interface TaskAssigneeProps {
    task: CmsTask;
    onClaim: () => void;
    onUnclaim: () => void;
    disabled?: boolean;
}

// The first letters of the first and the last word, e.g. "MM" for "Max Mustermann".
function initials(name: string): string {
    const words = name.trim().split(/\s+/).filter(Boolean);

    return words.length > 1 ? `${words[0]![0]}${words.at(-1)![0]}`.toUpperCase() : (words[0]?.[0] ?? '').toUpperCase();
}

/**
 * Who a group task is assigned to, as Jira shows it: an empty avatar with "Assign to me" while nobody has taken it,
 * then the taker's avatar and name, with a menu to give it back. Nothing for a task without claim or unclaim.
 */
export function TaskAssignee({ task, onClaim, onUnclaim, disabled }: TaskAssigneeProps) {
    const { t } = useTranslation();

    if (task.actions.includes('claim')) {
        return (
            <span className={styles.assignee}>
                <Button variant="quiet" size="text" disabled={disabled} onClick={onClaim}>
                    <span className={styles.placeholder}>
                        <UserPlusIcon size={12} aria-hidden />
                    </span>
                    {t('tasks.actions.claim')}
                </Button>
            </span>
        );
    }

    if (!task.actions.includes('unclaim')) {
        return null;
    }

    const name = task.assignee?.name ?? '';
    const letters = initials(name);

    return (
        <span className={styles.assignee}>
            <DropdownMenu>
                <DropdownMenuTrigger
                    render={<Button variant="quiet" size="text" disabled={disabled} />}
                    aria-label={t('tasks.assignee.assignedTo', { name })}
                >
                    <span className={styles.avatar} aria-hidden>
                        {letters || <UserIcon size={12} />}
                    </span>
                    {name}
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={onUnclaim}>
                        <Undo2Icon size={14} aria-hidden />
                        {t('tasks.actions.unclaim')}
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>
        </span>
    );
}
