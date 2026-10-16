import { ChevronDownIcon, ChevronUpIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { CitationPart } from '@/services/apiService/genaix/types';

import { refText } from './contextReference';
import { highlight } from './highlight';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `citation`: where an answer came from, as quiet meta text under it (no card). Collapsed to "Sources"
 * with a chevron; opened, each source with its marker in the text, its name and the passage.
 */
export function CitationPartView({ part }: PartViewProps<CitationPart>) {
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
                            <span className={styles.sourceLabel}>{refText(entry.ref)}</span>
                            {entry.snippet && <p className={styles.snippet}>{highlight(entry.snippet)}</p>}
                        </div>
                    </li>
                ))}
            </ol>
        </section>
    );
}
