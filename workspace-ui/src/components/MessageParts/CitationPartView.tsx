import { useTranslation } from 'react-i18next';

import type { CitationPart } from '@/services/apiService/genaix/types';

import { refText } from './contextReference';
import { highlight } from './highlight';
import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `citation`: where an answer came from, as quiet meta text under it (no card): each source with its
 * marker in the text, its name and the passage.
 */
export function CitationPartView({ part }: PartViewProps<CitationPart>) {
    const { t } = useTranslation();
    const title = t('parts.citation.title');

    return (
        <section className={styles.sources} aria-label={title}>
            <div className={styles.sourcesTitle}>{title}</div>
            <ol className={styles.citations}>
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
