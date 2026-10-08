import type { ContextReference } from '@/services/apiService/genaix/types';

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
