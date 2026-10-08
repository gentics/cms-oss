import Markdown from 'react-markdown';

import type { TextPart } from '@/services/apiService/genaix/types';

import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

/**
 * `text`: prose, Markdown unless `format` is `plain`. `react-markdown` renders no raw HTML, so model
 * output cannot inject markup.
 */
export function TextPartView({ part }: PartViewProps<TextPart>) {
    if (part.format === 'plain') {
        return <p className={`${styles.prose} ${styles.plain}`}>{part.text}</p>;
    }

    return (
        <div className={styles.prose}>
            <Markdown>{part.text}</Markdown>
        </div>
    );
}
