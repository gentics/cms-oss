import { CheckIcon, ChevronDownIcon, ChevronRightIcon, CircleDotIcon, CircleIcon, type LucideIcon, MinusIcon, PauseIcon, XIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { currentStep, type StepView } from '@/helper/workflowSteps/workflowSteps';

import styles from './WorkflowSteps.module.css';

type Status = 'pending' | 'running' | 'waiting' | 'done' | 'skipped' | 'failed';

const ICONS: Record<Status, LucideIcon> = {
    pending: CircleIcon,
    running: CircleDotIcon,
    waiting: PauseIcon,
    done: CheckIcon,
    skipped: MinusIcon,
    failed: XIcon,
};

// A status outside the contract's enum shows as pending (client rule: tolerate unknown enum values).
function statusOf(step: StepView): Status {
    return step.status in ICONS ? step.status as Status : 'pending';
}

/**
 * Where the agent is in its workflow, as one quiet line: the current step (`step.updated`), or how many
 * are done. The chevron opens the list of the main steps, each with icon and status word (design.md
 * §3.3: never by colour alone).
 */
export function WorkflowSteps({ steps }: { steps: StepView[] }) {
    const { t } = useTranslation();
    const [isOpen, setIsOpen] = useState(false);
    const listId = useId();
    const current = currentStep(steps);
    const summary = current
        ? t('chat.steps.current', { number: steps.indexOf(current) + 1, total: steps.length, label: current.label })
        : t('chat.steps.progress', { done: steps.filter(({ status }) => status === 'done').length, total: steps.length });

    return (
        <div className={styles.steps} role="group" aria-label={t('chat.steps.title')}>
            <div className={styles.line}>
                <Button
                    variant="ghost"
                    size="icon-xs"
                    aria-expanded={isOpen}
                    aria-controls={listId}
                    aria-label={t(isOpen ? 'chat.steps.collapse' : 'chat.steps.expand')}
                    onClick={() => setIsOpen((open) => !open)}
                >
                    {isOpen ? <ChevronDownIcon size={14} /> : <ChevronRightIcon size={14} />}
                </Button>
                <span className={styles.summary}>{summary}</span>
                {current?.status === 'waiting' && <span className={styles.needsYou}>{t('chat.steps.status.waiting')}</span>}
            </div>
            <ol id={listId} className={styles.list} hidden={!isOpen}>
                {steps.map((step) => {
                    const status = statusOf(step);
                    const Icon = ICONS[status];

                    return (
                        <li key={step.id} className={`${styles.step} ${styles[status]}`}>
                            <Icon size={12} aria-hidden />
                            <span className={styles.label}>{step.label}</span>
                            <span className={styles.status}>{t(`chat.steps.status.${status}`)}</span>
                        </li>
                    );
                })}
            </ol>
        </div>
    );
}
