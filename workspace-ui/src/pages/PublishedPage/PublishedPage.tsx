import { Link, useParams, useSearch } from '@tanstack/react-router';
import { CheckIcon, ExternalLinkIcon, EyeIcon, MessagesSquareIcon, SendIcon } from 'lucide-react';
import { Fragment, useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Composer } from '@/components/Composer/Composer';
import { RecentSessions } from '@/components/RecentSessions/RecentSessions';
import { SessionTodos } from '@/components/SessionTodos/SessionTodos';
import { Topbar } from '@/components/Topbar/Topbar';
import { Button } from '@/components/ui/button';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { readOnlyPreviewUrl } from '@/helper/previewUrl/previewUrl';
import { usePlanState } from '@/hooks/useGenaixQueries';
import { usePublishedUrls } from '@/hooks/usePublishedUrls';
import { useSessionStart } from '@/hooks/useSessionStart';
import type { PlanState, Publication } from '@/services/apiService/genaix/types';
import { isSameCmsObject } from '@/store/useWorkspaceEventStore';

import styles from './PublishedPage.module.css';

// The order the outcome is told in when the pages went out in different ways: online first.
const OUTCOMES: Publication['status'][] = ['published', 'review_requested'];

/**
 * What went out (design.md §12 "Erfolg nach dem Veröffentlichen"): the success mark, as a headline
 * "Published" for one page or "4 pages published" for several, "Sent to review" when they wait for
 * approval, or how many went which way ("2 published, 1 in review"); below it the pages' names. Only a published
 * page links to the website (`usePublishedUrls`), in a new tab: one page with its address and "Open the page", of
 * several each one's name. One page in review offers "Open the preview" instead: its CMS preview, read-only
 * (`readOnlyPreviewUrl`); of several, the session's preview shows each. Every outcome offers the way back to the
 * session.
 */
function Outcome({ sessionId, state }: { sessionId: string; state: PlanState }) {
    const { t } = useTranslation();
    const headlineId = useId();
    const pages = state.publications ?? [];
    const counts = OUTCOMES.map((status) => [status, pages.filter((page) => page.status === status).length] as const).filter(([, count]) => count > 0);
    const isMixed = counts.length > 1;
    const isReview = !isMixed && counts[0]?.[0] === 'review_requested';
    const single = pages.length === 1 ? pages[0] : undefined;
    // Where each published page is online.
    const publishedUrls = usePublishedUrls(pages, pages.map(({ cms_ref: ref }) => ref));
    const url = single ? publishedUrls[0] : undefined;
    // One page in review: its latest preview (`PlanState.previews`), not online yet.
    const reviewPreview = single?.status === 'review_requested'
        ? state.previews?.find(({ cms_ref: ref }) => isSameCmsObject(ref, single.cms_ref))
        : undefined;
    const names = pages.map(({ cms_ref: ref }) => ref.name || t('preview.pages.unnamed', { id: ref.id }));
    let headline: string;

    if (isMixed) {
        headline = counts.map(([status, count]) => t(`planBar.count.${status}`, { count })).join(', ');
    } else if (pages.length > 1) {
        headline = t(isReview ? 'published.manyInReview' : 'published.many', { count: pages.length });
    } else {
        headline = t(isReview ? 'published.inReview' : 'published.one');
    }

    const Mark = isReview || isMixed ? SendIcon : CheckIcon;

    return (
        <section className={styles.outcome} aria-labelledby={headlineId}>
            <span className={styles.mark} aria-hidden>
                <Mark size={22} />
            </span>
            <h1 id={headlineId} className={styles.headline}>{headline}</h1>
            {names.length > 0 && (
                <p className={styles.pages}>
                    {single
                        ? [...names, ...(url ? [`${url.host}${url.pathname === '/' ? '' : url.pathname}`] : [])].join(' · ')
                        : names.map((name, index) => {
                            const pageUrl = publishedUrls[index];

                            return (
                                <Fragment key={pages[index]!.cms_ref.id}>
                                    {index > 0 && ' · '}
                                    {pageUrl
                                        ? (
                                            <a className={styles.pageLink} href={pageUrl.href} target="_blank" rel="noreferrer">
                                                {name}
                                                <ExternalLinkIcon size={12} aria-hidden />
                                            </a>
                                        )
                                        : name}
                                </Fragment>
                            );
                        })}
                </p>
            )}
            <div className={styles.actions}>
                {url && (
                    <Button variant="secondary" nativeButton={false} role={undefined} render={<a href={url.href} target="_blank" rel="noreferrer" />}>
                        <ExternalLinkIcon size={16} aria-hidden />
                        {t('published.openPage')}
                    </Button>
                )}
                {reviewPreview && (
                    <Button
                        variant="secondary"
                        nativeButton={false}
                        role={undefined}
                        render={<a href={readOnlyPreviewUrl(reviewPreview.preview_url)} target="_blank" rel="noopener noreferrer" />}
                    >
                        <EyeIcon size={16} aria-hidden />
                        {t('published.openPreview')}
                    </Button>
                )}
                <Button variant="ghost" nativeButton={false} role={undefined} render={<Link to="/sessions/$id" params={{ id: sessionId }} />}>
                    <MessagesSquareIcon size={16} aria-hidden />
                    {t('published.openSession')}
                </Button>
            </div>
        </section>
    );
}

