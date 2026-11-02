import { AtSignIcon } from 'lucide-react';
import type { ReactNode } from 'react';

import styles from './ReferenceToken.module.css';

/** The `@` token naming a part of the page in a card, e.g. "Text · content_2 · Link" (design.md §12 "`@`-Token / Referenz"). */
export function ReferenceToken({ children }: { children: ReactNode }) {
    return (
        <span className={styles.token}>
            <AtSignIcon size={13} aria-hidden="true" />
            {children}
        </span>
    );
}
