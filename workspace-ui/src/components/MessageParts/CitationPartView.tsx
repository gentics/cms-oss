import { ChevronDownIcon, ChevronUpIcon, CircleAlertIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { guidelineName } from '@/helper/guidelines/guidelines';
import { useMissingGuidelines } from '@/hooks/useMissingGuidelines';
import type { CitationPart, ContextReference } from '@/services/apiService/genaix/types';

import { refText } from './contextReference';
import { highlight } from './highlight';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

// A cited guideline, named as removed or renamed once the session's node no longer has it.
function GuidelineSource({ value, sessionId }: { value: ContextReference; sessionId: string }) {
    const { t } = useTranslation();
    const slug = String(value.id);
    const isMissing = useMissingGuidelines(sessionId, [slug]).length > 0;

    return isMissing
        ? (
            <span className={styles.sourceMissing}>
                <CircleAlertIcon size={12} aria-hidden />
                {t('guidelines.missing', { name: value.label || guidelineName(slug) })}
            </span>
        )
        : <span className={styles.sourceLabel}>{refText(value)}</span>;
}

/**
 * `citation`: where an answer came from, as quiet meta text under it (no card). Collapsed to "Sources"
 * with a chevron; opened, each source with its marker in the text, its name and the passage. A
 * guideline the session's node no longer has is named as removed or renamed (`useMissingGuidelines`).
 */
export function CitationPartView({ part, sessionId }: PartViewProps<CitationPart>) {
    const { t } = useTranslation();
    const title = t('parts.citation.title');
    const [isOpen, setIsOpen] = useState(false);
    const listId = useId();

    return (
        <section className={styles.sources} aria-label={title}>
            <Button variant="quiet" size="text" aria-expanded={isOpen} aria-controls={listId} onClick={() => setIsOpen((open) => !open)}>
                {title}
                {isOpen ? <ChevronUpIcon size={12} /> : <ChevronDownIcon size={12} />}
            </Button>
            <ol id={listId} className={styles.citations} hidden={!isOpen}>
                {(part.refs ?? []).map((entry, index) => (
                    <li key={index} className={styles.citation}>
                        <span className={styles.marker}>{entry.marker ?? `[${index + 1}]`}</span>
                        <div className={styles.citationText}>
                            {entry.ref.type === 'guideline'
                                ? <GuidelineSource value={entry.ref} sessionId={sessionId} />
                                : <span className={styles.sourceLabel}>{refText(entry.ref)}</span>}
                            {entry.snippet && <p className={styles.snippet}>{highlight(entry.snippet)}</p>}
                        </div>
                    </li>
                ))}
            </ol>
        </section>
    );
}