/**
 * The page after a plan was published (`/sessions/$id/published?plan=`, design.md §12 "Erfolg nach dem
 * Veröffentlichen"): what went out (`Outcome`), then the prompt that starts the next session, the open
 * to-dos (`SessionTodos`, placed as on the dashboard) and the sessions to continue. It reads the plan itself, so it shows the same after a reload; a plan that is
 * not `published` or `review_requested` (yet) has nothing to show and links back to its review. A plan that could
 * not be read says why, with a button to try again.
 */
export function PublishedPage() {
    const { t } = useTranslation();
    const { id = '' } = useParams({ strict: false });
    const { plan: planId } = useSearch({ strict: false });
    const state = usePlanState(id, planId || undefined);
    const { start, isStarting, uploadProgress } = useSessionStart();
    const newSessionId = useId();
    const status = state.data?.plan.status;
    const isSettled = status === 'published' || status === 'review_requested';

    let outcome;

    if (state.data && isSettled) {
        outcome = <Outcome sessionId={id} state={state.data} />;
    } else if (planId && (state.isPending || state.isFetching)) {
        outcome = <p className={styles.state} role="status">{t('review.loading')}</p>;
    } else if (state.isError) {
        // Not read is not "not published": the plan may well be out.
        outcome = (
            <div className={`${styles.state} ${styles.failed}`} role="alert">
                <p>{t(errorMessageKey(state.error))}</p>
                <Button size="sm" onClick={() => void state.refetch()}>{t('sessions.retry')}</Button>
            </div>
        );
    } else {
        outcome = (
            <p className={styles.state}>
                {t('published.notPublished')}
                {' '}
                <Link to="/sessions/$id/review" params={{ id }} search={planId ? { plan: planId } : {}}>{t('published.toReview')}</Link>
            </p>
        );
    }

    return (
        <div className={styles.page}>
            <Topbar />
            <div className={styles.body}>
                <main className={styles.scroll}>
                    <div className={styles.content}>
                        {outcome}

                        <section className={styles.next} aria-labelledby={newSessionId}>
                            <h2 id={newSessionId} className={styles.overline}>{t('published.newSession')}</h2>
                            <Composer variant="start" placeholder={t('published.placeholder')} onSubmit={start} isSubmitting={isStarting} uploadProgress={uploadProgress} />
                        </section>

                        <div className={styles.todos}>
                            <SessionTodos />
                        </div>

                        <RecentSessions title={t('published.continue')} defaultOpen />
                    </div>
                </main>
            </div>
        </div>
    );
}
