import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';

import styles from './MessageParts.module.css';

interface PartCardProps {
    icon: LucideIcon;
    title: string;
    children: ReactNode;
    /** Caption and actions under the body; no foot without it. */
    footer?: ReactNode;
}

/**
 * The chat card every structured part sits in (design.md §12 "Chat-Karte (Part)"): head with icon
 * and overline, body, optional foot. A `section` named by its title.
 */
export function PartCard({ icon: Icon, title, children, footer }: PartCardProps) {
    return (
        <section className={styles.card} aria-label={title}>
            <div className={styles.head}>
                <Icon size={14} aria-hidden />
                <span>{title}</span>
            </div>
            <div className={styles.body}>{children}</div>
            {footer && <div className={styles.foot}>{footer}</div>}
        </section>
    );
}
