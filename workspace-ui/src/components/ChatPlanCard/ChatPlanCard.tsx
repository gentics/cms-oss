import { RouteIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { PartCard } from '@/components/MessageParts/PartCard';
import { PlanHead } from '@/components/PlanBar/PlanHead';
import { PlanCard } from '@/components/PlanCard/PlanCard';
import type { Plan } from '@/services/apiService/genaix/types';

/**
 * A plan of the session that is not the open one in the plan bar, as a chat card where it first came
 * (design.md §12 "Chat-Karte (Part)"): the plan as it stands, closed when it was superseded or
 * discarded, never removed (09-integration-guide.md, "The Workspace UI"). Its foot (`PlanHead`) says
 * where it is and offers what its status still allows: reopen once published or in review, and every
 * transition of an older plan still released beside a newer one (`plan_policy.concurrency`).
 */
export function ChatPlanCard({ plan, sessionId }: { plan: Plan; sessionId: string }) {
    const { t } = useTranslation();

    return (
        <PartCard icon={RouteIcon} title={t('parts.plan.title')} footer={<PlanHead sessionId={sessionId} plan={plan} isInCard />}>
            <PlanCard plan={plan} sessionId={sessionId} canAnswer={false} />
        </PartCard>
    );
}
