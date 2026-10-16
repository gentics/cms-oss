import { Link } from '@tanstack/react-router';
import { ClipboardCheckIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { writtenPagesOf } from '@/helper/planActions/planActions';
import { usePlanState, usePlanTransition, useTransitionPlan } from '@/hooks/useGenaixQueries';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';

// The modules that have nothing to review: `content_research` only reads and writes no page (contract
// §4.1, "no CMS write tool"). The state of a plan of only these is not even asked for.
const NOTHING_TO_REVIEW: ReadonlySet<string> = new Set(['content_research']);

// The statuses of a plan whose run has finished (contract, `PlanStatus`): done, then released, then
// sent out. While it is accepted or running there is nothing finished to review yet.
const FINISHED: ReadonlySet<PlanStatus> = new Set(['done', 'released', 'published', 'review_requested']);

/**
 * "Review & release": opens the plan on the review page, once the plan has finished (`FINISHED`) and
 * wrote at least one page (`writtenPagesOf` on its `PlanState`, read again on every plan event); before
 * that, and while its state is not known, there is no button. A `done` plan is released on the way, so
 * the quality gates check it while the review page opens, which shows that (`ReviewPanel`) and then the
 * checks, the refusal or publish. A plan whose modules all write no page (`NOTHING_TO_REVIEW`; rc.5: a
 * plan may use several modules) gets no button.
 */
export function ReviewButton({ sessionId, plan }: { sessionId: string; plan: Plan }) {
    const { t } = useTranslation();
    const transitionPlan = useTransitionPlan(sessionId);
    const { pending } = usePlanTransition(sessionId, plan.id);
    const modules = plan.workflows ?? [];
    const writesNoPage = modules.length > 0 && modules.every(({ id }) => NOTHING_TO_REVIEW.has(id));
    const isReviewable = FINISHED.has(plan.status) && !writesNoPage;
    const state = usePlanState(sessionId, isReviewable ? plan.id : undefined);

    if (!isReviewable || !state.data || writtenPagesOf(state.data.cms_objects).length === 0) {
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
