import { AtSignIcon, CircleAlertIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { guidelineName } from '@/helper/guidelines/guidelines';
import { useMissingGuidelines } from '@/hooks/useMissingGuidelines';
import type { ContextReference, PlannedObject } from '@/services/apiService/genaix/types';

import { REF_ICONS, refText } from './contextReference';

import styles from './MessageParts.module.css';

/**
 * A guideline that no longer exists, because its file was removed or renamed: a token of the warning
 * family with icon and text (design.md §3.3), named by `name`.
 */
export function MissingGuidelineToken({ name }: { name: string }) {
    const { t } = useTranslation();
    const text = t('guidelines.missing', { name });

    return (
        <span className={styles.refMissing} title={text}>
            <CircleAlertIcon size={13} aria-hidden />
            <span className={styles.refText}>{text}</span>
        </span>
    );
}

function PlainRefToken({ value }: { value: ContextReference }) {
    const Icon = REF_ICONS[value.type] ?? AtSignIcon;
    const text = refText(value);

    return (
        <span className={styles.ref} title={text}>
            <Icon size={13} aria-hidden />
            <span className={styles.refText}>{text}</span>
        </span>
    );
}

// A guideline of the session `sessionId`: as any reference while it exists (or while that is not known yet).
function GuidelineRefToken({ value, sessionId }: { value: ContextReference; sessionId: string }) {
    const slug = String(value.id);
    const isMissing = useMissingGuidelines(sessionId, [slug]).length > 0;

    return isMissing ? <MissingGuidelineToken name={value.label || guidelineName(slug)} /> : <PlainRefToken value={value} />;
}

interface RefTokenProps {
    value: ContextReference;
    /**
     * The session the reference is shown in. With it, a guideline its node no longer has shows as
     * removed or renamed (`MissingGuidelineToken`).
     */
    sessionId?: string;
}

/** A `ContextReference` as an `@`-token (design.md §12 "`@`-Token / Referenz"). */
export function RefToken({ value, sessionId }: RefTokenProps) {
    return value.type === 'guideline' && sessionId !== undefined
        ? <GuidelineRefToken value={value} sessionId={sessionId} />
        : <PlainRefToken value={value} />;
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
