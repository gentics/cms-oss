import { Link, useParams, useSearch } from '@tanstack/react-router';
import { ArrowLeftIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { PreviewColumn } from '@/components/PreviewColumn/PreviewColumn';
import { ReviewPages } from '@/components/ReviewPages/ReviewPages';
import { ReviewPanel } from '@/components/ReviewPanel/ReviewPanel';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { usePlanState } from '@/hooks/useGenaixQueries';
import { useSessionEvents } from '@/hooks/useSessionEvents';
import type { PlanState } from '@/services/apiService/genaix/types';
import { cmsRefKey, type LivePreview, livePreviewOf, previewKey, selectSession, useWorkspaceEventStore } from '@/store/useWorkspaceEventStore';

import styles from './ReviewPage.module.css';

/**
 * The plan's pages to preview (`PlanState.previews`), and the latest of them: only the plan's pages,
 * not those another plan of the session wrote. Each page as the stream last sent it (`live`), which
 * reloads it on every `preview.updated`, else as the plan has it stored. The latest is the page the
 * stream sent last, else the plan's `preview`.
 */
function planPreviews(state: PlanState | undefined, live: LivePreview[] = []): { preview?: LivePreview; previews: LivePreview[] } {
    const liveOf = (page: LivePreview) => live.find((sent) => previewKey(sent) === previewKey(page)) ?? page;
    const previews = (state?.previews ?? []).map((stored) => liveOf(livePreviewOf(stored)));
    const stored = state?.preview ? liveOf(livePreviewOf(state.preview)) : undefined;
    const preview = previews.reduce<LivePreview | undefined>((latest, page) => (!latest || page.seq > latest.seq ? page : latest), stored);

    return { preview, previews };
}

/**
 * The review page (`/sessions/$id/review`, design.md §11 "Review"): the plan `?plan=` names (an older
 * one, from its chat card), else the session's newest plan, the one `plan.updated` reported last (also
 * a closed one, shown read-only), with its page preview on the left
 * and the review panel on the right, the checklist and the actions that release and publish it
 * (`ReviewPanel`). It follows the session's event stream, which also replays the plans, so a live
 * `preview.updated` replaces the plan's stored preview and the plan is read again as it changes. The
 * preview shows only the plan's pages (`planPreviews`), and only shows them; working on them happens in
 * the chat.
 *
 * A plan that wrote several pages lists them on the left (`ReviewPages`). The page `?page=` names is
 * shown, else the one previewed last, else the first: its preview, and in the panel its checks and
 * whether it can be published.
 */
export function ReviewPage() {
    const { t } = useTranslation();
    const { id = '' } = useParams({ strict: false });

    useSessionEvents(id || undefined);

    const { plan: planId, page: pageId } = useSearch({ strict: false });
    const newestPlanId = useWorkspaceEventStore((store) => selectSession(id)(store).plans?.at(-1)?.plan.id);
    // The router keeps a search value as given, so an empty `?plan=` arrives as `''`: the newest plan then.
    const state = usePlanState(id, planId || newestPlanId);
    const livePreviews = useWorkspaceEventStore((store) => selectSession(id)(store).previews);
    const { preview: latest, previews } = planPreviews(state.data, livePreviews);
    const publications = state.data?.publications ?? [];
    const latestKey = latest && previewKey(latest);
    const page = publications.length > 1
        ? publications.find(({ cms_ref: ref }) => ref.id === pageId) ?? publications.find(({ cms_ref: ref }) => cmsRefKey(ref) === latestKey) ?? publications[0]
        : undefined;
    const preview = page ? previews.find((shown) => previewKey(shown) === cmsRefKey(page.cms_ref)) : latest;

    let panel;

    if (state.isError) {
        panel = (
            <div className={styles.state} role="alert">
                <p>{t(errorMessageKey(state.error))}</p>
                <Button size="sm" onClick={() => void state.refetch()}>{t('sessions.retry')}</Button>
            </div>
        );
    } else if (state.data) {
        panel = <ReviewPanel sessionId={id} state={state.data} page={page} />;
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
                {state.data && page && <ReviewPages sessionId={id} publications={publications} checks={state.data.checks} selectedId={page.cms_ref.id} />}
                <div className={styles.preview}>
                    {preview
                        ? <PreviewColumn preview={preview} previews={page ? undefined : previews} />
                        : <p className={styles.noPreview}>{t('review.noPreview')}</p>}
                </div>
                <aside className={styles.panel}>{panel}</aside>
            </main>
        </div>
    );
}
