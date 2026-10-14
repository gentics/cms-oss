import { CircleAlertIcon, CircleCheckIcon, GlobeIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { ThinkingIndicator } from '@/components/ThinkingIndicator/ThinkingIndicator';
import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import type { Publication } from '@/services/apiService/genaix/types';

import styles from './PlanBar.module.css';

/** Where publishing stands: asked, under way, ended with every page out or in review, or failed. */
export type PublishPhase = 'confirm' | 'publishing' | 'published' | 'done' | 'failed';

interface PublishDialogProps {
    phase: PublishPhase;
    /** How many pages go online (`pending` or `failed`); 0 while the plan's publications are not known. */
    pageCount: number;
    /** The outcome per page in words ("1 published, 1 in review"), once publishing ended. */
    summary: string;
    /** The i18n key of why publishing failed. */
    failureKey?: string;
    /** The pages that failed, each with its `error`. */
    failedPages: Publication[];
    onConfirm: () => void;
    onClose: () => void;
}

/**
 * The final approval before the plan's pages are published, on the review panel: asks first, and
 * sends nothing until confirmed. While publishing it shows that it is under way and cannot be closed;
 * then it says what went out (pages sent to review count as done) or why it failed. Before it is
 * confirmed and once publishing ended, it closes at any time.
 */
export function PublishDialog({ phase, pageCount, summary, failureKey, failedPages, onConfirm, onClose }: PublishDialogProps) {
    const { t } = useTranslation();
    const isPublishing = phase === 'publishing';
    const hasEnded = phase === 'published' || phase === 'done';

    return (
        <Dialog open onOpenChange={(open) => !open && !isPublishing && onClose()}>
            {/* The ✕ only while asking; once publishing ended the footer's Close does it. */}
            <DialogContent showCloseButton={phase === 'confirm'}>
                <DialogHeader>
                    <DialogTitle>
                        {phase === 'failed' && <CircleAlertIcon size={18} aria-hidden className={styles.failedIcon} />}
                        {hasEnded && <CircleCheckIcon size={18} aria-hidden className={styles.doneIcon} />}
                        {phase === 'failed' ? t('planBar.publishDialog.failed') : hasEnded ? t(`planBar.publishDialog.${phase}`) : t('planBar.publishDialog.title')}
                    </DialogTitle>
                </DialogHeader>
                <DialogBody>
                    {phase === 'confirm' && (
                        <DialogDescription>
                            {pageCount ? t('planBar.publishDialog.text', { count: pageCount }) : t('planBar.publishDialog.textPages')}
                        </DialogDescription>
                    )}
                    {isPublishing && <ThinkingIndicator text={t('planBar.publishDialog.publishing')} />}
                    {hasEnded && <DialogDescription>{summary}</DialogDescription>}
                    {phase === 'failed' && (
                        <>
                            {failureKey && <DialogDescription>{t(failureKey)}</DialogDescription>}
                            {failedPages.length > 0 && (
                                <ul className={styles.failedPages}>
                                    {failedPages.map(({ cms_ref: ref, error }) => (
                                        <li key={ref.id} className={styles.failedPage}>
                                            <span className={styles.failedPageName}>{ref.name ?? ref.id}</span>
                                            {error && <span className={styles.failedPageError}>{error.detail ?? error.title}</span>}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </>
                    )}
                </DialogBody>
                {!isPublishing && (
                    <DialogFooter>
                        {phase === 'confirm'
                            ? (
                                <>
                                    <Button variant="secondary" size="sm" onClick={onClose}>{t('planBar.publishDialog.cancel')}</Button>
                                    <Button variant="primary" size="sm" onClick={onConfirm}>
                                        <GlobeIcon size={14} />
                                        {t('planBar.publishDialog.confirm')}
                                    </Button>
                                </>
                            )
                            : <Button variant="secondary" size="sm" onClick={onClose}>{t('planBar.publishDialog.close')}</Button>}
                    </DialogFooter>
                )}
            </DialogContent>
        </Dialog>
    );
}
