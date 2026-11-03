import { ChevronDownIcon, LayersIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { isFirstDraft } from '@/helper/planActions/planActions';
import type { Plan } from '@/services/apiService/genaix/types';

import styles from './PlanSwitcher.module.css';

interface PlanSwitcherProps {
    /** The session's open plans, oldest first (`useOpenPlans`); the switcher shows only with more than one. */
    plans: Plan[];
    /** The plan the bar shows. */
    currentId: string;
    onPick: (planId: string) => void;
}

/**
 * "2 open plans" in the plan bar, only while more than one plan of the session is open (rc.5:
 * `Session.open_plan_ids`; at most one executes): a menu of them, each with its summary and status,
 * the one the bar shows checked; picking one shows that plan in the bar, with its accept, review and
 * the rest. With one open plan or none there is nothing to pick and nothing is shown.
 */
export function PlanSwitcher({ plans, currentId, onPick }: PlanSwitcherProps) {
    const { t } = useTranslation();

    if (plans.length < 2) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger render={<Button variant="ghost" size="sm" />}>
                <LayersIcon size={14} aria-hidden />
                {t('planBar.switcher.count', { count: plans.length })}
                <ChevronDownIcon size={14} aria-hidden />
            </DropdownMenuTrigger>
            <DropdownMenuContent side="top" align="end">
                <DropdownMenuGroup>
                    <DropdownMenuLabel>{t('planBar.switcher.label')}</DropdownMenuLabel>
                    <DropdownMenuRadioGroup value={currentId}>
                        {plans.map((plan) => {
                            // As the plan's head says it: a revision is drafted with the items of the version it revises (rc.5).
                            const statusKey = plan.status === 'drafting' && !isFirstDraft(plan) ? 'revising' : plan.status;

                            return (
                                <DropdownMenuRadioItem key={plan.id} value={plan.id} closeOnClick onClick={() => onPick(plan.id)}>
                                    <span className={styles.item}>
                                        <span className={styles.summary}>{plan.summary || t('planBar.switcher.untitled')}</span>
                                        <span className={styles.status}>{t(`parts.plan.status.${statusKey}`)}</span>
                                    </span>
                                </DropdownMenuRadioItem>
                            );
                        })}
                    </DropdownMenuRadioGroup>
                </DropdownMenuGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
