import Markdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';

import type { TextPart } from '@/services/apiService/genaix/types';

import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

// GFM tables in the look of a `table` part, scrolling sideways when wider than the bubble.
const components: Components = {
    table: ({ node: _node, ...props }) => (
        <div className={styles.proseTable}>
            <table {...props} className={styles.table} />
        </div>
    ),
};

/**
 * `text`: prose, Markdown unless `format` is `plain`, with the GitHub extensions (`remark-gfm`):
 * tables, strikethrough, task lists and autolinks. `react-markdown` renders no raw HTML, so model
 * output cannot inject markup.
 */
export function TextPartView({ part }: PartViewProps<TextPart>) {
    if (part.format === 'plain') {
        return <p className={`${styles.prose} ${styles.plain}`}>{part.text}</p>;
    }

    return (
        <div className={styles.prose}>
            <Markdown remarkPlugins={[remarkGfm]} components={components}>{part.text}</Markdown>
        </div>
    );
}
