import { Link } from '@tanstack/react-router';
import { ClipboardCheckIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { usePlanTransition, useTransitionPlan } from '@/hooks/useGenaixQueries';
import type { Plan } from '@/services/apiService/genaix/types';

// The modules whose plans have nothing to review: `content_research` only reads and writes no page
// (contract §4.1, "no CMS write tool").
const NOTHING_TO_REVIEW: ReadonlySet<string> = new Set(['content_research']);

/**
 * "Review & release": opens the plan on the review page. A `done` plan is released on the way, so the
 * quality gates check it while the review page opens, which shows that (`ReviewPanel`) and then the
 * checks, the refusal or publish. A plan whose run still works is not released here, since that would
 * stop the run (contract §4.2); the review page offers its release. A plan of a module that writes no
 * page (`NOTHING_TO_REVIEW`) gets no button.
 */
export function ReviewButton({ sessionId, plan }: { sessionId: string; plan: Plan }) {
    const { t } = useTranslation();
    const transitionPlan = useTransitionPlan(sessionId);
    const { pending } = usePlanTransition(sessionId, plan.id);

    if (NOTHING_TO_REVIEW.has(plan.workflow)) {
        return null;
    }

    function releaseOnTheWay() {
        if (plan.status === 'done' && !pending) {
            transitionPlan.mutate({ planId: plan.id, request: { action: 'release' }, isCheck: true });
        }
    }

    return (
        <Button
            variant="secondary"
            size="sm"
            nativeButton={false}
            role={undefined}
            render={<Link to="/sessions/$id/review" params={{ id: sessionId }} search={{ plan: plan.id }} />}
            onClick={releaseOnTheWay}
        >
            <ClipboardCheckIcon size={14} aria-hidden />
            {t('planBar.review')}
        </Button>
    );
}
