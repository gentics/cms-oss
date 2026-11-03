import { useQueryClient } from '@tanstack/react-query';
import { SaveIcon, Undo2Icon } from 'lucide-react';
import { useEffect, useId, useRef } from 'react';
import { useTranslation } from 'react-i18next';

import { GuidelineEditor } from '@/components/GuidelineEditor/GuidelineEditor';
import { MissingGuidelineToken } from '@/components/MessageParts/RefToken';
import { Button } from '@/components/ui/button';
import { guidelineName, guidelinePath } from '@/helper/guidelines/guidelines';
import { cmsKeys, useCmsGuidelines, useCmsGuidelineText, useSaveCmsGuideline } from '@/hooks/useCmsQueries';
import { useSessionNodeId } from '@/hooks/useSessionNodeId';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';
import { guidelineDraftKey, useGuidelineDraftStore } from '@/store/useGuidelineDraftStore';

import styles from './GuidelineColumn.module.css';

// While the plan is drafted or carried out, its run may write the file: the editor waits. A closed plan's
// file may be gone (a discarded create).
const EDITABLE: PlanStatus[] = ['proposed', 'done', 'released', 'published', 'review_requested'];

interface GuidelineColumnProps {
    sessionId: string;
    /** The plan that works on the guideline. */
    plan: Plan;
    /** The guideline's id, `<package>/<name>[.default]` (`planGuideline`). */
    slug: string;
}

/**
 * The right column of a session whose plan works on a guideline, in place of the page preview (ticket:
 * no preview, a plain text editor): the guideline's Markdown as the agent wrote it, for a Chief Content
 * Creator to read and edit (`GuidelineEditor`). Saving writes the file directly through the CMS
 * (`useSaveCmsGuideline`), not through the agent. The file is found among the guidelines of the
 * session's node (`useSessionNodeId`) by its slug; one that is not there any more is named as removed
 * or renamed.
 *
 * The text is read again whenever the plan moves on (its status, version or a finished item), as its
 * run may have written the file; changes not saved yet stay. While a run may write it, the editor is
 * read-only (`EDITABLE`).
 *
 * Changes not saved yet are kept in `useGuidelineDraftStore`, so a change of session or of the plan
 * shown loses none: back at the guideline, they are there again. While the editor shows such changes,
 * the browser asks before the page is reloaded or closed (`beforeunload`).
 */
export function GuidelineColumn({ sessionId, plan, slug }: GuidelineColumnProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const queryClient = useQueryClient();
    const nodeId = useSessionNodeId(sessionId);
    const guidelines = useCmsGuidelines(nodeId);
    const guideline = guidelines.data?.find((candidate) => candidate.slug === slug);
    const text = useCmsGuidelineText(guideline);
    const save = useSaveCmsGuideline();
    // The user's text not saved yet, kept beyond this column (`useGuidelineDraftStore`).
    const draftKey = guideline && guidelineDraftKey(guideline.package, guidelinePath(guideline));
    const draft = useGuidelineDraftStore((state) => (draftKey ? state.drafts[draftKey] : undefined));
    const isEditable = EDITABLE.includes(plan.status) && text.data !== undefined && !save.isPending;
    const isChanged = draft !== undefined && draft !== text.data;
    // What the plan has done so far: when it changes, its run may have written the file.
    const progress = `${plan.status}:${plan.version}:${plan.items.filter(({ status }) => status === 'done').length}`;
    const seenProgress = useRef(progress);

    // Changes not saved yet: the browser asks before the page goes.
    useEffect(() => {
        if (!isChanged) {
            return;
        }

        const warn = (event: BeforeUnloadEvent) => event.preventDefault();

        window.addEventListener('beforeunload', warn);

        return () => window.removeEventListener('beforeunload', warn);
    }, [isChanged]);

    useEffect(() => {
        if (seenProgress.current === progress || nodeId === undefined) {
            return;
        }

        seenProgress.current = progress;
        void queryClient.invalidateQueries({ queryKey: cmsKeys.guidelines(nodeId) });

        if (guideline) {
            void queryClient.invalidateQueries({ queryKey: cmsKeys.packageFile(guideline.package, guidelinePath(guideline)) });
        }
    }, [guideline, nodeId, progress, queryClient]);

    function setDraft(text: string | undefined) {
        if (draftKey) {
            useGuidelineDraftStore.getState().setDraft(draftKey, text);
        }
    }

    function body() {
        if (guidelines.data && !guideline) {
            return <p><MissingGuidelineToken name={guidelineName(slug)} /></p>;
        }

        if (text.isError || guidelines.isError) {
            return <p className={styles.empty}>{t('guidelineColumn.loadFailed')}</p>;
        }

        if (!guideline || text.data === undefined) {
            return <p className={styles.empty}>{t('guidelineColumn.loading')}</p>;
        }

        return (
            <GuidelineEditor
                fileName={guideline.fileName}
                value={draft ?? text.data}
                onChange={setDraft}
                disabled={!isEditable}
            />
        );
    }

    return (
        <section className={styles.column} aria-labelledby={titleId}>
            <header className={styles.head}>
                <h2 id={titleId} className={styles.title}>{guidelineName(slug)}</h2>
                <div className={styles.actions}>
                    <Button variant="ghost" size="sm" disabled={!isChanged || save.isPending} onClick={() => setDraft(undefined)}>
                        <Undo2Icon size={14} />
                        {t('guidelineColumn.revert')}
                    </Button>
                    <Button
                        variant="primary"
                        size="sm"
                        disabled={!isChanged || !isEditable || !guideline}
                        onClick={() => {
                            if (guideline && draft !== undefined) {
                                save.mutate({ guideline, text: draft });
                            }
                        }}
                    >
                        <SaveIcon size={14} />
                        {t('guidelineColumn.save')}
                    </Button>
                </div>
            </header>
            <div className={styles.body}>{body()}</div>
        </section>
    );
}
