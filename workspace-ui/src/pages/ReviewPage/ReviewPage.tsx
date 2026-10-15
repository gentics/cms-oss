import { Link, useParams, useSearch } from '@tanstack/react-router';
import { ArrowLeftIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { PreviewColumn } from '@/components/PreviewColumn/PreviewColumn';
import { ReviewPanel } from '@/components/ReviewPanel/ReviewPanel';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { usePlanState } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import { livePreviewOf, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './ReviewPage.module.css';

/**
 * The review page (`/sessions/$id/review`, design.md §11 "Review"): the plan `?plan=` names (an older
 * one, from its chat card), else the session's newest plan, the one `plan.updated` reported last (also
 * a closed one, shown read-only), with its page preview on the left
 * and the review panel on the right, the checklist and the actions that release and publish it
 * (`ReviewPanel`). It follows the session's event stream, which also replays the plans, so a live
 * `preview.updated` replaces the plan's stored preview and the plan is read again as it changes. The
 * preview only shows the page here, with a page select when there are several; working on it happens
 * in the chat.
 */
export function ReviewPage() {
    const { t } = useTranslation();
    const { id = '' } = useParams({ strict: false });

    useSessionEvents(id || undefined);

    const { plan: planId } = useSearch({ strict: false });
    const newestPlanId = useWorkspaceEventStore((store) => selectSession(id)(store).plans?.at(-1)?.plan.id);
    // The router keeps a search value as given, so an empty `?plan=` arrives as `''`: the newest plan then.
    const state = usePlanState(id, planId || newestPlanId);
    const livePreview = useWorkspaceEventStore((store) => selectSession(id)(store).preview);
    const livePreviews = useWorkspaceEventStore((store) => selectSession(id)(store).previews);
    // The live previews, else the plan's stored ones: its latest, and the latest of each page it wrote.
    const storedPreview = state.data?.preview;
    const preview = livePreview ?? (storedPreview ? livePreviewOf(storedPreview) : undefined);
    const previews = livePreview ? livePreviews : state.data?.previews?.map(livePreviewOf);

    let panel;

    if (state.isError) {
        panel = (
            <div className={styles.state} role="alert">
                <p>{t(errorMessageKey(state.error))}</p>
                <Button size="sm" onClick={() => void state.refetch()}>{t('sessions.retry')}</Button>
            </div>
        );
    } else if (state.data) {
        panel = <ReviewPanel sessionId={id} state={state.data} />;
    } else {
        panel = <p className={styles.state} role="status">{t(planId || newestPlanId || !id ? 'review.loading' : 'review.noPlan')}</p>;
    }

    return (
        <div className={styles.page}>
            <header className={styles.head}>
                <Button variant="ghost" size="sm" nativeButton={false} role={undefined} render={<Link to="/sessions/$id" params={{ id }} />}>
                    <ArrowLeftIcon size={14} aria-hidden />
                    {t('review.back')}
                </Button>
                <h1 className={styles.title}>{t('review.pageTitle')}</h1>
            </header>
            <main className={styles.main}>
                <div className={styles.preview}>
                    {preview
                        ? <PreviewColumn preview={preview} previews={previews} />
                        : <p className={styles.noPreview}>{t('review.noPreview')}</p>}
                </div>
                <aside className={styles.panel}>{panel}</aside>
            </main>
        </div>
    );
}
