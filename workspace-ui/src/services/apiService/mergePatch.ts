type JsonObject = Record<string, unknown>;

function isObject(value: unknown): value is JsonObject {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

/**
 * Applies a JSON Merge Patch (RFC 7386): a partial object that only names what changed.
 *
 * Why: while GenAIx streams a structured message part (anything but `text` and `status_note`), each
 * `part.delta` event with `patch_format: merge_patch` carries such a patch instead of the whole part
 * (contract, `PartDeltaEvent`). `useWorkspaceEventStore` applies it to the part built so far.
 *
 * How:
 * - A key set to `null` in the patch deletes that key.
 * - A nested object merges key by key, at every depth.
 * - Every other value replaces the old one. Arrays are replaced whole, never merged.
 *
 * Example: `{ title: 'Draft', meta: { lang: 'en', author: 'x' } }` patched with
 * `{ title: 'Final', meta: { author: null } }` gives `{ title: 'Final', meta: { lang: 'en' } }`.
 *
 * Returns a new value and leaves `target` unchanged, so the store sees the change.
 */
export function applyMergePatch(target: unknown, patch: unknown): unknown {
    if (!isObject(patch)) {
        return patch;
    }

    const result: JsonObject = isObject(target) ? { ...target } : {};

    for (const [key, value] of Object.entries(patch)) {
        if (value === null) {
            delete result[key];
        } else {
            result[key] = applyMergePatch(result[key], value);
        }
    }

    return result;
}
