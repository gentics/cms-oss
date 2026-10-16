import { RouteIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { PartCard } from '@/components/MessageParts/PartCard';
import { PlanHead } from '@/components/PlanBar/PlanHead';
import { ReviewButton } from '@/components/PlanBar/ReviewButton';
import { PlanCard } from '@/components/PlanCard/PlanCard';
import { hasGatedActions } from '@/helper/planActions/planActions';
import type { Plan } from '@/services/apiService/genaix/types';

/**
 * A plan of the session that is not the open one in the plan bar, as a chat card where it first came
 * (design.md §12 "Chat-Karte (Part)"): the plan as it stands, closed when it was superseded or
 * discarded, never removed (09-integration-guide.md, "The Workspace UI"). Its foot (`PlanHead`) says
 * where it is and offers what its status still allows here: discard and reopen. While a gated transition
 * is open to it (release, publish, request review; e.g. an older plan still released beside a newer one,
 * `plan_policy.concurrency`), "Review & release" opens this plan on the review page, where it is made
 * (`ReviewButton`, shown once the plan is done and wrote a page, which releases a done plan on the way).
 */
export function ChatPlanCard({ plan, sessionId }: { plan: Plan; sessionId: string }) {
    const { t } = useTranslation();

    return (
        <PartCard
            icon={RouteIcon}
            title={t('parts.plan.title')}
            footer={(
                <PlanHead sessionId={sessionId} plan={plan} isInCard>
                    {hasGatedActions(plan.status) && <ReviewButton sessionId={sessionId} plan={plan} />}
                </PlanHead>
            )}
        >
            <PlanCard plan={plan} sessionId={sessionId} canAnswer={false} />
        </PartCard>
    );
}
