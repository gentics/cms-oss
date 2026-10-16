import {
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

/** The icon of a reference's type (design.md §9; the nearest Lucide icon for the types it does not name); `@` for none. */
export const REF_ICONS: Partial<Record<ContextReference['type'], LucideIcon>> = {
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

/** The wording of a reference: its `label`, else its type and id. */
export function refText(ref: ContextReference): string {
    return ref.label || `${ref.type} ${ref.id}`;
}

/** Whether `value` has the shape of a `ContextReference` (a `type` and an `id`). */
export function isContextReference(value: unknown): value is ContextReference {
    return typeof value === 'object' && value !== null
        && typeof (value as { type?: unknown }).type === 'string'
        && (typeof (value as { id?: unknown }).id === 'string' || typeof (value as { id?: unknown }).id === 'number');
}
