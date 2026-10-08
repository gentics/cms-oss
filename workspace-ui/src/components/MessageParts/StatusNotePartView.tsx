import { BanIcon, CircleAlertIcon, InfoIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import type { StatusNotePart } from '@/services/apiService/genaix/types';

import type { PartViewProps } from './partProps';

import styles from './MessageParts.module.css';

const ICONS = { info: InfoIcon, warning: CircleAlertIcon, skipped: BanIcon };

/** `status_note`: a short persisted line; the icon and its label say the kind (design.md §14: never colour alone). */
export function StatusNotePartView({ part }: PartViewProps<StatusNotePart>) {
    const { t } = useTranslation();
    const kind = part.kind ?? 'info';
    const Icon = ICONS[kind];

    return (
        <p className={`${styles.note} ${kind === 'warning' ? styles.noteWarning : ''}`}>
            <Icon size={14} role="img" aria-label={t(`parts.statusNote.${kind}`)} />
            <span>{part.text}</span>
        </p>
    );
}
