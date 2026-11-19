import { Trans } from 'react-i18next';

import styles from './DeletedSessionTitle.module.css';

/**
 * The toast title for a deleted session, e.g. "“Landing page” deleted" (`sessions.deleted`). The name is an element
 * of its own, so that a long one, e.g. a whole prompt, is cut off with an ellipsis and the title stays one line
 * with "deleted" at its end. The name is the element's content, never parsed as markup.
 */
export function DeletedSessionTitle({ title }: { title: string }) {
    return (
        <span className={styles.title}>
            <Trans i18nKey="sessions.deleted" components={{ name: <span className={styles.name}>{title}</span> }} />
        </span>
    );
}
