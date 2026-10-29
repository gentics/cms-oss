import { Link } from '@tanstack/react-router';
import { CircleAlertIcon, CircleCheckIcon, CircleDashedIcon, type LucideIcon, SendIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { isCheckOnPage, isFailingCheck, isGateCheck } from '@/helper/pageChecks/pageChecks';
import type { Check, Publication } from '@/services/apiService/genaix/types';

import styles from './ReviewPages.module.css';

type Status = Publication['status'];

const ICONS: Record<Status, LucideIcon> = { pending: CircleDashedIcon, published: CircleCheckIcon, review_requested: SendIcon, failed: CircleAlertIcon };

// A status outside the contract's enum shows as pending (client rule: tolerate unknown enum values).
function statusOf({ status }: Publication): Status {
    return Object.hasOwn(ICONS, status) ? status : 'pending';
}

interface ReviewPagesProps {
    sessionId: string;
    /** The plan's pages, one publication each, in the order the plan wrote them. */
    publications: Publication[];
    /** The plan's checks, to count each page's open ones. */
    checks: Check[];
    /** The id of the page shown. */
    selectedId: number;
}

/**
 * The pages of a plan that wrote several, on the left of the review page: each with its publish status
 * and the number of its gate checks that found something (warnings and failures naming it as `target`).
 * Picking one puts it in the URL (`?page=`), and the review page shows it: its preview, its checks and
 * whether it can be published.
 */
export function ReviewPages({ sessionId, publications, checks, selectedId }: ReviewPagesProps) {
    const { t } = useTranslation();

    return (
        <nav className={styles.pages} aria-label={t('review.publications')}>
            <h2 className={styles.overline}>{t('review.publications')}</h2>
            <ul className={styles.list}>
                {publications.map((publication) => {
                    const { cms_ref: page } = publication;
                    const status = statusOf(publication);
                    const Icon = ICONS[status];
                    const issues = checks.filter((check) => isGateCheck(check) && isCheckOnPage(check, page) && isFailingCheck(check)).length;

                    return (
                        <li key={page.id}>
                            <Link
                                to="/sessions/$id/review"
                                params={{ id: sessionId }}
                                search={(previous) => ({ ...previous, page: page.id })}
                                className={styles.row}
                                aria-current={page.id === selectedId ? 'page' : undefined}
                            >
                                <span className={styles.name}>{page.name || t('preview.pages.unnamed', { id: page.id })}</span>
                                <span className={styles.meta}>
                                    <span className={styles.status} data-status={status}>
                                        <Icon size={12} aria-hidden="true" />
                                        {t(`review.publication.${status}`)}
                                    </span>
                                    {issues > 0 && <span className={styles.issues}>{t('review.page.issues', { count: issues })}</span>}
                                </span>
                            </Link>
                        </li>
                    );
                })}
            </ul>
        </nav>
    );
}
