import {
    AtSignIcon,
    FileIcon,
    FileTextIcon,
    FolderIcon,
    GlobeIcon,
    ImageIcon,
    LanguagesIcon,
    LayersIcon,
    LinkIcon,
    type LucideIcon,
    PuzzleIcon,
    ScrollTextIcon,
} from 'lucide-react';

import type { ContextReference } from '@/services/apiService/genaix/types';

import { refText } from './contextReference';

import styles from './MessageParts.module.css';

// design.md §9; the nearest Lucide icon for the types it does not name.
const ICONS: Partial<Record<ContextReference['type'], LucideIcon>> = {
    node: GlobeIcon,
    folder: FolderIcon,
    page: FileTextIcon,
    document: FileTextIcon,
    file: FileIcon,
    session_file: FileIcon,
    image: ImageIcon,
    template: LayersIcon,
    construct: PuzzleIcon,
    guideline: ScrollTextIcon,
    language: LanguagesIcon,
    url: LinkIcon,
};

/** A `ContextReference` as an `@`-token (design.md §12 "`@`-Token / Referenz"). */
export function RefToken({ value }: { value: ContextReference }) {
    const Icon = ICONS[value.type] ?? AtSignIcon;
    const text = refText(value);

    return (
        <span className={styles.ref} title={text}>
            <Icon size={13} aria-hidden />
            <span className={styles.refText}>{text}</span>
        </span>
    );
}
