import { CheckCheckIcon, CheckIcon, PencilIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { PlanAdjustment } from '@/hooks/usePlanAdjustment';

interface PlanAdjustActionsProps {
    adjustment: PlanAdjustment;
    /** Adjust was clicked: the plan bar expands, so that the steps are in view while they are adjusted. */
    onAdjust?: () => void;
}

/**
 * The buttons of a proposed plan in the plan bar's head (`usePlanAdjustment`): Adjust and Accept plan,
 * and Cancel and Done while the user adjusts the steps. Nothing once the plan cannot be acted on.
 */
export function PlanAdjustActions({ adjustment, onAdjust }: PlanAdjustActionsProps) {
    const { t } = useTranslation();

    if (adjustment.draft) {
        return (
            <>
                <Button variant="secondary" size="sm" onClick={adjustment.cancel}>{t('parts.plan.cancel')}</Button>
                <Button variant="primary" size="sm" disabled={!adjustment.canFinish} onClick={adjustment.finish}>
                    <CheckIcon size={14} />
                    {t('parts.plan.done')}
                </Button>
            </>
        );
    }

    if (!adjustment.canAct) {
        return null;
    }

    return (
        <>
            <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                    adjustment.adjust();
                    onAdjust?.();
                }}
            >
                <PencilIcon size={14} />
                {t('parts.plan.adjust')}
            </Button>
            <Button variant="primary" size="sm" onClick={adjustment.accept}>
                <CheckCheckIcon size={14} />
                {t('parts.plan.accept')}
            </Button>
        </>
    );
}
