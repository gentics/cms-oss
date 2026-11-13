import { AtSignIcon, type LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

import styles from './ReferenceToken.module.css';

export interface ReferenceTokenProps {
    /** The label, e.g. "Text · content_2 · Link"; cut with an ellipsis at the token's width. */
    children: ReactNode;
    /** The kind's icon (design.md §9); `@` by default. */
    icon?: LucideIcon;
    /** Verbatim (e.g. a file whose wording must stay): the same token with a dashed border (design.md §12). */
    verbatim?: boolean;
    /** A passage of text rather than a name (the composer's locked verbatim passage): shown whole and wrapping. */
    passage?: boolean;
    /** `onSolid` in the user's own message, on `--bg-solid` (design.md §12 "Chat-Blasen"). */
    tone?: 'surface' | 'onSolid';
    /** A trailing control inside the token, e.g. the composer's × that removes it. */
    action?: ReactNode;
    /** The full label as a tooltip, for one cut off. */
    title?: string;
}

/**
 * The token naming an object (design.md §12 "`@`-Token / Referenz"): a part of the page in a card, a reference, file or
 * setting in the composer, and the same in a sent message. One look everywhere; verbatim with a dashed border.
 */
export function ReferenceToken({ children, icon: Icon = AtSignIcon, verbatim = false, passage = false, tone = 'surface', action, title }: ReferenceTokenProps) {
    return (
        <span
            className={[styles.token, tone === 'onSolid' ? styles.onSolid : '', verbatim ? styles.verbatim : '', passage ? styles.passage : '', action ? styles.withAction : ''].filter(Boolean).join(' ')}
            data-verbatim-token={verbatim || undefined}
            data-passage={passage || undefined}
            data-tone={tone}
        >
            <Icon size={13} aria-hidden="true" />
            <span className={styles.label} title={title}>{children}</span>
            {action}
        </span>
    );
}
