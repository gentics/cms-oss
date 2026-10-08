import styles from './MessageParts.module.css';

/**
 * The `UnknownPart` rule (contract, `UnknownPart`): a part of a type this client does not know shows
 * its `text` when it has one, else nothing. Never JSON, never an error.
 */
export function UnknownPartView({ part }: { part: { type: string; text?: unknown } }) {
    if (typeof part.text !== 'string' || part.text === '') {
        return null;
    }

    return <p className={`${styles.prose} ${styles.plain}`}>{part.text}</p>;
}
