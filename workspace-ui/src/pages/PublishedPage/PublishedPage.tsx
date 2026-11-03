import { Link, useParams, useSearch } from '@tanstack/react-router';
import { CheckIcon, ExternalLinkIcon, MessagesSquareIcon, SendIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Composer } from '@/components/Composer/Composer';
import { RecentSessions } from '@/components/RecentSessions/RecentSessions';
import { SessionTodos } from '@/components/SessionTodos/SessionTodos';
import { Topbar } from '@/components/Topbar/Topbar';
import { Button } from '@/components/ui/button';
import { usePlanState } from '@/hooks/useGenaixQueries';
import { useSessionStart } from '@/hooks/useSessionStart';
import type { CmsObjectRef, PlanState, Publication } from '@/services/apiService/genaix/types';

import styles from './PublishedPage.module.css';

// The order the outcome is told in when the pages went out in different ways: online first.
const OUTCOMES: Publication['status'][] = ['published', 'review_requested'];

/**
 * The page's address to open it at, whenever the plan has one: `CmsObjectRef.url` is the published or
 * preview URL as the CMS would open it, else its nice URL or folder path (contract, `CmsObjectRef.url`,
 * best-effort). A path is taken against this app's address, which the CMS serves (as the review page's
 * preview does); anything but `http(s)` (`javascript:`) is no link.
 */
function openableUrlOf({ url }: CmsObjectRef): URL | undefined {
    if (!url) {
        return undefined;
    }

    try {
        const parsed = new URL(url, document.baseURI);

        return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed : undefined;
    } catch {
        return undefined;
    }
}

/**
 * What went out (design.md §12 "Erfolg nach dem Veröffentlichen"): the success mark, as a headline
 * "Published" for one page or "4 pages published" for several, "Sent to review" when they wait for
 * approval, or how many went which way ("2 published, 1 in review"); below it the pages' names, and
 * for one page its address (`cms_ref.url`), which it offers to open; every outcome offers the way back
 * to the session.
 */
function Outcome({ sessionId, state }: { sessionId: string; state: PlanState }) {
    const { t } = useTranslation();
    const headlineId = useId();
    const pages = state.publications ?? [];
    const counts = OUTCOMES.map((status) => [status, pages.filter((page) => page.status === status).length] as const).filter(([, count]) => count > 0);
    const isMixed = counts.length > 1;
    const isReview = !isMixed && counts[0]?.[0] === 'review_requested';
    const single = pages.length === 1 ? pages[0] : undefined;
    // One page: its address, also while it waits for review (the preview then).
    const url = single ? openableUrlOf(single.cms_ref) : undefined;
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
                    {[...names, ...(url ? [`${url.host}${url.pathname === '/' ? '' : url.pathname}`] : [])].join(' · ')}
                </p>
            )}
            <div className={styles.actions}>
                {url && (
                    <Button variant="secondary" nativeButton={false} role={undefined} render={<a href={url.href} target="_blank" rel="noreferrer" />}>
                        <ExternalLinkIcon size={16} aria-hidden />
                        {t('published.openPage')}
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
 * not `published` or `review_requested` (yet) has nothing to show and links back to its review.
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
