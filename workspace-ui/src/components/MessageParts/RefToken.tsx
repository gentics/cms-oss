import { AtSignIcon } from 'lucide-react';

import type { ContextReference, PlannedObject } from '@/services/apiService/genaix/types';

import { REF_ICONS, refText } from './contextReference';

import styles from './MessageParts.module.css';

/** A `ContextReference` as an `@`-token (design.md §12 "`@`-Token / Referenz"). */
export function RefToken({ value }: { value: ContextReference }) {
    const Icon = REF_ICONS[value.type] ?? AtSignIcon;
    const text = refText(value);

    return (
        <span className={styles.ref} title={text}>
            <Icon size={13} aria-hidden />
            <span className={styles.refText}>{text}</span>
        </span>
    );
}

/**
 * An object a plan item will create (`PlannedObject`, rc.5 addition): its name and where it will appear,
 * as a token that is read-only (design.md §8 "Nur lesen"), since the object does not exist yet.
 */
export function PlannedToken({ value }: { value: PlannedObject }) {
    const Icon = REF_ICONS[value.type] ?? AtSignIcon;
    const text = value.path ? `${value.label} · ${value.path}` : value.label;

    return (
        <span className={styles.planned} title={text}>
            <Icon size={13} aria-hidden />
            <span className={styles.refText}>
                {value.label}
                {value.path && <span className={styles.plannedPath}>{` · ${value.path}`}</span>}
            </span>
        </span>
    );
}
